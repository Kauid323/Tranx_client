package com.tranx.community.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * 通用内容渲染组件
 * 根据内容类型选择使用Markdown渲染或普通文本渲染
 */
@Composable
fun ContentRenderer(
    content: String,
    contentType: String? = "text",
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier
) {
    when (contentType) {
        "markdown" -> {
            MarkdownText(
                markdown = content,
                style = style,
                modifier = modifier
            )
        }
        else -> {
            Text(
                text = content,
                style = style,
                modifier = modifier
            )
        }
    }
}
