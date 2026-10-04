package com.devjournal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Top gradient scrim for hero images to guarantee that back button,
 * actions, and title icons are 100% visible on any background brightness.
 */
@Composable
fun TopImageScrim(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    maxAlpha: Float = 0.75f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = maxAlpha),
                        Color.Black.copy(alpha = maxAlpha * 0.45f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Bottom gradient scrim for card cover images to lift up title text and tags.
 */
@Composable
fun BottomImageScrim(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    maxAlpha: Float = 0.85f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = maxAlpha * 0.5f),
                        Color.Black.copy(alpha = maxAlpha)
                    )
                )
            )
    )
}

/**
 * Combined full-height hero scrim with top and bottom feathered gradients.
 */
@Composable
fun FullHeroScrim(
    modifier: Modifier = Modifier,
    topAlpha: Float = 0.70f,
    bottomAlpha: Float = 0.85f
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = topAlpha),
                        Color.Black.copy(alpha = topAlpha * 0.3f),
                        Color.Transparent,
                        Color.Black.copy(alpha = bottomAlpha * 0.5f),
                        Color.Black.copy(alpha = bottomAlpha)
                    )
                )
            )
    )
}

/**
 * Elevated glassmorphic circular icon button specifically styled for
 * floating on hero images and rich media.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp,
    tint: Color = Color.White,
    backgroundColor: Color = Color(0x800B0F19)
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = tint),
                onClick = onClick
            ),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
