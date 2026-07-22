package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.parser.CodeHighlighter
import com.example.parser.MarkdownBlock
import com.example.parser.MarkdownInline
import com.example.parser.MarkdownParser
import com.example.parser.TaskItem

@Composable
fun MarkdownContent(
    content: String,
    theme: MarkdownReadingTheme,
    modifier: Modifier = Modifier
) {
    val blocks = MarkdownParser.parse(content)

    Column(modifier = modifier) {
        blocks.forEach { block ->
            RenderBlock(block = block, theme = theme)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun RenderBlock(
    block: MarkdownBlock,
    theme: MarkdownReadingTheme
) {
    when (block) {
        is MarkdownBlock.Header -> {
            RenderHeader(block.text, block.level, theme)
        }
        is MarkdownBlock.Paragraph -> {
            RenderParagraph(block.inlines, theme)
        }
        is MarkdownBlock.BulletList -> {
            RenderBulletList(block.items, theme)
        }
        is MarkdownBlock.OrderedList -> {
            RenderOrderedList(block.items, theme)
        }
        is MarkdownBlock.TaskList -> {
            RenderTaskList(block.items, theme)
        }
        is MarkdownBlock.CodeBlock -> {
            RenderCodeBlock(block.code, block.language, theme)
        }
        is MarkdownBlock.BlockQuote -> {
            RenderBlockQuote(block.inlines, theme)
        }
        is MarkdownBlock.Table -> {
            RenderTable(block.headers, block.rows, theme)
        }
        is MarkdownBlock.HorizontalRule -> {
            RenderHorizontalRule(theme)
        }
    }
}

@Composable
fun RenderHeader(
    text: String,
    level: Int,
    theme: MarkdownReadingTheme
) {
    val (fontSize, fontWeight, bottomMargin) = when (level) {
        1 -> Triple(26.sp, FontWeight.ExtraBold, 8.dp)
        2 -> Triple(22.sp, FontWeight.Bold, 6.dp)
        3 -> Triple(19.sp, FontWeight.SemiBold, 4.dp)
        4 -> Triple(17.sp, FontWeight.Medium, 2.dp)
        else -> Triple(15.sp, FontWeight.SemiBold, 2.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = theme.headerColor,
            lineHeight = fontSize * 1.3
        )
        Spacer(modifier = Modifier.height(bottomMargin))
        if (level <= 2) {
            HorizontalDivider(
                color = theme.dividerColor,
                thickness = if (level == 1) 2.dp else 1.dp
            )
        }
    }
}

@Composable
fun RenderParagraph(
    inlines: List<MarkdownInline>,
    theme: MarkdownReadingTheme
) {
    val uriHandler = LocalUriHandler.current

    // Check if there are any image inlines in this paragraph
    val imageInlines = inlines.filterIsInstance<MarkdownInline.Image>()
    if (imageInlines.isNotEmpty()) {
        imageInlines.forEach { img ->
            RenderImageInline(img, theme)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Filter out image elements for normal paragraph rendering
    val textInlines = inlines.filter { it !is MarkdownInline.Image }
    if (textInlines.isNotEmpty()) {
        val annotatedString = buildAnnotatedStringFromInlines(textInlines, theme)
        val hasLinks = textInlines.any { hasLinkInline(it) }

        if (hasLinks) {
            ClickableText(
                text = annotatedString,
                style = TextStyle(
                    color = theme.textColor,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                var url = annotation.item.trim()
                                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("mailto:")) {
                                    url = "https://$url"
                                }
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                // Ignore failure
                            }
                        }
                }
            )
        } else {
            Text(
                text = annotatedString,
                style = TextStyle(
                    color = theme.textColor,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun hasLinkInline(inline: MarkdownInline): Boolean {
    return when (inline) {
        is MarkdownInline.Link -> true
        is MarkdownInline.Bold -> inline.inlines.any { hasLinkInline(it) }
        is MarkdownInline.Italic -> inline.inlines.any { hasLinkInline(it) }
        is MarkdownInline.BoldItalic -> inline.inlines.any { hasLinkInline(it) }
        is MarkdownInline.Strikethrough -> inline.inlines.any { hasLinkInline(it) }
        else -> false
    }
}

@Composable
fun RenderBulletList(
    items: List<List<MarkdownInline>>,
    theme: MarkdownReadingTheme
) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        items.forEach { inlines ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    RenderParagraph(inlines = inlines, theme = theme)
                }
            }
        }
    }
}

@Composable
fun RenderOrderedList(
    items: List<List<MarkdownInline>>,
    theme: MarkdownReadingTheme
) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        items.forEachIndexed { index, inlines ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Text(
                    text = "${index + 1}.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.accentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    RenderParagraph(inlines = inlines, theme = theme)
                }
            }
        }
    }
}

@Composable
fun RenderTaskList(
    items: List<TaskItem>,
    theme: MarkdownReadingTheme
) {
    Column(modifier = Modifier.padding(start = 4.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (item.isChecked) theme.accentColor else theme.textColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(end = 8.dp, top = 2.dp)
                        .width(20.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    RenderParagraph(inlines = item.inlines, theme = theme)
                }
            }
        }
    }
}

