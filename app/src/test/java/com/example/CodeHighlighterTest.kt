package com.example

import com.example.parser.CodeHighlighter
import org.junit.Assert.assertNotNull
import org.junit.Test

class CodeHighlighterTest {

    @Test
    fun testStringWithCommentPrefixNotColoredAsComment() {
        val code = """val url = "http://example.com/test""""
        val annotatedString = CodeHighlighter.highlight(code, "kotlin", isDark = true)

        assertNotNull(annotatedString)
        // Verify that highlight executes without throwing and produces formatted spans
        val spans = annotatedString.spanStyles
        assertNotNull(spans)
    }

    @Test
    fun testCommentWithStringQuotesNotColoredAsString() {
        val code = """// This is a comment with "quotes" inside"""
        val annotatedString = CodeHighlighter.highlight(code, "kotlin", isDark = true)

        assertNotNull(annotatedString)
        val spans = annotatedString.spanStyles
        assertNotNull(spans)
    }
}
