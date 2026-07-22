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

        // 1. Types & PascalCase names
        applyRegex(builder, code, """\b[A-Z][a-zA-Z0-9_]*\b""", SpanStyle(color = themeColors.typeColor, fontWeight = FontWeight.Bold))

        // 2. Numbers
        applyRegex(builder, code, """\b(0x[0-9a-fA-F]+|\d+(\.\d+)?[fFL]?)\b""", SpanStyle(color = themeColors.numberColor))

        // 3. Keywords
        val keywords = listOf(
            "package", "import", "class", "interface", "object", "fun", "val", "var",
            "return", "if", "else", "when", "for", "while", "do", "break", "continue",
            "this", "super", "sealed", "private", "protected", "public", "internal",
            "launch", "delay", "suspend", "null", "true", "false", "in", "is", "as", "throw", "try", "catch", "finally"
        )
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        applyRegex(builder, code, keywordPattern, SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold))

        // 4. Annotations
        applyRegex(builder, code, """@[a-zA-Z0-9_]+""", SpanStyle(color = themeColors.annotationColor))

        // 5. Strings
        applyRegex(builder, code, """"[^"\\]*(?:\\.[^"\\]*)*"""", SpanStyle(color = themeColors.stringColor))
        applyRegex(builder, code, """'[^'\\]*(?:\\.[^'\\]*)*'""", SpanStyle(color = themeColors.stringColor))
        applyRegex(builder, code, """\"\"\"[\s\S]*?\"\"\"""", SpanStyle(color = themeColors.stringColor))

        // 6. Comments (applied last to overwrite everything inside)
        applyRegex(builder, code, """//.*""", SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic))
        applyRegex(builder, code, """/\*[\s\S]*?\*/""", SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic))

        return builder.toAnnotatedString()
    }

    private fun highlightPython(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors

        // 1. Numbers
        applyRegex(builder, code, """\b(\d+(\.\d+)?)\b""", SpanStyle(color = themeColors.numberColor))

        // 2. Keywords
        val keywords = listOf(
            "def", "class", "return", "if", "elif", "else", "for", "while", "in", "is", "not",
            "and", "or", "import", "from", "as", "try", "except", "finally", "with", "lambda",
            "print", "len", "range", "None", "True", "False", "pass", "break", "continue"
        )
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        applyRegex(builder, code, keywordPattern, SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold))

        // 3. Decorators
        applyRegex(builder, code, """@[a-zA-Z0-9_]+""", SpanStyle(color = themeColors.annotationColor))

        // 4. Strings
        applyRegex(builder, code, """'[^'\\]*(?:\\.[^'\\]*)*'""", SpanStyle(color = themeColors.stringColor))
        applyRegex(builder, code, """"[^"\\]*(?:\\.[^"\\]*)*"""", SpanStyle(color = themeColors.stringColor))
        applyRegex(builder, code, """'''[\s\S]*?'''""", SpanStyle(color = themeColors.stringColor))
        applyRegex(builder, code, """\"\"\"[\s\S]*?\"\"\"""", SpanStyle(color = themeColors.stringColor))

        // 5. Comments
        applyRegex(builder, code, """#.*""", SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic))

        return builder.toAnnotatedString()
    }

    private fun highlightJson(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors

        // 1. JSON values
        applyRegex(builder, code, """\b(true|false|null)\b""", SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold))
        applyRegex(builder, code, """\b(-?\d+(\.\d+)?([eE][+-]?\d+)?)\b""", SpanStyle(color = themeColors.numberColor))

        // 2. JSON Keys (before colon)
        applyRegex(builder, code, """"[^"\\]*"\s*(?=\:)""", SpanStyle(color = themeColors.typeColor, fontWeight = FontWeight.Bold))

        // 3. JSON String values (not followed by colon)
        applyRegex(builder, code, """"(?:[^"\\]|\\.)*"(?!\s*:)""", SpanStyle(color = themeColors.stringColor))

        return builder.toAnnotatedString()
    }

    private fun highlightHtml(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors

        // 1. Attributes values
        applyRegex(builder, code, """"[^"]*"""", SpanStyle(color = themeColors.stringColor))
        applyRegex(builder, code, """'[^']*'""", SpanStyle(color = themeColors.stringColor))

        // 2. Tag brackets and tag names
        applyRegex(builder, code, """<[a-zA-Z0-9!\-/]+""", SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold))
        applyRegex(builder, code, """>""", SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold))

        // 3. Attribute names (e.g. href=)
        applyRegex(builder, code, """\b[a-zA-Z\-]+(?=\=)""", SpanStyle(color = themeColors.annotationColor))

        // 4. Comments
        applyRegex(builder, code, """<!--[\s\S]*?-->""", SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic))

        return builder.toAnnotatedString()
    }

    private fun highlightJs(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors

        // 1. Numbers
        applyRegex(builder, code, """\b(\d+(\.\d+)?)\b""", SpanStyle(color = themeColors.numberColor))

        // 2. Keywords
        val keywords = listOf(
            "const", "let", "var", "function", "return", "if", "else", "for", "while", "do",
            "switch", "case", "default", "break", "continue", "import", "export", "from", "class",
            "extends", "new", "this", "super", "try", "catch", "finally", "throw", "async", "await",
            "null", "undefined", "true", "false", "typeof", "instanceof"
        )
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        applyRegex(builder, code, keywordPattern, SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold))

        // 3. Strings
        applyRegex(builder, code, """'[^'\\]*(?:\\.[^'\\]*)*'""", SpanStyle(color = themeColors.stringColor))
        applyRegex(builder, code, """"[^"\\]*(?:\\.[^'\\]*)*"""", SpanStyle(color = themeColors.stringColor))
        applyRegex(builder, code, """`[\s\S]*?`""", SpanStyle(color = themeColors.stringColor))

        // 4. Comments
        applyRegex(builder, code, """//.*""", SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic))
        applyRegex(builder, code, """/\*[\s\S]*?\*/""", SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic))

        return builder.toAnnotatedString()
    }

    private fun highlightGeneric(code: String, isDark: Boolean): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val themeColors = if (isDark) DarkColors else LightColors

        // Simple keywords
        val keywords = listOf("class", "function", "fn", "def", "func", "return", "if", "else", "for", "while", "import", "package")
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        applyRegex(builder, code, keywordPattern, SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold))

        // String Literals
        applyRegex(builder, code, """"[^"\\]*(?:\\.[^"\\]*)*"""", SpanStyle(color = themeColors.stringColor))

        // Comments
        applyRegex(builder, code, """//.*""", SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic))
        applyRegex(builder, code, """#.*""", SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic))

        return builder.toAnnotatedString()
    }

    private fun applyRegex(builder: AnnotatedString.Builder, text: String, regex: String, style: SpanStyle) {
        try {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                builder.addStyle(style, matcher.start(), matcher.end())
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
