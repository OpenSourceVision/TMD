package com.example.parser

// --- AST Models ---

sealed class MarkdownBlock {
    data class Header(val text: String, val level: Int) : MarkdownBlock()
    data class Paragraph(val inlines: List<MarkdownInline>) : MarkdownBlock()
    data class BulletList(val items: List<List<MarkdownInline>>) : MarkdownBlock()
    data class OrderedList(val items: List<List<MarkdownInline>>) : MarkdownBlock()
    data class TaskList(val items: List<TaskItem>) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
    data class BlockQuote(val inlines: List<MarkdownInline>) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

data class TaskItem(
    val isChecked: Boolean,
    val inlines: List<MarkdownInline>
)

sealed class MarkdownInline {
    data class Normal(val text: String) : MarkdownInline()
    data class Bold(val inlines: List<MarkdownInline>) : MarkdownInline()
    data class Italic(val inlines: List<MarkdownInline>) : MarkdownInline()
    data class BoldItalic(val inlines: List<MarkdownInline>) : MarkdownInline()
    data class Strikethrough(val inlines: List<MarkdownInline>) : MarkdownInline()
    data class InlineCode(val text: String) : MarkdownInline()
    data class Link(val text: String, val url: String) : MarkdownInline()
    data class Image(val altText: String, val url: String) : MarkdownInline()
}

enum class MatchType {
    IMAGE, LINK, AUTO_LINK, BOLD_ITALIC, BOLD, ITALIC, STRIKETHROUGH, INLINE_CODE
}

// --- Markdown Parser ---

object MarkdownParser {

    private fun cleanUrl(rawUrl: String): String {
        var u = rawUrl.trim()
        if (u.contains(" ")) {
            u = u.substringBefore(" ").trim()
        }
        return u.removeSurrounding("\"", "\"").removeSurrounding("'", "'")
    }