@Composable
fun RenderCodeBlock(
    code: String,
    language: String,
    theme: MarkdownReadingTheme
) {
    val clipboardManager = LocalClipboardManager.current
    val highlightedCode = CodeHighlighter.highlight(code, language, theme.isDark)

    Card(
        colors = CardDefaults.cardColors(containerColor = theme.cardBackgroundColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.dividerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("code_block")
    ) {
        Column {
            // Header bar of code block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.dividerColor.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = theme.accentColor,
                    modifier = Modifier.width(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language.isNotEmpty()) language.uppercase() else "CODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(code)) },
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = theme.textColor.copy(alpha = 0.6f),
                        modifier = Modifier.width(14.dp)
                    )
                }
            }

            // Scrollable code container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = highlightedCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = theme.textColor
                )
            }
        }
    }
}

@Composable
fun RenderBlockQuote(
    inlines: List<MarkdownInline>,
    theme: MarkdownReadingTheme
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
            .background(theme.cardBackgroundColor.copy(alpha = 0.5f))
    ) {
        // Left accent block line
        Canvas(modifier = Modifier.matchParentSize()) {
            drawLine(
                color = theme.accentColor,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 4.dp.toPx()
            )
        }

        Box(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp)) {
            val annotatedString = buildAnnotatedStringFromInlines(inlines, theme)
            Text(
                text = annotatedString,
                style = TextStyle(
                    color = theme.textColor.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 22.sp
                )
            )
        }
    }
}

@Composable
fun RenderTable(
    headers: List<String>,
    rows: List<List<String>>,
    theme: MarkdownReadingTheme
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.cardBackgroundColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.dividerColor),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column {
                // Table Header
                if (headers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(theme.dividerColor.copy(alpha = 0.35f))
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        headers.forEach { headerText ->
                            Text(
                                text = headerText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = theme.headerColor,
                                modifier = Modifier
                                    .widthIn(min = 100.dp, max = 220.dp)
                                    .padding(horizontal = 10.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = theme.dividerColor, thickness = 1.dp)
                }

                // Table Rows
                rows.forEachIndexed { rowIndex, rowCells ->
                    val bg = if (rowIndex % 2 == 0) Color.Transparent else theme.dividerColor.copy(alpha = 0.1f)
                    Row(
                        modifier = Modifier
                            .background(bg)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headers.indices.forEach { colIndex ->
                            val cellText = rowCells.getOrNull(colIndex) ?: ""
                            val parsedInlines = MarkdownParser.parseInlines(cellText)
                            val annotatedString = buildAnnotatedStringFromInlines(parsedInlines, theme)
                            Text(
                                text = annotatedString,
                                fontSize = 14.sp,
                                color = theme.textColor,
                                modifier = Modifier
                                    .widthIn(min = 100.dp, max = 220.dp)
                                    .padding(horizontal = 10.dp)
                            )
                        }
                    }
                    if (rowIndex < rows.size - 1) {
                        HorizontalDivider(color = theme.dividerColor.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderHorizontalRule(theme: MarkdownReadingTheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            drawLine(
                color = theme.dividerColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
    }
}

@Composable
fun RenderImageInline(
    image: MarkdownInline.Image,
    theme: MarkdownReadingTheme
) {
    val context = LocalContext.current
    var cleanUrl = image.url.trim()
    if (cleanUrl.contains(" ")) {
        cleanUrl = cleanUrl.substringBefore(" ").trim()
    }
    cleanUrl = cleanUrl.removeSurrounding("\"", "\"").removeSurrounding("'", "'")

    Card(
        colors = CardDefaults.cardColors(containerColor = theme.cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(cleanUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = image.altText.ifEmpty { "Image" },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 380.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(theme.dividerColor.copy(alpha = 0.1f))
            )
            if (image.altText.isNotEmpty()) {
                Text(
                    text = image.altText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    color = theme.textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// Helper to convert list of inlines to AnnotatedString
fun buildAnnotatedStringFromInlines(
    inlines: List<MarkdownInline>,
    theme: MarkdownReadingTheme
): AnnotatedString {
    return buildAnnotatedString {
        appendInlines(inlines, theme)
    }
}

private fun AnnotatedString.Builder.appendInlines(
    inlines: List<MarkdownInline>,
    theme: MarkdownReadingTheme
) {
    inlines.forEach { inline ->
        when (inline) {
            is MarkdownInline.Normal -> {
                append(inline.text)
            }
            is MarkdownInline.Bold -> {
                val start = length
                appendInlines(inline.inlines, theme)
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = theme.textColor),
                    start,
                    length
                )
            }
            is MarkdownInline.Italic -> {
                val start = length
                appendInlines(inline.inlines, theme)
                addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = theme.textColor),
                    start,
                    length
                )
            }
            is MarkdownInline.BoldItalic -> {
                val start = length
                appendInlines(inline.inlines, theme)
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = theme.textColor
                    ),
                    start,
                    length
                )
            }
            is MarkdownInline.Strikethrough -> {
                val start = length
                appendInlines(inline.inlines, theme)
                addStyle(
                    SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        color = theme.textColor.copy(alpha = 0.7f)
                    ),
                    start,
                    length
                )
            }
            is MarkdownInline.InlineCode -> {
                val start = length
                append(inline.text)
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = theme.inlineCodeText,
                        background = theme.inlineCodeBg
                    ),
                    start,
                    length
                )
            }
            is MarkdownInline.Link -> {
                val start = length
                append(inline.text)
                addStyle(
                    SpanStyle(
                        color = theme.accentColor,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    length
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = inline.url,
                    start = start,
                    end = length
                )
            }
            is MarkdownInline.Image -> {
                append("[图片: ${inline.altText}]")
            }
        }
    }
}
