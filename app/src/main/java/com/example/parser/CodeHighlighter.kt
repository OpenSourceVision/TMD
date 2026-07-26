package com.example.parser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.util.regex.Pattern

// --- Code Syntax Highlighter ---

object CodeHighlighter {

    fun highlight(code: String, language: String, isDark: Boolean): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        val lang = language.lowercase()
        return when (lang) {
            "kotlin", "kt", "java" -> highlightKotlin(code, isDark)
            "python", "py" -> highlightPython(code, isDark)
            "json" -> highlightJson(code, isDark)
            "html", "xml" -> highlightHtml(code, isDark)
            "javascript", "js", "typescript", "ts" -> highlightJs(code, isDark)
            else -> highlightGeneric(code, isDark)
        }
    }

    private fun highlightKotlin(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors
        val occupied = BooleanArray(code.length)

        // 1. Strings & Comments (Lexical Order)
        highlightStringsAndComments(
            builder, code, occupied,
            regex = """\"\"\"[\s\S]*?\"\"\"|"[^"\\\r\n]*(?:\\.[^"\\\r\n]*)*"|'[^'\\\r\n]*(?:\\.[^'\\\r\n]*)*'|//.*|/\*[\s\S]*?\*/""",
            stringStyle = SpanStyle(color = themeColors.stringColor),
            commentStyle = SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic),
            isCommentPrefix = { it.startsWith("//") || it.startsWith("/*") }
        )

        // 2. Annotations
        applyRegexWithMask(builder, code, """@[a-zA-Z0-9_]+""", SpanStyle(color = themeColors.annotationColor), occupied)

        // 3. Types & PascalCase names
        applyRegexWithMask(builder, code, """\b[A-Z][a-zA-Z0-9_]*\b""", SpanStyle(color = themeColors.typeColor, fontWeight = FontWeight.Bold), occupied)

        // 4. Numbers
        applyRegexWithMask(builder, code, """\b(0x[0-9a-fA-F]+|\d+(\.\d+)?[fFL]?)\b""", SpanStyle(color = themeColors.numberColor), occupied)

        // 5. Keywords
        val keywords = listOf(
            "package", "import", "class", "interface", "object", "fun", "val", "var",
            "return", "if", "else", "when", "for", "while", "do", "break", "continue",
            "this", "super", "sealed", "private", "protected", "public", "internal",
            "launch", "delay", "suspend", "null", "true", "false", "in", "is", "as", "throw", "try", "catch", "finally"
        )
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        applyRegexWithMask(builder, code, keywordPattern, SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold), occupied)

        return builder.toAnnotatedString()
    }

    private fun highlightPython(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors
        val occupied = BooleanArray(code.length)

        // 1. Strings & Comments
        highlightStringsAndComments(
            builder, code, occupied,
            regex = """\"\"\"[\s\S]*?\"\"\"|'''[\s\S]*?'''|"[^"\\\r\n]*(?:\\.[^"\\\r\n]*)*"|'[^'\\\r\n]*(?:\\.[^'\\\r\n]*)*'|#.*""",
            stringStyle = SpanStyle(color = themeColors.stringColor),
            commentStyle = SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic),
            isCommentPrefix = { it.startsWith("#") }
        )

        // 2. Decorators
        applyRegexWithMask(builder, code, """@[a-zA-Z0-9_]+""", SpanStyle(color = themeColors.annotationColor), occupied)

        // 3. Numbers
        applyRegexWithMask(builder, code, """\b(\d+(\.\d+)?)\b""", SpanStyle(color = themeColors.numberColor), occupied)

        // 4. Keywords
        val keywords = listOf(
            "def", "class", "return", "if", "elif", "else", "for", "while", "in", "is", "not",
            "and", "or", "import", "from", "as", "try", "except", "finally", "with", "lambda",
            "print", "len", "range", "None", "True", "False", "pass", "break", "continue"
        )
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        applyRegexWithMask(builder, code, keywordPattern, SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold), occupied)

        return builder.toAnnotatedString()
    }

    private fun highlightJson(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors
        val occupied = BooleanArray(code.length)

        // 1. JSON Keys (before colon)
        applyRegexWithMask(builder, code, """"[^"\\]*"\s*(?=\:)""", SpanStyle(color = themeColors.typeColor, fontWeight = FontWeight.Bold), occupied)

        // 2. JSON String values
        applyRegexWithMask(builder, code, """"(?:[^"\\]|\\.)*"""", SpanStyle(color = themeColors.stringColor), occupied)

        // 3. JSON numbers & booleans
        applyRegexWithMask(builder, code, """\b(true|false|null)\b""", SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold), occupied)
        applyRegexWithMask(builder, code, """\b(-?\d+(\.\d+)?([eE][+-]?\d+)?)\b""", SpanStyle(color = themeColors.numberColor), occupied)

        return builder.toAnnotatedString()
    }

    private fun highlightHtml(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors
        val occupied = BooleanArray(code.length)

        // 1. Comments & Strings
        highlightStringsAndComments(
            builder, code, occupied,
            regex = """<!--[\s\S]*?-->|"[^"]*"|'[^']*'""",
            stringStyle = SpanStyle(color = themeColors.stringColor),
            commentStyle = SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic),
            isCommentPrefix = { it.startsWith("<!--") }
        )

        // 2. Tag brackets and tag names
        applyRegexWithMask(builder, code, """<[a-zA-Z0-9!\-/]+""", SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold), occupied)
        applyRegexWithMask(builder, code, """>""", SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold), occupied)

        // 3. Attribute names
        applyRegexWithMask(builder, code, """\b[a-zA-Z\-]+(?=\=)""", SpanStyle(color = themeColors.annotationColor), occupied)

        return builder.toAnnotatedString()
    }

    private fun highlightJs(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors
        val occupied = BooleanArray(code.length)

        // 1. Strings & Comments
        highlightStringsAndComments(
            builder, code, occupied,
            regex = """`[\s\S]*?`|"[^"\\\r\n]*(?:\\.[^"\\\r\n]*)*"|'[^'\\\r\n]*(?:\\.[^'\\\r\n]*)*'|//.*|/\*[\s\S]*?\*/""",
            stringStyle = SpanStyle(color = themeColors.stringColor),
            commentStyle = SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic),
            isCommentPrefix = { it.startsWith("//") || it.startsWith("/*") }
        )

        // 2. Numbers
        applyRegexWithMask(builder, code, """\b(\d+(\.\d+)?)\b""", SpanStyle(color = themeColors.numberColor), occupied)

        // 3. Keywords
        val keywords = listOf(
            "const", "let", "var", "function", "return", "if", "else", "for", "while", "do",
            "switch", "case", "default", "break", "continue", "import", "export", "from", "class",
            "extends", "new", "this", "super", "try", "catch", "finally", "throw", "async", "await",
            "null", "undefined", "true", "false", "typeof", "instanceof"
        )
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        applyRegexWithMask(builder, code, keywordPattern, SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold), occupied)

        return builder.toAnnotatedString()
    }

    private fun highlightGeneric(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors
        val occupied = BooleanArray(code.length)

        // 1. Strings & Comments
        highlightStringsAndComments(
            builder, code, occupied,
            regex = """"([^"\\]|\\.)*"|//.*|#.*""",
            stringStyle = SpanStyle(color = themeColors.stringColor),
            commentStyle = SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic),
            isCommentPrefix = { it.startsWith("//") || it.startsWith("#") }
        )

        // 2. Keywords
        val keywords = listOf("class", "function", "fn", "def", "func", "return", "if", "else", "for", "while", "import", "package")
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        applyRegexWithMask(builder, code, keywordPattern, SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold), occupied)

        return builder.toAnnotatedString()
    }

    private fun highlightStringsAndComments(
        builder: AnnotatedString.Builder,
        text: String,
        occupied: BooleanArray,
        regex: String,
        stringStyle: SpanStyle,
        commentStyle: SpanStyle,
        isCommentPrefix: (String) -> Boolean
    ) {
        try {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                var isOverlap = false
                for (i in start until end) {
                    if (occupied[i]) {
                        isOverlap = true
                        break
                    }
                }
                if (!isOverlap) {
                    val matchedText = matcher.group()
                    val style = if (isCommentPrefix(matchedText)) commentStyle else stringStyle
                    builder.addStyle(style, start, end)
                    for (i in start until end) {
                        occupied[i] = true
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun applyRegexWithMask(
        builder: AnnotatedString.Builder,
        text: String,
        regex: String,
        style: SpanStyle,
        occupied: BooleanArray
    ) {
        try {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                var isOverlap = false
                for (i in start until end) {
                    if (occupied[i]) {
                        isOverlap = true
                        break
                    }
                }
                if (!isOverlap) {
                    builder.addStyle(style, start, end)
                    for (i in start until end) {
                        occupied[i] = true
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    // --- Highlighting Color Palettes ---

    private interface HighlightPalette {
        val keywordColor: Color
        val typeColor: Color
        val numberColor: Color
        val stringColor: Color
        val commentColor: Color
        val annotationColor: Color
    }

    private object DarkColors : HighlightPalette {
        override val keywordColor = Color(0xFFC678DD)    // Purple
        override val typeColor = Color(0xFF61AFEF)       // Soft Blue
        override val numberColor = Color(0xFFD19A66)     // Peach / Orange
        override val stringColor = Color(0xFF98C379)     // Soft Green
        override val commentColor = Color(0xFF7F848E)    // Silent Gray
        override val annotationColor = Color(0xFFE5C07B) // Warm Yellow
    }

    private object LightColors : HighlightPalette {
        override val keywordColor = Color(0xFFD73A49)    // Elegant Red
        override val typeColor = Color(0xFF6F42C1)       // Royal Purple
        override val numberColor = Color(0xFF005CC5)     // Clear Blue
        override val stringColor = Color(0xFF032F62)     // Dark Slate Blue
        override val commentColor = Color(0xFF6A737D)    // Dark Gray
        override val annotationColor = Color(0xFFE36209) // Rust Orange
    }
}
