package com.devjournal.presentation.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.devjournal.data.model.Post
import com.devjournal.presentation.components.BottomImageScrim
import com.devjournal.presentation.components.PostActionMenu
import com.devjournal.presentation.components.PostStatsBar
import com.devjournal.ui.theme.BorderSubtleDark
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PostCard(
    post: Post,
    authorName: String = "",
    authorPhotoUrl: String = "",
    isLiked: Boolean,
    isBookmarked: Boolean,
    onLikeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onPostClick: () -> Unit,
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onAuthorClick: ((String) -> Unit)? = null,
    canEdit: Boolean = false,
    canDelete: Boolean = false,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtleDark, RoundedCornerShape(16.dp))
            .clickable { onPostClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cover Image with Scrim and Tag Overlay
            if (post.coverImageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    AsyncImage(
                        model = post.coverImageUrl,
                        contentDescription = "Cover Image for ${post.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bottom scrim to lift the tag chip
                    BottomImageScrim(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        height = 64.dp,
                        maxAlpha = 0.75f
                    )

                    // Tag chip overlay
                    val primaryTag = post.tags.firstOrNull() ?: "Engineering"
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.70f)
                    ) {
                        Text(
                            text = if (primaryTag.startsWith("#")) primaryTag else "#$primaryTag",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Author row (Compact, professional byline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (onAuthorClick != null && post.authorId.isNotBlank()) {
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onAuthorClick(post.authorId) }
                                        .padding(vertical = 2.dp, horizontal = 2.dp)
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Author Avatar with subtle border ring
                        if (authorPhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = authorPhotoUrl,
                                contentDescription = "Author Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, BorderSubtleDark, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.dp, BorderSubtleDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Author Avatar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            val displayName = if (authorName.isNotBlank()) {
                                authorName
                            } else if (post.authorId.isNotBlank()) {
                                "Developer (${post.authorId.take(6)})"
                            } else {
                                "DevJournal Author"
                            }

                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatTimestamp(post.createdAt?.toDate()?.time),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = " • ${post.readTimeMinutes.coerceAtLeast(1)} min read",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Anchored 3-dot menu with icon parity (Edit & Delete both have icons)
                    PostActionMenu(
                        canEdit = canEdit,
                        canDelete = canDelete,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Post Title
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Post Excerpt
                val displayText = if (post.excerpt.isNotBlank()) post.excerpt else post.content.take(120)
                if (displayText.isNotBlank()) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tag chips for text-only posts
                if (post.coverImageUrl.isBlank() && post.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        post.tags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, BorderSubtleDark)
                            ) {
                                Text(
                                    text = if (tag.startsWith("#")) tag else "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Unified Social Stats Bar
                PostStatsBar(
                    likeCount = post.likeCount,
                    isLiked = isLiked,
                    onLikeClick = onLikeClick,
                    commentCount = post.commentCount,
                    onCommentClick = onCommentClick,
                    viewCount = post.viewCount,
                    onViewsClick = null,
                    isBookmarked = isBookmarked,
                    onBookmarkClick = onBookmarkClick,
                    onShareClick = onShareClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatTimestamp(timeMs: Long?): String {
    if (timeMs == null) return "Just now"
    val diff = System.currentTimeMillis() - timeMs
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(timeMs)
    }
}
