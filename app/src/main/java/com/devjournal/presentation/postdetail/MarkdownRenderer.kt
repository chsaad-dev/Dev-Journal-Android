package com.devjournal.presentation.postdetail

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class Text(val content: String) : MarkdownBlock()
    data class Heading(val level: Int, val content: String) : MarkdownBlock()
    data class Quote(val content: String) : MarkdownBlock()
    data class Code(val language: String, val content: String) : MarkdownBlock()
}

fun parseMarkdown(content: String): List<MarkdownBlock> {
    val lines = content.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    
    var inCodeBlock = false
    var codeLanguage = ""
    val codeContent = java.lang.StringBuilder()
    val currentText = java.lang.StringBuilder()
    
    fun flushText() {
        if (currentText.isNotBlank()) {
            blocks.add(MarkdownBlock.Text(currentText.toString().trimEnd()))
            currentText.clear()
        }
    }
    
    for (line in lines) {
        if (inCodeBlock) {
            if (line.trim() == "```") {
                inCodeBlock = false
                blocks.add(MarkdownBlock.Code(codeLanguage, codeContent.toString().trimEnd()))
                codeContent.clear()
            } else {
                codeContent.appendLine(line)
            }
        } else {
            if (line.trim().startsWith("```")) {
                flushText()
                inCodeBlock = true
                codeLanguage = line.trim().removePrefix("```").trim()
            } else if (line.startsWith("#")) {
                flushText()
                val level = line.takeWhile { it == '#' }.length
                val text = line.removePrefix("#".repeat(level)).trim()
                blocks.add(MarkdownBlock.Heading(level, text))
            } else if (line.startsWith("> ")) {
                flushText()
                blocks.add(MarkdownBlock.Quote(line.removePrefix("> ").trim()))
            } else if (line.isBlank()) {
                flushText()
            } else {
                currentText.appendLine(line)
            }
        }
    }
    flushText()
    return blocks
}

fun highlightSyntax(code: String): AnnotatedString {
    val keywords = listOf(
        "val", "var", "fun", "class", "interface", "object", 
        "if", "else", "when", "for", "while", "return", 
        "true", "false", "null", "import", "package", 
        "override", "private", "public", "protected", "internal", 
        "suspend", "inline", "reified", "let", "apply", "run", "also", "with"
    )
    val keywordColor = Color(0xFFCC7832)
    val stringColor = Color(0xFF6A8759)
    val numberColor = Color(0xFF6897BB)
    val annotationColor = Color(0xFFBBB529)
    val commentColor = Color(0xFF808080)
    
    return buildAnnotatedString {
        append(code)
        
        // Match numbers
        Regex("\\b\\d+\\b").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
        }
        
        // Match keywords
        Regex("\\b(${keywords.joinToString("|")})\\b").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = keywordColor), match.range.first, match.range.last + 1)
        }
        
        // Match annotations
        Regex("@[a-zA-Z_]\\w*").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = annotationColor), match.range.first, match.range.last + 1)
        }
        
        // Match strings
        Regex("\".*?\"").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
        }
        
        // Match comments (line comments)
        Regex("//.*").findAll(code).forEach { match ->
            addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
        }
    }
}

@Composable
fun CodeBlockView(code: String, language: String, onCopy: () -> Unit) {
    val syntaxHighlighted = remember(code, language) { highlightSyntax(code) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E1E) // IDE Dark theme background
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B2B2B))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFA9A9A9)
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = Color(0xFFA9A9A9),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = syntaxHighlighted,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                ),
                color = Color(0xFFA9B7C6), // Default text color
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun RenderMarkdownBody(content: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val blocks = remember(content) { parseMarkdown(content) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Text -> {
                    Text(
                        text = block.content,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        else -> MaterialTheme.typography.titleLarge
                    }
                    Text(
                        text = block.content,
                        style = style.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is MarkdownBlock.Quote -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = block.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
            }
        }
    }
}