    fun parse(text: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = text.split("\n")
        var index = 0

        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            // Skip empty lines
            if (trimmed.isEmpty()) {
                index++
                continue
            }

            // 1. Code Block
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                index++
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    codeLines.add(lines[index])
                    index++
                }
                if (index < lines.size) {
                    index++ // skip closing ```
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), lang))
                continue
            }

            // 2. Horizontal Rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___" || trimmed == "- - -") {
                blocks.add(MarkdownBlock.HorizontalRule)
                index++
                continue
            }

            // 3. Header
            if (trimmed.startsWith("#")) {
                var level = 0
                while (level < trimmed.length && trimmed[level] == '#') {
                    level++
                }
                if (level in 1..6 && level < trimmed.length && trimmed[level] == ' ') {
                    var headerText = trimmed.substring(level + 1).trim()
                    // Strip optional trailing {#id} or trailing #
                    headerText = headerText.replace("""\s*\{#.*\}\s*$""".toRegex(), "")
                    headerText = headerText.replace("""\s+#+$""".toRegex(), "")
                    blocks.add(MarkdownBlock.Header(headerText, level))
                    index++
                    continue
                }
            }

            // 4. Blockquote
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (index < lines.size && lines[index].trim().startsWith(">")) {
                    val qLine = lines[index].trim().removePrefix(">").trim()
                    quoteLines.add(qLine)
                    index++
                }
                val fullQuoteText = quoteLines.joinToString(" ")
                blocks.add(MarkdownBlock.BlockQuote(parseInlines(fullQuoteText)))
                continue
            }

            // 5. Table
            if (trimmed.contains("|") && index + 1 < lines.size && isTableSeparator(lines[index + 1].trim())) {
                val headers = parseTableRow(trimmed)
                index += 2 // skip header and separator line
                val rows = mutableListOf<List<String>>()
                while (index < lines.size && lines[index].trim().contains("|") && lines[index].trim().isNotEmpty()) {
                    rows.add(parseTableRow(lines[index].trim()))
                    index++
                }
                blocks.add(MarkdownBlock.Table(headers, rows))
                continue
            }

            // 6. Task List (- [ ] or - [x])
            val taskListRegex = """^[\*\-\+]\s+\[([ xX])\]\s+(.*)$""".toRegex()
            if (taskListRegex.matches(trimmed)) {
                val taskItems = mutableListOf<TaskItem>()
                while (index < lines.size && taskListRegex.matches(lines[index].trim())) {
                    val match = taskListRegex.matchEntire(lines[index].trim())
                    if (match != null) {
                        val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
                        val content = match.groupValues[2].trim()
                        taskItems.add(TaskItem(isChecked, parseInlines(content)))
                    }
                    index++
                }
                blocks.add(MarkdownBlock.TaskList(taskItems))
                continue
            }

            // 7. Bullet List
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("+ ")) {
                val listItems = mutableListOf<List<MarkdownInline>>()
                while (index < lines.size && (lines[index].trim().startsWith("* ") || lines[index].trim().startsWith("- ") || lines[index].trim().startsWith("+ "))
                    && !taskListRegex.matches(lines[index].trim())) {
                    val rawItem = lines[index].trim()
                    val prefix = if (rawItem.startsWith("* ")) "* " else if (rawItem.startsWith("- ")) "- " else "+ "
                    val content = rawItem.removePrefix(prefix).trim()
                    listItems.add(parseInlines(content))
                    index++
                }
                blocks.add(MarkdownBlock.BulletList(listItems))
                continue
            }

            // 8. Ordered List
            val orderedListRegex = """^(\d+)\.\s+(.*)$""".toRegex()
            if (orderedListRegex.matches(trimmed)) {
                val listItems = mutableListOf<List<MarkdownInline>>()
                while (index < lines.size && orderedListRegex.matches(lines[index].trim())) {
                    val match = orderedListRegex.matchEntire(lines[index].trim())
                    if (match != null) {
                        val content = match.groupValues[2].trim()
                        listItems.add(parseInlines(content))
                    }
                    index++
                }
                blocks.add(MarkdownBlock.OrderedList(listItems))
                continue
            }

            // 9. Paragraph
            val paragraphLines = mutableListOf<String>()
            while (index < lines.size && lines[index].trim().isNotEmpty()
                && !lines[index].trim().startsWith("```")
                && !lines[index].trim().startsWith("#")
                && !lines[index].trim().startsWith(">")
                && !lines[index].trim().startsWith("* ")
                && !lines[index].trim().startsWith("- ")
                && !lines[index].trim().startsWith("+ ")
                && !orderedListRegex.matches(lines[index].trim())
                && !taskListRegex.matches(lines[index].trim())
                && !(lines[index].trim().contains("|") && index + 1 < lines.size && isTableSeparator(lines[index + 1].trim()))
                && lines[index].trim() != "---" && lines[index].trim() != "***" && lines[index].trim() != "___") {
                paragraphLines.add(lines[index].trim())
                index++
            }
            if (paragraphLines.isNotEmpty()) {
                val paragraphText = joinParagraphLines(paragraphLines)
                blocks.add(MarkdownBlock.Paragraph(parseInlines(paragraphText)))
            }
        }
        return blocks
    }

    private fun isTableSeparator(line: String): Boolean {
        if (!line.contains("|") || !line.contains("-")) return false
        val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        return cells.isNotEmpty() && cells.all { cell -> cell.all { c -> c == '-' || c == ':' || c == ' ' } }
    }

    private fun parseTableRow(line: String): List<String> {
        var trimmed = line.trim()
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1)
        if (trimmed.endsWith("|")) trimmed = trimmed.substring(0, trimmed.length - 1)
        return trimmed.split("|").map { it.trim() }
    }

    private fun joinParagraphLines(lines: List<String>): String {
        if (lines.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in lines.indices) {
            val curr = lines[i]
            if (i > 0) {
                val prev = lines[i - 1]
                val lastChar = prev.lastOrNull() ?: ' '
                val firstChar = curr.firstOrNull() ?: ' '
                if (isCjk(lastChar) && isCjk(firstChar)) {
                    // Do not insert space between Chinese / CJK characters
                } else {
                    sb.append(" ")
                }
            }
            sb.append(curr)
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

    fun parseInlines(text: String): List<MarkdownInline> {
        if (text.isEmpty()) return emptyList()

        val imageRegex = """!\[(.*?)\]\((.*?)\)""".toRegex()
        val linkRegex = """(?<!\!)\[(.*?)\]\((.*?)\)""".toRegex()
        val autoLinkRegex = """(?<![\(\]])\b(https?://[^\s<>\)]+)\b""".toRegex()
        val boldItalicRegex = """\*\*\*(.*?)\*\*\*|___(.*?)___""".toRegex()
        val boldRegex = """\*\*(.*?)\*\*|__(.*?)__""".toRegex()
        val italicRegex = """\*(.*?)\*|_(.*?)_""".toRegex()
        val strikethroughRegex = """~~(.*?)~~""".toRegex()
        val inlineCodeRegex = """`(.*?)`""".toRegex()

        var earliestMatch: MatchResult? = null
        var earliestType: MatchType? = null

        val imgMatch = imageRegex.find(text)
        val linkMatch = linkRegex.find(text)
        val autoMatch = autoLinkRegex.find(text)
        val biMatch = boldItalicRegex.find(text)
        val bMatch = boldRegex.find(text)
        val itMatch = italicRegex.find(text)
        val stMatch = strikethroughRegex.find(text)
        val codeMatch = inlineCodeRegex.find(text)

        fun updateEarliest(match: MatchResult?, type: MatchType) {
            if (match != null) {
                val currentEarliest = earliestMatch
                if (currentEarliest == null || match.range.first < currentEarliest.range.first) {
                    earliestMatch = match
                    earliestType = type
                }
            }
        }

        updateEarliest(imgMatch, MatchType.IMAGE)
        updateEarliest(linkMatch, MatchType.LINK)
        updateEarliest(autoMatch, MatchType.AUTO_LINK)
        updateEarliest(biMatch, MatchType.BOLD_ITALIC)
        updateEarliest(bMatch, MatchType.BOLD)
        updateEarliest(itMatch, MatchType.ITALIC)
        updateEarliest(stMatch, MatchType.STRIKETHROUGH)
        updateEarliest(codeMatch, MatchType.INLINE_CODE)

        val match = earliestMatch
        val type = earliestType

        if (match != null && type != null) {
            val start = match.range.first
            val end = match.range.last + 1

            val prefix = text.substring(0, start)
            val suffix = text.substring(end)

            val inlineElement = when (type) {
                MatchType.IMAGE -> {
                    val alt = match.groupValues[1]
                    val url = cleanUrl(match.groupValues[2])
                    MarkdownInline.Image(alt, url)
                }
                MatchType.LINK -> {
                    val linkText = match.groupValues[1]
                    val url = cleanUrl(match.groupValues[2])
                    MarkdownInline.Link(linkText, url)
                }
                MatchType.AUTO_LINK -> {
                    val url = cleanUrl(match.groupValues[1])
                    MarkdownInline.Link(url, url)
                }
                MatchType.BOLD_ITALIC -> {
                    val content = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: match.groupValues.getOrNull(2) ?: ""
                    MarkdownInline.BoldItalic(parseInlines(content))
                }
                MatchType.BOLD -> {
                    val content = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: match.groupValues.getOrNull(2) ?: ""
                    MarkdownInline.Bold(parseInlines(content))
                }
                MatchType.ITALIC -> {
                    val content = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: match.groupValues.getOrNull(2) ?: ""
                    MarkdownInline.Italic(parseInlines(content))
                }
                MatchType.STRIKETHROUGH -> {
                    val content = match.groupValues[1]
                    MarkdownInline.Strikethrough(parseInlines(content))
                }
                MatchType.INLINE_CODE -> {
                    val content = match.groupValues[1]
                    MarkdownInline.InlineCode(content)
                }
            }

            return parseInlines(prefix) + listOf(inlineElement) + parseInlines(suffix)
        }

        return listOf(MarkdownInline.Normal(text))
    }
}
