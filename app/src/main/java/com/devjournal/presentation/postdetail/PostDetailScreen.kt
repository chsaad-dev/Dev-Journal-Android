package com.devjournal.presentation.postdetail

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.devjournal.data.model.Comment
import com.devjournal.presentation.components.BottomImageScrim
import com.devjournal.presentation.components.EdgeFadeHorizontalRow
import com.devjournal.presentation.components.GlassIconButton
import com.devjournal.presentation.components.PostActionMenu
import com.devjournal.presentation.components.StatChip
import com.devjournal.presentation.components.TopImageScrim
import com.devjournal.presentation.util.sharePost
import com.devjournal.ui.theme.BookmarkPurple
import com.devjournal.ui.theme.BookmarkPurpleContainer
import com.devjournal.ui.theme.BorderSubtleDark
import com.devjournal.ui.theme.LikeRed
import com.devjournal.ui.theme.LikeRedContainer
import com.devjournal.ui.theme.ViewCyan
import com.devjournal.ui.theme.ViewCyanContainer
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel = hiltViewModel(),
    onAuthorClick: (String) -> Unit = {},
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val post = uiState.post
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val hasCover = !post?.coverImageUrl.isNullOrBlank()
    // Smooth scroll-driven TopAppBar elevation & title reveal
    val isScrolled by remember { derivedStateOf { scrollState.value > 160 } }
    val showSolidTopBar = isScrolled || !hasCover

    LaunchedEffect(uiState.postDeleted) {
        if (uiState.postDeleted) {
            onBackClick()
        }
    }

    var showDeletePostDialog by remember { mutableStateOf(false) }
    var commentIdToDelete by remember { mutableStateOf<String?>(null) }

    if (showDeletePostDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePostDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletePostDialog = false
                        viewModel.onDeletePost()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (commentIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { commentIdToDelete = null },
            title = { Text("Delete Comment") },
            text = { Text("Are you sure you want to delete this comment?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        commentIdToDelete?.let { viewModel.onDeleteComment(it) }
                        commentIdToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { commentIdToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showSolidTopBar,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = post?.title ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    if (showSolidTopBar) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBackClick,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                },
                actions = {
                    if (showSolidTopBar) {
                        // Bookmarked action
                        IconButton(onClick = { viewModel.onBookmarkClick() }) {
                            Icon(
                                imageVector = if (uiState.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (uiState.isBookmarked) "Saved" else "Save Article",
                                tint = if (uiState.isBookmarked) BookmarkPurple else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Share action
                        IconButton(onClick = { post?.let { sharePost(context, it) } }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Post owner menu
                        if (post != null && (uiState.isAdmin || uiState.currentUserId == post.authorId)) {
                            PostActionMenu(
                                canEdit = false, // Post editor navigated via feed or profile
                                canDelete = true,
                                onEditClick = null,
                                onDeleteClick = { showDeletePostDialog = true }
                            )
                        }
                    } else {
                        GlassIconButton(
                            icon = if (uiState.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (uiState.isBookmarked) "Saved" else "Save Article",
                            tint = if (uiState.isBookmarked) BookmarkPurple else Color.White,
                            onClick = { viewModel.onBookmarkClick() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassIconButton(
                            icon = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            onClick = { post?.let { sharePost(context, it) } }
                        )
                        if (post != null && (uiState.isAdmin || uiState.currentUserId == post.authorId)) {
                            Spacer(modifier = Modifier.width(8.dp))
                            PostActionMenu(
                                canEdit = false,
                                canDelete = true,
                                onEditClick = null,
                                onDeleteClick = { showDeletePostDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showSolidTopBar) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.Transparent
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.commentInput,
                        onValueChange = { viewModel.onCommentInputChange(it) },
                        placeholder = {
                            Text(
                                "Add a thoughtful comment...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.onSubmitComment() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderSubtleDark,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.onSubmitComment() },
                        enabled = uiState.commentInput.isNotBlank() && !uiState.isSubmittingComment,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (uiState.commentInput.isNotBlank() && !uiState.isSubmittingComment)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        if (uiState.isSubmittingComment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Comment",
                                tint = if (uiState.commentInput.isNotBlank())
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (post == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Article not found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // Cover Image with Top & Bottom Scrims
                if (post.coverImageUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        AsyncImage(
                            model = post.coverImageUrl,
                            contentDescription = "Cover Image for ${post.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Top scrim guarantees back/share/bookmark visibility
                        TopImageScrim(height = 110.dp, maxAlpha = 0.75f)
                        // Bottom scrim creates a smooth fade into surface
                        BottomImageScrim(height = 80.dp, maxAlpha = 0.65f)
                    }
                } else {
                    Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding() + 8.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Title
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Author Row (Clean, modern byline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (post.authorId.isNotBlank()) {
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onAuthorClick(post.authorId) }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.authorPhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = uiState.authorPhotoUrl,
                                contentDescription = "Author Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, BorderSubtleDark, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.5.dp, BorderSubtleDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Author Avatar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val authorDisplayName = if (uiState.authorName.isNotBlank()) {
                                uiState.authorName
                            } else if (post.authorId.isNotBlank()) {
                                "Developer (${post.authorId.take(6)})"
                            } else {
                                "DevJournal Author"
                            }

                            Text(
                                text = authorDisplayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatDetailTimestamp(post.createdAt?.toDate()?.time)} • ${post.readTimeMinutes.coerceAtLeast(1)} min read",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tag Chips Row with edge fade
                    if (post.tags.isNotEmpty()) {
                        val detailTagScrollState = rememberScrollState()
                        EdgeFadeHorizontalRow(
                            scrollState = detailTagScrollState,
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            post.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
                                ) {
                                    Text(
                                        text = if (tag.startsWith("#")) tag else "#$tag",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Excerpt (Styled as an italic lead paragraph)
                    if (post.excerpt.isNotBlank()) {
                        Text(
                            text = post.excerpt,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    HorizontalDivider(color = BorderSubtleDark, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Complete Styled Markdown Content
                    RenderMarkdownBody(content = post.content)

                    Spacer(modifier = Modifier.height(28.dp))

                    HorizontalDivider(color = BorderSubtleDark, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Unified Social Engagement Row (Likes, Comments, Views)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Like Chip
                        StatChip(
                            icon = if (uiState.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            label = "${post.likeCount} likes",
                            contentDescription = if (uiState.isLiked) "Unlike" else "Like",
                            isActive = uiState.isLiked,
                            activeColor = LikeRed,
                            activeContainerColor = LikeRedContainer,
                            showBorder = true,
                            onClick = { viewModel.onLikeClick() }
                        )

                        // Comment Chip
                        StatChip(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            label = "${uiState.comments.size} comments",
                            contentDescription = "Comments",
                            isActive = false,
                            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            showBorder = true,
                            onClick = {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                        )

                        // Views Chip (Clickable for author / admin to open reader list)
                        StatChip(
                            icon = Icons.Outlined.Visibility,
                            label = "${post.viewCount} views",
                            contentDescription = "View count",
                            isActive = uiState.canViewReadersList,
                            activeColor = ViewCyan,
                            activeContainerColor = ViewCyanContainer,
                            showBorder = true,
                            onClick = {
                                if (uiState.canViewReadersList) {
                                    viewModel.onOpenViewersSheet()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "View count is public. Detailed reader list is visible to the author and admins.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Comments Header
                    Text(
                        text = "Discussion (${uiState.comments.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.comments.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
                        ) {
                            Text(
                                text = "No comments yet. Share your thoughts or ask a question!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.comments.forEach { comment ->
                                CommentItem(
                                    comment = comment,
                                    currentUserId = uiState.currentUserId,
                                    isAdmin = uiState.isAdmin,
                                    commenterName = uiState.commenterNames[comment.userId],
                                    commenterPhotoUrl = uiState.commenterPhotoUrls[comment.userId],
                                    onDelete = { commentIdToDelete = comment.id }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (uiState.isViewersSheetOpen) {
        ReadByBottomSheet(
            viewers = uiState.viewers,
            isLoading = uiState.isLoadingViewers,
            onDismissRequest = { viewModel.onCloseViewersSheet() },
            onUserClick = { uid ->
                viewModel.onCloseViewersSheet()
                onAuthorClick(uid)
            }
        )
    }
}

/**
 * Modern comment card with strict ownership check for delete action,
 * user avatar ring, and clean typography.
 */
@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    isAdmin: Boolean,
    commenterName: String?,
    commenterPhotoUrl: String?,
    onDelete: () -> Unit
) {
    val isOwner = !currentUserId.isNullOrBlank() && currentUserId == comment.userId
    val canDelete = isOwner || isAdmin

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Commenter Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, BorderSubtleDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!commenterPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = commenterPhotoUrl,
                        contentDescription = "Commenter Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Commenter Avatar",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayName = if (!commenterName.isNullOrBlank()) {
                        commenterName
                    } else if (comment.userId.isNotBlank()) {
                        "User (${comment.userId.take(6)})"
                    } else {
                        "Developer"
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatDetailTimestamp(comment.createdAt?.toDate()?.time),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Strict comment delete action: only owner or verified admin
                        if (canDelete) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Comment",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatDetailTimestamp(timeMs: Long?): String {
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
