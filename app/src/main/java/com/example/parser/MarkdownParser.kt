package com.example.parser

enum class TableAlignment {
    LEFT, CENTER, RIGHT
}

// --- AST Models ---

sealed class MarkdownBlock {
    data class Header(val text: String, val level: Int) : MarkdownBlock()
    data class Paragraph(val inlines: List<MarkdownInline>) : MarkdownBlock()
    data class BulletList(
        val items: List<List<MarkdownInline>>,
        val indentLevels: List<Int> = emptyList()
    ) : MarkdownBlock()
    data class OrderedList(
        val items: List<List<MarkdownInline>>,
        val indentLevels: List<Int> = emptyList()
    ) : MarkdownBlock()
    data class TaskList(val items: List<TaskItem>) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
    data class BlockQuote(
        val inlines: List<MarkdownInline>,
        val blocks: List<MarkdownBlock> = emptyList()
    ) : MarkdownBlock()
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<TableAlignment> = emptyList(),
        val headerInlines: List<List<MarkdownInline>> = emptyList(),
        val rowInlines: List<List<List<MarkdownInline>>> = emptyList()
    ) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
    data class HtmlBlock(val content: String) : MarkdownBlock()
}

data class TaskItem(
    val isChecked: Boolean,
    val inlines: List<MarkdownInline>,
    val indentLevel: Int = 0
)

sealed class MarkdownInline {
    data class Normal(val text: String) : MarkdownInline()
    data class Bold(val inlines: List<MarkdownInline>) : MarkdownInline()
    data class Italic(val inlines: List<MarkdownInline>) : MarkdownInline()
    data class BoldItalic(val inlines: List<MarkdownInline>) : MarkdownInline()
    data class Strikethrough(val inlines: List<MarkdownInline>) : MarkdownInline()
    data class InlineCode(val text: String) : MarkdownInline()
    data class Link(
        val text: String,
        val url: String,
        val inlines: List<MarkdownInline> = emptyList()
    ) : MarkdownInline()
    data class Image(
        val altText: String,
        val url: String,
        val inlines: List<MarkdownInline> = emptyList()
    ) : MarkdownInline()
}

enum class MatchType {
    IMAGE_INLINE, IMAGE_REF, LINK_INLINE, LINK_REF, ANGLE_AUTO_LINK, BARE_AUTO_LINK,
    BOLD_ITALIC, BOLD, ITALIC, STRIKETHROUGH, INLINE_CODE
}

data class RefDef(val url: String, val title: String)

// --- Markdown Parser ---

object MarkdownParser {

    private fun cleanUrl(rawUrl: String): String {
        var u = rawUrl.trim()
        if (u.startsWith("<") && u.endsWith(">")) {
            u = u.substring(1, u.length - 1).trim()
        }
        if (u.contains(" ")) {
            u = u.substringBefore(" ").trim()
        }
        return u.removeSurrounding("\"", "\"").removeSurrounding("'", "'")
    }

    // Mask escaped characters during inline scanning
    private const val ESC_AST = "\uE001"
    private const val ESC_UND = "\uE002"
    private const val ESC_HASH = "\uE003"
    private const val ESC_LBRACK = "\uE004"
    private const val ESC_RBRACK = "\uE005"
    private const val ESC_LPAREN = "\uE006"
    private const val ESC_RPAREN = "\uE007"
    private const val ESC_BSLASH = "\uE008"
    private const val ESC_PLUS = "\uE009"
    private const val ESC_MINUS = "\uE00A"
    private const val ESC_TILDE = "\uE00B"
    private const val ESC_BACKTICK = "\uE00C"
    private const val ESC_EXCL = "\uE00D"
    private const val ESC_PIPE = "\uE00E"
    private const val ESC_GT = "\uE00F"

