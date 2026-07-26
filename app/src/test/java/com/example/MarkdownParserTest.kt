package com.example

import com.example.parser.MarkdownBlock
import com.example.parser.MarkdownInline
import com.example.parser.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    private val parser = MarkdownParser

    @Test
    fun testSnakeCaseNameNotItalic() {
        val text = "This is snake_case_name variable."
        val inlines = parser.parseInlines(text)

        // Verify that snake_case_name is NOT split into italic 'case'
        val containsItalic = inlines.any { it is MarkdownInline.Italic }
        assertTrue("snake_case_name should not be parsed as italic", !containsItalic)

        val fullText = inlines.joinToString("") {
            when (it) {
                is MarkdownInline.Normal -> it.text
                else -> ""
            }
        }
        assertTrue(fullText.contains("snake_case_name"))
    }

    @Test
    fun testUnderlineItalicWordBoundary() {
        val text = "This is _italic_ text."
        val inlines = parser.parseInlines(text)

        val italicInline = inlines.filterIsInstance<MarkdownInline.Italic>().firstOrNull()
        assertTrue("Expected _italic_ to be parsed as MarkdownInline.Italic", italicInline != null)

        val italicContent = italicInline?.inlines?.filterIsInstance<MarkdownInline.Normal>()?.firstOrNull()?.text
        assertEquals("italic", italicContent)
    }

    @Test
    fun testUnderlineBoldWordBoundary() {
        val text = "This is __bold__ text."
        val inlines = parser.parseInlines(text)

        val boldInline = inlines.filterIsInstance<MarkdownInline.Bold>().firstOrNull()
        assertTrue("Expected __bold__ to be parsed as MarkdownInline.Bold", boldInline != null)

        val boldContent = boldInline?.inlines?.filterIsInstance<MarkdownInline.Normal>()?.firstOrNull()?.text
        assertEquals("bold", boldContent)
    }

    @Test
    fun testTableParsingWithEscapedPipe() {
        val markdown = """
            | Column 1 | Column 2 |
            | --- | --- |
            | Cell with \| pipe | Cell 2 |
        """.trimIndent()

        val blocks = parser.parse(markdown)
        val tableBlock = blocks.filterIsInstance<MarkdownBlock.Table>().firstOrNull()

        assertTrue("Expected a Table block", tableBlock != null)
        assertEquals(2, tableBlock?.headers?.size)
        assertEquals("Column 1", tableBlock?.headers?.get(0))
        assertEquals("Column 2", tableBlock?.headers?.get(1))

        assertEquals(1, tableBlock?.rows?.size)
        val row = tableBlock?.rows?.get(0)
        assertEquals(2, row?.size)
        assertEquals("Cell with | pipe", row?.get(0))
        assertEquals("Cell 2", row?.get(1))
    }

    @Test
    fun testHeadersAndLists() {
        val markdown = """
            # Heading 1
            ## Heading 2
            - Bullet 1
            - Bullet 2
            1. Number 1
            2. Number 2
        """.trimIndent()

        val blocks = parser.parse(markdown)
        assertTrue(blocks.any { it is MarkdownBlock.Header && it.level == 1 })
        assertTrue(blocks.any { it is MarkdownBlock.Header && it.level == 2 })
        assertTrue(blocks.any { it is MarkdownBlock.BulletList })
        assertTrue(blocks.any { it is MarkdownBlock.OrderedList })
    }
}
