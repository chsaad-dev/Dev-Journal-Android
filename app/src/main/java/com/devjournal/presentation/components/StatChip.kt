package com.devjournal.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devjournal.ui.theme.BookmarkPurple
import com.devjournal.ui.theme.BookmarkPurpleContainer
import com.devjournal.ui.theme.CommentBlue
import com.devjournal.ui.theme.LikeRed
import com.devjournal.ui.theme.LikeRedContainer
import com.devjournal.ui.theme.ViewCyan
import com.devjournal.ui.theme.ViewCyanContainer
import java.util.Locale

/**
 * Modern, tactile stat chip with distinct active/inactive visual states,
 * smooth spring scaling animations, and glassmorphic / tonal styling.
 */
@Composable
fun StatChip(
    icon: ImageVector,
    label: String? = null,
    contentDescription: String? = null,
    isActive: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    activeContainerColor: Color = activeColor.copy(alpha = 0.15f),
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveContainerColor: Color = Color.Transparent,
    showBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tintColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        label = "tintColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isActive) activeContainerColor else inactiveContainerColor,
        label = "containerColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = rememberRipple(bounded = true, color = activeColor),
                            onClick = onClick
                        )
                } else {
                    Modifier.clip(RoundedCornerShape(20.dp))
                }
            )
            .then(
                if (showBorder || isActive) {
                    Modifier.border(
                        width = 1.dp,
                        color = if (isActive) activeColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (label != null) 9.dp else 7.dp,
                vertical = 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tintColor,
                modifier = Modifier.size(16.dp)
            )

            if (!label.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = 0.1.sp
                    ),
                    color = tintColor
                )
            }
        }
    }
}

/**
 * Standardized Social Action Bar for Post Cards and Post Detail headers.
 */
@Composable
fun PostStatsBar(
    likeCount: Int,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    commentCount: Int,
    onCommentClick: () -> Unit,
    viewCount: Int,
    onViewsClick: (() -> Unit)? = null,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left actions: Like, Comment, Views
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Like Chip
            StatChip(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatStatCount(likeCount),
                contentDescription = if (isLiked) "Unlike post" else "Like post",
                isActive = isLiked,
                activeColor = LikeRed,
                activeContainerColor = LikeRedContainer,
                onClick = onLikeClick
            )

            // Comment Chip
            StatChip(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = formatStatCount(commentCount),
                contentDescription = "View comments",
                isActive = false,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onCommentClick
            )

            // Views Chip (Pill with subtle cyan tint)
            StatChip(
                icon = Icons.Outlined.Visibility,
                label = formatStatCount(viewCount),
                contentDescription = "$viewCount views",
                isActive = true,
                activeColor = ViewCyan,
                activeContainerColor = ViewCyanContainer,
                showBorder = true,
                onClick = onViewsClick
            )
        }

        // Right actions: Bookmark & Share
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Bookmark Chip (Strict state: vibrant purple ONLY when active, clear outline when unselected)
            StatChip(
                icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark post",
                isActive = isBookmarked,
                activeColor = BookmarkPurple,
                activeContainerColor = BookmarkPurpleContainer,
                showBorder = isBookmarked,
                onClick = onBookmarkClick
            )

            // Share Chip
            StatChip(
                icon = Icons.Outlined.Share,
                contentDescription = "Share post",
                isActive = false,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onShareClick
            )
        }
    }
}

/**
 * Formats large counts into human-readable shorthand (e.g. 1.2K, 3.4M).
 */
fun formatStatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