    private fun maskEscapes(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    '*' -> sb.append(ESC_AST)
                    '_' -> sb.append(ESC_UND)
                    '#' -> sb.append(ESC_HASH)
                    '[' -> sb.append(ESC_LBRACK)
                    ']' -> sb.append(ESC_RBRACK)
                    '(' -> sb.append(ESC_LPAREN)
                    ')' -> sb.append(ESC_RPAREN)
                    '\\' -> sb.append(ESC_BSLASH)
                    '+' -> sb.append(ESC_PLUS)
                    '-' -> sb.append(ESC_MINUS)
                    '~' -> sb.append(ESC_TILDE)
                    '`' -> sb.append(ESC_BACKTICK)
                    '!' -> sb.append(ESC_EXCL)
                    '|' -> sb.append(ESC_PIPE)
                    '>' -> sb.append(ESC_GT)
                    else -> sb.append(text[i]).append(text[i + 1])
                }
                i += 2
            } else {
                sb.append(text[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun unmaskEscapes(text: String): String {
        return text
            .replace(ESC_AST, "*")
            .replace(ESC_UND, "_")
            .replace(ESC_HASH, "#")
            .replace(ESC_LBRACK, "[")
            .replace(ESC_RBRACK, "]")
            .replace(ESC_LPAREN, "(")
            .replace(ESC_RPAREN, ")")
            .replace(ESC_BSLASH, "\\")
            .replace(ESC_PLUS, "+")
            .replace(ESC_MINUS, "-")
            .replace(ESC_TILDE, "~")
            .replace(ESC_BACKTICK, "`")
            .replace(ESC_EXCL, "!")
            .replace(ESC_PIPE, "|")
            .replace(ESC_GT, ">")
    }

    fun parse(text: String): List<MarkdownBlock> {
        val rawLines = text.split("\n")
        val refMap = mutableMapOf<String, RefDef>()
        val filteredLines = mutableListOf<String>()

        // Pass 1: Extract reference link definitions like [ref]: url "title"
        val refDefRegex = """^\s*\[([^\]]+)\]:\s*(\S+)(?:\s+["'(](.*?)["')])?\s*$""".toRegex()
        var inFencedCodePass1 = false
        var currentFencePass1 = ""
        for (line in rawLines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("```") || trimmedLine.startsWith("~~~")) {
                val fence = if (trimmedLine.startsWith("```")) "```" else "~~~"
                if (!inFencedCodePass1) {
                    inFencedCodePass1 = true
                    currentFencePass1 = fence
                } else if (trimmedLine.startsWith(currentFencePass1)) {
                    inFencedCodePass1 = false
                    currentFencePass1 = ""
                }
                filteredLines.add(line)
                continue
            }
            if (inFencedCodePass1) {
                filteredLines.add(line)
                continue
            }

            val match = refDefRegex.matchEntire(line)
            if (match != null) {
                val refKey = match.groupValues[1].trim().lowercase()
                val url = cleanUrl(match.groupValues[2])
                val title = match.groupValues[3] ?: ""
                refMap[refKey] = RefDef(url, title)
            } else {
                filteredLines.add(line)
            }
        }

        val blocks = mutableListOf<MarkdownBlock>()
        val lines = filteredLines
        var index = 0

        val taskListRegex = """^(\s*)[\*\-\+]\s+\[([ xX])\]\s+(.*)$""".toRegex()
        val bulletListRegex = """^(\s*)[\*\-\+]\s+(.*)$""".toRegex()
        val orderedListRegex = """^(\s*)(\d+)\.\s+(.*)$""".toRegex()
        val setextH1Regex = """^\s*={2,}\s*$""".toRegex()
        val setextH2Regex = """^\s*-{2,}\s*$""".toRegex()

        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            // Skip empty lines
            if (trimmed.isEmpty()) {
                index++
                continue
            }

            // 1. Code Block (Fenced: ``` or ~~~)
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                val fence = if (trimmed.startsWith("```")) "```" else "~~~"
                val lang = trimmed.removePrefix(fence).trim()
                val codeLines = mutableListOf<String>()
                index++
                while (index < lines.size && !lines[index].trim().startsWith(fence)) {
                    codeLines.add(lines[index])
                    index++
                }
                if (index < lines.size) {
                    index++ // skip closing fence
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), lang))
                continue
            }

            // 2. Indented Code Block (4 spaces or 1 tab)
            if ((line.startsWith("    ") || line.startsWith("\t")) && isCandidateForIndentedCode(lines, index)) {
                val codeLines = mutableListOf<String>()
                while (index < lines.size && (lines[index].startsWith("    ") || lines[index].startsWith("\t") || lines[index].trim().isEmpty())) {
                    val currentLine = lines[index]
                    val stripped = if (currentLine.startsWith("    ")) currentLine.substring(4) else if (currentLine.startsWith("\t")) currentLine.substring(1) else ""
                    codeLines.add(stripped)
                    index++
                }
                // Trim trailing empty lines
                while (codeLines.isNotEmpty() && codeLines.last().isEmpty()) {
                    codeLines.removeAt(codeLines.size - 1)
                }
                if (codeLines.isNotEmpty()) {
                    blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), ""))
                    continue
                }
            }

            // 3. Horizontal Rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___" || trimmed == "- - -" || trimmed == "* * *") {
                blocks.add(MarkdownBlock.HorizontalRule)
                index++
                continue
            }

            // 4. ATX Header (# Heading)
            if (trimmed.startsWith("#")) {
                var level = 0
                while (level < trimmed.length && trimmed[level] == '#') {
                    level++
                }
                if (level in 1..6 && (level == trimmed.length || trimmed[level] == ' ')) {
                    var headerText = if (level < trimmed.length) trimmed.substring(level + 1).trim() else ""
                    headerText = headerText.replace("""\s*\{#.*\}\s*$""".toRegex(), "")
                    headerText = headerText.replace("""\s+#+$""".toRegex(), "")
                    blocks.add(MarkdownBlock.Header(unmaskEscapes(headerText), level))
                    index++
                    continue
                }
            }

            // 5. Blockquote (Supports nested block parsing!)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (index < lines.size && (lines[index].trim().startsWith(">") || (lines[index].trim().isNotEmpty() && quoteLines.isNotEmpty()))) {
                    val rawLine = lines[index]
                    if (rawLine.trim().startsWith(">")) {
                        var stripped = rawLine.trim().substring(1)
                        if (stripped.startsWith(" ")) stripped = stripped.substring(1)
                        quoteLines.add(stripped)
                        index++
                    } else if (rawLine.startsWith("    ") || rawLine.startsWith("\t") || !isBlockStarter(rawLine.trim())) {
                        quoteLines.add(rawLine)
                        index++
                    } else {
                        break
                    }
                }
                val innerContent = quoteLines.joinToString("\n")
                val innerBlocks = parse(innerContent)
                val directInlines = parseInlines(quoteLines.joinToString(" "), refMap)
                blocks.add(MarkdownBlock.BlockQuote(directInlines, innerBlocks))
                continue
            }

            // 6. Table
            if (trimmed.contains("|") && index + 1 < lines.size && isTableSeparator(lines[index + 1].trim())) {
                val headers = parseTableRow(trimmed)
                val alignments = parseTableAlignments(lines[index + 1].trim())
                index += 2 // skip header and separator
                val rows = mutableListOf<List<String>>()
                while (index < lines.size && lines[index].trim().contains("|") && lines[index].trim().isNotEmpty()) {
                    rows.add(parseTableRow(lines[index].trim()))
                    index++
                }
                val headerInlines = headers.map { parseInlines(it, refMap) }
                val rowInlines = rows.map { row -> row.map { cell -> parseInlines(cell, refMap) } }
                blocks.add(MarkdownBlock.Table(headers, rows, alignments, headerInlines, rowInlines))
                continue
            }

            // 7. Setext Header check for next line (=== or ---)
            if (index + 1 < lines.size && !isBlockStarter(trimmed)) {
                val nextTrimmed = lines[index + 1].trim()
                if (setextH1Regex.matches(nextTrimmed)) {
                    blocks.add(MarkdownBlock.Header(unmaskEscapes(trimmed), 1))
                    index += 2
                    continue
                } else if (setextH2Regex.matches(nextTrimmed) && !trimmed.startsWith("- ") && !trimmed.startsWith("* ")) {
                    blocks.add(MarkdownBlock.Header(unmaskEscapes(trimmed), 2))
                    index += 2
                    continue
                }
            }

            // 8. Task List (- [ ] or - [x])
            if (taskListRegex.matches(line)) {
                val taskItems = mutableListOf<TaskItem>()
                val indentStack = mutableListOf(0)
                while (index < lines.size && taskListRegex.matches(lines[index])) {
                    val match = taskListRegex.find(lines[index])
                    if (match != null) {
                        val indentSpaces = match.groupValues[1].length
                        val indentLevel = calculateIndentLevel(indentSpaces, indentStack)
                        val isChecked = match.groupValues[2].equals("x", ignoreCase = true)
                        val content = match.groupValues[3].trim()
                        taskItems.add(TaskItem(isChecked, parseInlines(content, refMap), indentLevel))
                    }
                    index++
                }
                blocks.add(MarkdownBlock.TaskList(taskItems))
                continue
            }

            // 9. Bullet List (Supports nested indentation!)
            if (bulletListRegex.matches(line) && !taskListRegex.matches(line)) {
                val listItems = mutableListOf<List<MarkdownInline>>()
                val indentLevels = mutableListOf<Int>()
                val indentStack = mutableListOf(0)
                while (index < lines.size && bulletListRegex.matches(lines[index]) && !taskListRegex.matches(lines[index])) {
                    val match = bulletListRegex.find(lines[index])
                    if (match != null) {
                        val indentSpaces = match.groupValues[1].length
                        val indentLevel = calculateIndentLevel(indentSpaces, indentStack)
                        val content = match.groupValues[2].trim()

                        // Collect multi-line continuation if any
                        index++
                        val itemLines = mutableListOf(content)
                        while (index < lines.size && lines[index].trim().isNotEmpty()
                            && !bulletListRegex.matches(lines[index])
                            && !orderedListRegex.matches(lines[index])
                            && !isBlockStarter(lines[index].trim())) {
                            itemLines.add(lines[index].trim())
                            index++
                        }
                        listItems.add(parseInlines(joinParagraphLines(itemLines), refMap))
                        indentLevels.add(indentLevel)
                    } else {
                        index++
                    }
                }
                blocks.add(MarkdownBlock.BulletList(listItems, indentLevels))
                continue
            }

            // 10. Ordered List (Supports nested indentation!)
            if (orderedListRegex.matches(line)) {
                val listItems = mutableListOf<List<MarkdownInline>>()
                val indentLevels = mutableListOf<Int>()
                val indentStack = mutableListOf(0)
                while (index < lines.size && orderedListRegex.matches(lines[index])) {
                    val match = orderedListRegex.find(lines[index])
                    if (match != null) {
                        val indentSpaces = match.groupValues[1].length
                        val indentLevel = calculateIndentLevel(indentSpaces, indentStack)
                        val content = match.groupValues[3].trim()

                        index++
                        val itemLines = mutableListOf(content)
                        while (index < lines.size && lines[index].trim().isNotEmpty()
                            && !bulletListRegex.matches(lines[index])
                            && !orderedListRegex.matches(lines[index])
                            && !isBlockStarter(lines[index].trim())) {
                            itemLines.add(lines[index].trim())
                            index++
                        }
                        listItems.add(parseInlines(joinParagraphLines(itemLines), refMap))
                        indentLevels.add(indentLevel)
                    } else {
                        index++
                    }
                }
                blocks.add(MarkdownBlock.OrderedList(listItems, indentLevels))
                continue
            }

            // 11. HTML Block
            val angleAutoLinkRegex = """^<(?:https?://|mailto:)[^>]+>$""".toRegex()
            if (!angleAutoLinkRegex.matches(trimmed) && trimmed.startsWith("<") && (trimmed.endsWith(">") || trimmed.contains(">"))) {
                val htmlTagRegex = """^</?(?:[a-zA-Z][a-zA-Z0-9-]*)(?:\s+[^>]*)?>""".toRegex()
                if (!trimmed.startsWith("<http://") && !trimmed.startsWith("<https://") && !trimmed.startsWith("<mailto:") && htmlTagRegex.containsMatchIn(trimmed)) {
                    val htmlLines = mutableListOf<String>()
                    while (index < lines.size && lines[index].trim().isNotEmpty()) {
                        htmlLines.add(lines[index])
                        index++
                    }
                    blocks.add(MarkdownBlock.HtmlBlock(htmlLines.joinToString("\n")))
                    continue
                }
            }

            // 12. Paragraph
            val paragraphLines = mutableListOf<String>()
            while (index < lines.size && lines[index].trim().isNotEmpty()
                && !lines[index].trim().startsWith("```")
                && !lines[index].trim().startsWith("~~~")
                && !lines[index].trim().startsWith("#")
                && !lines[index].trim().startsWith(">")
                && !bulletListRegex.matches(lines[index])
                && !orderedListRegex.matches(lines[index])
                && !taskListRegex.matches(lines[index])
                && !(lines[index].trim().contains("|") && index + 1 < lines.size && isTableSeparator(lines[index + 1].trim()))
                && lines[index].trim() != "---" && lines[index].trim() != "***" && lines[index].trim() != "___") {

                // Check Setext header trigger on next line
                if (index + 1 < lines.size) {
                    val nextLine = lines[index + 1].trim()
                    if (setextH1Regex.matches(nextLine) || setextH2Regex.matches(nextLine)) {
                        break
                    }
                }
                paragraphLines.add(lines[index])
                index++
            }

            if (paragraphLines.isNotEmpty()) {
                val paragraphText = joinParagraphLines(paragraphLines)
                blocks.add(MarkdownBlock.Paragraph(parseInlines(paragraphText, refMap)))
            }
        }

        return blocks
    }

    private fun calculateIndentLevel(spaces: Int, stack: MutableList<Int>): Int {
        if (spaces <= 0) {
            stack.clear()
            stack.add(0)
            return 0
        }
        if (spaces > stack.last()) {
            stack.add(spaces)
            return stack.size - 1
        }
        while (stack.size > 1 && spaces < stack.last()) {
            stack.removeAt(stack.size - 1)
        }
        if (spaces >= stack.last()) {
            return stack.size - 1
        }
        return 0
    }

    private fun isBlockStarter(trimmed: String): Boolean {
        return trimmed.startsWith("#") || trimmed.startsWith("```") || trimmed.startsWith("~~~")
                || trimmed.startsWith(">") || trimmed.startsWith("* ") || trimmed.startsWith("- ")
                || trimmed.startsWith("+ ") || trimmed.matches("""^\d+\.\s+.*""".toRegex())
                || trimmed == "---" || trimmed == "***" || trimmed == "___"
    }

    private fun isCandidateForIndentedCode(lines: List<String>, index: Int): Boolean {
        if (index == 0) return true
        val prevTrimmed = lines[index - 1].trim()
        return prevTrimmed.isEmpty()
    }

    private fun isTableSeparator(line: String): Boolean {
        val cells = parseTableRow(line)
        return cells.isNotEmpty() && cells.all { cell ->
            val cleaned = cell.trim()
            cleaned.isNotEmpty() && cleaned.all { c -> c == '-' || c == ':' || c == ' ' }
        }
    }

    private fun parseTableAlignments(line: String): List<TableAlignment> {
        val cells = parseTableRow(line)
        return cells.map { cell ->
            val cleaned = cell.trim()
            val starts = cleaned.startsWith(":")
            val ends = cleaned.endsWith(":")
            when {
                starts && ends -> TableAlignment.CENTER
                ends -> TableAlignment.RIGHT
                starts -> TableAlignment.LEFT
                else -> TableAlignment.LEFT
            }
        }
    }

    private fun parseTableRow(line: String): List<String> {
        var trimmed = line.trim()
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1)
        if (trimmed.endsWith("|") && !trimmed.endsWith("\\|")) trimmed = trimmed.substring(0, trimmed.length - 1)

        val cells = mutableListOf<String>()
        val currentCell = StringBuilder()
        var isEscaped = false

        for (i in trimmed.indices) {
            val char = trimmed[i]
            if (isEscaped) {
                currentCell.append(char)
                isEscaped = false
            } else if (char == '\\' && i + 1 < trimmed.length && trimmed[i + 1] == '|') {
                isEscaped = true
            } else if (char == '|') {
                cells.add(currentCell.toString().trim())
                currentCell.clear()
            } else {
                currentCell.append(char)
            }
        }
        cells.add(currentCell.toString().trim())
        return cells
    }

    private fun joinParagraphLines(lines: List<String>): String {
        if (lines.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in lines.indices) {
            val curr = lines[i]
            val currTrimmed = curr.trim()
            if (i > 0) {
                val prev = lines[i - 1]
                // Hard line break check (trailing 2+ spaces or trailing backslash)
                if (prev.endsWith("  ") || prev.endsWith("\\")) {
                    sb.append("\n")
                } else {
                    val lastChar = prev.trim().lastOrNull() ?: ' '
                    val firstChar = currTrimmed.firstOrNull() ?: ' '
                    if (isCjk(lastChar) && isCjk(firstChar)) {
                        // CJK characters: no space
                    } else {
                        sb.append(" ")
                    }
                }
            }
            sb.append(currTrimmed)
        }
        return sb.toString()
    }

    private fun isCjk(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
    }

    fun parseInlines(text: String, refMap: Map<String, RefDef> = emptyMap()): List<MarkdownInline> {
        if (text.isEmpty()) return emptyList()

        val maskedText = maskEscapes(text)

        // Regexes for inline elements
        val imageInlineRegex = """!\[(.*?)\]\((.*?)\)""".toRegex()
        val imageRefRegex = """!\[(.*?)\]\[(.*?)\]|!\[(.*?)\]\[\]""".toRegex()
        val linkInlineRegex = """(?<!\!)\[(.*?)\]\((.*?)\)""".toRegex()
        val linkRefRegex = """(?<!\!)\[(.*?)\]\[(.*?)\]|(?<!\!)\[(.*?)\]\[\]""".toRegex()
        val angleAutoLinkRegex = """<((?:https?://|mailto:)[^>]+)>""".toRegex()
        val bareAutoLinkRegex = """(?<![\(\]])\b(https?://[^\s<>\)]+)\b""".toRegex()
        val boldItalicRegex = """\*\*\*(.*?)\*\*\*|(?<!\w)___(.*?)___(?!\w)""".toRegex()
        val boldRegex = """\*\*(.*?)\*\*|(?<!\w)__(.*?)__(?!\w)""".toRegex()
        val italicRegex = """\*(.*?)\*|(?<!\w)_(.*?)_(?!\w)""".toRegex()
        val strikethroughRegex = """~~(.*?)~~""".toRegex()
        val inlineCodeRegex = """`(.*?)`""".toRegex()

        var earliestMatch: MatchResult? = null
        var earliestType: MatchType? = null

        fun updateEarliest(match: MatchResult?, type: MatchType) {
            if (match != null) {
                val current = earliestMatch
                if (current == null || match.range.first < current.range.first) {
                    earliestMatch = match
                    earliestType = type
                }
            }
        }

        updateEarliest(imageInlineRegex.find(maskedText), MatchType.IMAGE_INLINE)
        updateEarliest(imageRefRegex.find(maskedText), MatchType.IMAGE_REF)
        updateEarliest(linkInlineRegex.find(maskedText), MatchType.LINK_INLINE)
        updateEarliest(linkRefRegex.find(maskedText), MatchType.LINK_REF)
        updateEarliest(angleAutoLinkRegex.find(maskedText), MatchType.ANGLE_AUTO_LINK)
        updateEarliest(bareAutoLinkRegex.find(maskedText), MatchType.BARE_AUTO_LINK)
        updateEarliest(boldItalicRegex.find(maskedText), MatchType.BOLD_ITALIC)
        updateEarliest(boldRegex.find(maskedText), MatchType.BOLD)
        updateEarliest(italicRegex.find(maskedText), MatchType.ITALIC)
        updateEarliest(strikethroughRegex.find(maskedText), MatchType.STRIKETHROUGH)
        updateEarliest(inlineCodeRegex.find(maskedText), MatchType.INLINE_CODE)

        val match = earliestMatch
        val type = earliestType

        if (match != null && type != null) {
            val start = match.range.first
            val end = match.range.last + 1

            val prefix = maskedText.substring(0, start)
            val suffix = maskedText.substring(end)

            val inlineElement: MarkdownInline = when (type) {
                MatchType.IMAGE_INLINE -> {
                    val alt = unmaskEscapes(match.groupValues[1])
                    val url = cleanUrl(match.groupValues[2])
                    MarkdownInline.Image(alt, url, parseInlines(alt, refMap))
                }
                MatchType.IMAGE_REF -> {
                    val alt = unmaskEscapes(match.groupValues[1].ifEmpty { match.groupValues[3] })
                    val refKey = (match.groupValues[2].ifEmpty { alt }).lowercase()
                    val url = refMap[refKey]?.url ?: ""
                    MarkdownInline.Image(alt, url, parseInlines(alt, refMap))
                }
                MatchType.LINK_INLINE -> {
                    val linkText = unmaskEscapes(match.groupValues[1])
                    val url = cleanUrl(match.groupValues[2])
                    MarkdownInline.Link(linkText, url, parseInlines(linkText, refMap))
                }
                MatchType.LINK_REF -> {
                    val linkText = unmaskEscapes(match.groupValues[1].ifEmpty { match.groupValues[3] })
                    val refKey = (match.groupValues[2].ifEmpty { linkText }).lowercase()
                    val url = refMap[refKey]?.url ?: ""
                    MarkdownInline.Link(linkText, url, parseInlines(linkText, refMap))
                }
                MatchType.ANGLE_AUTO_LINK -> {
                    val rawUrl = unmaskEscapes(match.groupValues[1])
                    val url = cleanUrl(rawUrl)
                    MarkdownInline.Link(url, url)
                }
                MatchType.BARE_AUTO_LINK -> {
                    val rawUrl = unmaskEscapes(match.groupValues[1])
                    val url = cleanUrl(rawUrl)
                    MarkdownInline.Link(url, url)
                }
                MatchType.BOLD_ITALIC -> {
                    val content = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: match.groupValues.getOrNull(2) ?: ""
                    MarkdownInline.BoldItalic(parseInlines(content, refMap))
                }
                MatchType.BOLD -> {
                    val content = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: match.groupValues.getOrNull(2) ?: ""
                    MarkdownInline.Bold(parseInlines(content, refMap))
                }
                MatchType.ITALIC -> {
                    val content = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: match.groupValues.getOrNull(2) ?: ""
                    MarkdownInline.Italic(parseInlines(content, refMap))
                }
                MatchType.STRIKETHROUGH -> {
                    val content = match.groupValues[1]
                    MarkdownInline.Strikethrough(parseInlines(content, refMap))
                }
                MatchType.INLINE_CODE -> {
                    val content = unmaskEscapes(match.groupValues[1])
                    MarkdownInline.InlineCode(content)
                }
            }

            return parseInlines(prefix, refMap) + listOf(inlineElement) + parseInlines(suffix, refMap)
        }

        return listOf(MarkdownInline.Normal(unmaskEscapes(maskedText)))
    }
}
