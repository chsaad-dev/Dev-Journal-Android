package com.devjournal.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A horizontal scrolling row with subtle edge gradient fades on the left and right,
 * providing a clear, intuitive HCI scroll affordance so items don't cut off abruptly.
 */
@Composable
fun EdgeFadeHorizontalRow(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    fadeWidth: Dp = 20.dp,
    fadeColor: Color = MaterialTheme.colorScheme.background,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable RowScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // Left fade overlay (visible when scrolled)
        if (scrollState.value > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(fadeWidth)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(fadeColor, Color.Transparent)
                            )
                        )
                )
            }
        }

        // Right fade overlay (visible when there is more content to scroll)
        if (scrollState.value < scrollState.maxValue) {
            Box(
                modifier = Modifier
                    .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(fadeWidth)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, fadeColor)
                            )
                        )
                )
            }
        }
    }
}
