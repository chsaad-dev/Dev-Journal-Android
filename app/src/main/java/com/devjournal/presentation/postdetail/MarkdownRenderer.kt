package com.devjournal.presentation.postdetail

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import coil.compose.AsyncImage
import com.devjournal.ui.theme.BorderSubtleDark
import com.devjournal.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Heading(val level: Int, val content: String) : MarkdownBlock()
    data class Quote(val content: String) : MarkdownBlock()
    data class Code(val language: String, val content: String) : MarkdownBlock()
    data class BulletList(val items: List<String>) : MarkdownBlock()
    data class NumberedList(val items: List<String>) : MarkdownBlock()
    data class Image(val alt: String, val url: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

/**
 * Parses markdown into structured blocks (Headings, Code, Lists, Quotes, Dividers, Paragraphs).
 */
fun parseMarkdown(content: String): List<MarkdownBlock> {
    val lines = content.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()

    var inCodeBlock = false
    var codeLanguage = ""
    val codeContent = StringBuilder()
    val currentParagraph = StringBuilder()
    val currentBulletList = mutableListOf<String>()
    val currentNumberedList = mutableListOf<String>()

    fun flushParagraph() {
        if (currentParagraph.isNotBlank()) {
            blocks.add(MarkdownBlock.Paragraph(currentParagraph.toString().trimEnd()))
            currentParagraph.clear()
        }
    }

    fun flushBulletList() {
        if (currentBulletList.isNotEmpty()) {
            blocks.add(MarkdownBlock.BulletList(currentBulletList.toList()))
            currentBulletList.clear()
        }
    }

    fun flushNumberedList() {
        if (currentNumberedList.isNotEmpty()) {
            blocks.add(MarkdownBlock.NumberedList(currentNumberedList.toList()))
            currentNumberedList.clear()
        }
    }

    fun flushAll() {
        flushParagraph()
        flushBulletList()
        flushNumberedList()
    }

    for (rawLine in lines) {
        val line = rawLine.trimEnd()

        if (inCodeBlock) {
            if (line.trim() == "```") {
                inCodeBlock = false
                blocks.add(MarkdownBlock.Code(codeLanguage, codeContent.toString().trimEnd()))
                codeContent.clear()
            } else {
                codeContent.appendLine(rawLine)
            }
            continue
        }

        // Code block start
        if (line.trim().startsWith("```")) {
            flushAll()
            inCodeBlock = true
            codeLanguage = line.trim().removePrefix("```").trim()
            continue
        }

        // Divider
        if (line.trim() == "---" || line.trim() == "***" || line.trim() == "___") {
            flushAll()
            blocks.add(MarkdownBlock.Divider)
            continue
        }

        // Image block ![alt](url)
        val imageMatch = Regex("^\\s*!\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\s*$").find(line)
        if (imageMatch != null) {
            flushAll()
            blocks.add(MarkdownBlock.Image(imageMatch.groupValues[1], imageMatch.groupValues[2]))
            continue
        }

        // Heading
        if (line.startsWith("#")) {
            flushAll()
            val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
            val headingText = line.removePrefix("#".repeat(level)).trim()
            blocks.add(MarkdownBlock.Heading(level, headingText))
            continue
        }

        // Blockquote
        if (line.startsWith("> ")) {
            flushAll()
            blocks.add(MarkdownBlock.Quote(line.removePrefix("> ").trim()))
            continue
        }

        // Bullet list item (- or *)
        val bulletMatch = Regex("^\\s*[-*]\\s+(.*)$").find(line)
        if (bulletMatch != null) {
            flushParagraph()
            flushNumberedList()
            currentBulletList.add(bulletMatch.groupValues[1])
            continue
        }

        // Numbered list item (1. 2. etc.)
        val numberedMatch = Regex("^\\s*\\d+\\.\\s+(.*)$").find(line)
        if (numberedMatch != null) {
            flushParagraph()
            flushBulletList()
            currentNumberedList.add(numberedMatch.groupValues[1])
            continue
        }

        // Empty line flushes paragraph / lists
        if (line.isBlank()) {
            flushAll()
            continue
        }

        // Normal paragraph line
        flushBulletList()
        flushNumberedList()
        if (currentParagraph.isNotEmpty()) {
            currentParagraph.append(" ")
        }
        currentParagraph.append(line)
    }

    flushAll()
    return blocks
}

/**
 * Parses inline markdown elements: **bold**, *italic*, `inline code`.
 */
fun parseInlineMarkdown(
    text: String,
    codeBgColor: Color = Color(0xFF1E293B),
    codeTextColor: Color = Color(0xFF818CF8)
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val pattern = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`|\\[[^\\]]+\\]\\([^\\)]+\\))")
        val matches = pattern.findAll(text).toList()

        for (match in matches) {
            // Append preceding plain text
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }

            val token = match.value
            when {
                // **bold**
                token.startsWith("**") && token.endsWith("**") && token.length >= 4 -> {
                    val inner = token.substring(2, token.length - 2)
                    val start = length
                    append(inner)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                }
                // *italic*
                token.startsWith("*") && token.endsWith("*") && token.length >= 2 -> {
                    val inner = token.substring(1, token.length - 1)
                    val start = length
                    append(inner)
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                }
                // `inline code`
                token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                    val inner = token.substring(1, token.length - 1)
                    val start = length
                    append(inner)
                    addStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            background = codeBgColor,
                            color = codeTextColor
                        ),
                        start,
                        length
                    )
                }
                // [link text](url)
                token.startsWith("[") && token.contains("](") && token.endsWith(")") -> {
                    val linkText = token.substringAfter("[").substringBefore("](")
                    val start = length
                    append(linkText)
                    addStyle(
                        SpanStyle(
                            color = Color(0xFF58A6FF),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        ),
                        start,
                        length
                    )
                }
                else -> {
                    append(token)
                }
            }
            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

/**
 * Syntax highlighter for IDE-like code blocks.
 */
fun highlightSyntax(code: String): AnnotatedString {
    val keywords = listOf(
        "val", "var", "fun", "class", "interface", "object", "enum",
        "if", "else", "when", "for", "while", "return", "throw", "try", "catch", "finally",
        "true", "false", "null", "import", "package", "typealias",
        "override", "private", "public", "protected", "internal", "open", "abstract",
        "suspend", "inline", "reified", "data", "sealed", "companion",
        "const", "lateinit", "by", "lazy", "let", "apply", "run", "also", "with",
        "async", "await", "export", "default", "function", "const", "let", "from"
    )
    val keywordColor = Color(0xFFFF7B72) // Coral / Red-Orange
    val stringColor = Color(0xFFA5D6FF)  // Sky blue
    val numberColor = Color(0xFF79C0FF)  // Light blue
    val annotationColor = Color(0xFFFFA657) // Amber
    val commentColor = Color(0xFF8B949E) // Muted gray

    return buildAnnotatedString {
        append(code)

        // Numbers
        Regex("\\b\\d+(\\.\\d+)?\\b").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
        }

        // Keywords
        Regex("\\b(${keywords.joinToString("|")})\\b").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }

        // Annotations
        Regex("@[a-zA-Z_]\\w*").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = annotationColor), match.range.first, match.range.last + 1)
        }

        // Strings
        Regex("\".*?\"|'.*?'").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
        }

        // Line Comments
        Regex("//.*").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
        }
    }
}

/**
 * Modern IDE Code Block with language tag, syntax highlighting, and animated copy button.
 */
@Composable
fun CodeBlockView(
    code: String,
    language: String,
    onCopy: () -> Unit
) {
    val syntaxHighlighted = remember(code, language) { highlightSyntax(code) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderSubtleDark, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0D1117) // GitHub dark IDE tone
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF21262D)
                ) {
                    Text(
                        text = language.ifBlank { "code" }.lowercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFFC9D1D9),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Copy button with visual confirmation
                IconButton(
                    onClick = {
                        onCopy()
                        copied = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    if (copied) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Copied",
                            tint = Color(0xFF3FB950), // Green checkmark
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy code",
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Code Content
            Text(
                text = syntaxHighlighted,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 20.sp
                ),
                color = Color(0xFFC9D1D9),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Complete Markdown Body Renderer with rich typography, bullet/numbered lists,
 * blockquotes, code cards, and inline bold/italic/code parsing.
 */
@Composable
fun RenderMarkdownBody(
    content: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val blocks = remember(content) { parseMarkdown(content) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> {
                    val styledText = parseInlineMarkdown(block.text)
                    Text(
                        text = styledText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            letterSpacing = 0.15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                is MarkdownBlock.Heading -> {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            2 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = block.content,
                            style = style,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (block.level == 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(3.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }

                is MarkdownBlock.Quote -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(28.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = parseInlineMarkdown(block.content),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is MarkdownBlock.BulletList -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        block.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = parseInlineMarkdown(item),
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.NumberedList -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        block.items.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    modifier = Modifier.size(20.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = parseInlineMarkdown(item),
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = BorderSubtleDark,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                is MarkdownBlock.Code -> {
                    CodeBlockView(
                        code = block.content,
                        language = block.language,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(block.content))
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                is MarkdownBlock.Image -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderSubtleDark, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = block.url,
                                contentDescription = block.alt.ifBlank { "Embedded article image" },
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (block.alt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = block.alt,
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
