package com.devjournal.presentation.feed

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.devjournal.presentation.components.EdgeFadeHorizontalRow
import com.devjournal.presentation.profile.ProfileScreen
import com.devjournal.presentation.search.SearchScreen
import com.devjournal.presentation.util.sharePost
import kotlinx.coroutines.launch

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    onPostClick: (String) -> Unit,
    onNewPostClick: () -> Unit,
    onEditPostClick: (String) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onDraftClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onFollowersClick: (String) -> Unit = {},
    onFollowingClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeListState = rememberLazyListState()
    val feedListState = rememberLazyListState()
    val pagerState = rememberPagerState(initialPage = 0) { 4 }
    var postToDeleteId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val homePullRefreshState = rememberPullToRefreshState()
    val feedPullRefreshState = rememberPullToRefreshState()

    if (homePullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshFeed()
        }
    }
    if (feedPullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshFeed()
        }
    }
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            homePullRefreshState.endRefresh()
            feedPullRefreshState.endRefresh()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage
        if (!error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearErrorMessage()
        }
    }

    // Intercept physical/gesture Back button when not on Home page (directly switch to Home)
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.scrollToPage(0)
        }
    }

    // Trigger loadMorePosts when scrolled near the bottom of discovery feed
    LaunchedEffect(feedListState) {
        snapshotFlow { feedListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= uiState.posts.size - 3) {
                    viewModel.loadMorePosts()
                }
            }
    }

    // Trigger loadMorePosts when scrolled near the bottom of home following feed
    LaunchedEffect(homeListState) {
        snapshotFlow { homeListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= uiState.followingPosts.size - 3) {
                    viewModel.loadMorePosts()
                }
            }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* permission result handled */ }

        LaunchedEffect(Unit) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (postToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { postToDeleteId = null },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        postToDeleteId?.let { viewModel.onDeletePost(it) }
                        postToDeleteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage == 0) {
                                homeListState.animateScrollToItem(0)
                            } else {
                                pagerState.scrollToPage(0)
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (pagerState.currentPage == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage == 1) {
                                feedListState.animateScrollToItem(0)
                            } else {
                                pagerState.scrollToPage(1)
                                if (uiState.selectedTab == FeedTab.FOLLOWING) {
                                    viewModel.onTabSelected(FeedTab.FOR_YOU)
                                }
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (pagerState.currentPage == 1) Icons.Filled.DynamicFeed else Icons.Outlined.DynamicFeed,
                            contentDescription = "Feed"
                        )
                    },
                    label = { Text("Feed") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(2)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (pagerState.currentPage == 2) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    },
                    label = { Text("Search") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 3,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(3)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (pagerState.currentPage == 3) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        // Page 0: Home (Following feed only)
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = "DevJournal",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = onNotificationsClick) {
                                            Icon(
                                                imageVector = Icons.Outlined.Notifications,
                                                contentDescription = "Notifications",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            },
                            floatingActionButton = {
                                if (uiState.currentUserId != null) {
                                    FloatingActionButton(
                                        onClick = onNewPostClick,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Post"
                                        )
                                    }
                                }
                            }
                        ) { homePadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(homePadding)
                                    .nestedScroll(homePullRefreshState.nestedScrollConnection)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                if (uiState.currentUserId == null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(18.dp))
                                            Text(
                                                text = "Sign in to see followed authors",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Log in and follow your favorite engineers to personalize your Home feed.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else if (uiState.isFollowingLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                } else if (uiState.followingPosts.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PeopleOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(18.dp))
                                            Text(
                                                text = "Your Home feed is quiet",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Follow other engineers and creators to see their latest tutorials, code snippets, and stories here.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(1)
                                                        viewModel.onTabSelected(FeedTab.FOR_YOU)
                                                    }
                                                },
                                                shape = RoundedCornerShape(20.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            ) {
                                                Text("Explore Community Feed")
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(2)
                                                    }
                                                },
                                                shape = RoundedCornerShape(20.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Text("Find Creators to Follow")
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = homeListState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(
                                            items = uiState.followingPosts,
                                            key = { it.id },
                                            contentType = { "post_card" }
                                        ) { post ->
                                            PostCard(
                                                post = post,
                                                authorName = uiState.authorNames[post.authorId] ?: "",
                                                authorPhotoUrl = uiState.authorPhotoUrls[post.authorId] ?: "",
                                                isLiked = uiState.likedPostIds.contains(post.id),
                                                isBookmarked = uiState.bookmarkedPostIds.contains(post.id),
                                                onLikeClick = {
                                                    if (uiState.currentUserId == null) {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        coroutineScope.launch { snackbarHostState.showSnackbar("Sign in to like articles") }
                                                    } else {
                                                        viewModel.onLikeClick(post.id, uiState.likedPostIds.contains(post.id))
                                                    }
                                                },
                                                onBookmarkClick = {
                                                    if (uiState.currentUserId == null) {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        coroutineScope.launch { snackbarHostState.showSnackbar("Sign in to save articles") }
                                                    } else {
                                                        viewModel.onBookmarkClick(post.id, uiState.bookmarkedPostIds.contains(post.id))
                                                    }
                                                },
                                                onCommentClick = { onPostClick(post.id) },
                                                onShareClick = { sharePost(context, post) },
                                                onPostClick = { onPostClick(post.id) },
                                                canEdit = uiState.isAdmin || (uiState.currentUserId != null && post.authorId == uiState.currentUserId),
                                                canDelete = uiState.isAdmin || (uiState.currentUserId != null && post.authorId == uiState.currentUserId),
                                                onEditClick = { onEditPostClick(post.id) },
                                                onDeleteClick = { postToDeleteId = post.id },
                                                onAuthorClick = onAuthorClick
                                            )
                                        }
                                    }
                                }
                                }
                                PullToRefreshContainer(
                                    state = homePullRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                    }
                    1 -> {
                        // Page 1: Feed (Community discovery feed)
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = "DevJournal",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = onNotificationsClick) {
                                            Icon(
                                                imageVector = Icons.Outlined.Notifications,
                                                contentDescription = "Notifications",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            },
                            floatingActionButton = {
                                if (uiState.currentUserId != null) {
                                    FloatingActionButton(
                                        onClick = onNewPostClick,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Post"
                                        )
                                    }
                                }
                            }
                        ) { feedPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(feedPadding)
                                    .nestedScroll(feedPullRefreshState.nestedScrollConnection)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                TabRow(
                                    selectedTabIndex = if (uiState.selectedTab == FeedTab.TRENDING) 1 else 0,
                                    containerColor = MaterialTheme.colorScheme.background,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    divider = {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                                    }
                                ) {
                                    Tab(
                                        selected = uiState.selectedTab == FeedTab.FOR_YOU,
                                        onClick = { viewModel.onTabSelected(FeedTab.FOR_YOU) },
                                        text = { Text("For You", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                                    )
                                    Tab(
                                        selected = uiState.selectedTab == FeedTab.TRENDING,
                                        onClick = { viewModel.onTabSelected(FeedTab.TRENDING) },
                                        text = { Text("Trending", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                                    )
                                }

                                // Horizontal Tag Filter Chips with edge gradient scroll affordance
                                val tagScrollState = rememberScrollState()
                                EdgeFadeHorizontalRow(
                                    scrollState = tagScrollState,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.availableTags.forEach { tag ->
                                        val isSelected = uiState.selectedTag.equals(tag, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.onTagSelected(tag) },
                                            label = {
                                                Text(
                                                    text = if (tag.equals("All", ignoreCase = true)) "#All" else if (tag.startsWith("#")) tag else "#$tag",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                borderWidth = 1.dp
                                            )
                                        )
                                    }
                                }

                                // Post list / Loading / Empty state
                                if (uiState.isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                } else if (uiState.posts.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "No articles found",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Try switching to another category or check back later.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = feedListState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(
                                            items = uiState.posts,
                                            key = { it.id },
                                            contentType = { "post_card" }
                                        ) { post ->
                                            PostCard(
                                                post = post,
                                                authorName = uiState.authorNames[post.authorId] ?: "",
                                                authorPhotoUrl = uiState.authorPhotoUrls[post.authorId] ?: "",
                                                isLiked = uiState.likedPostIds.contains(post.id),
                                                isBookmarked = uiState.bookmarkedPostIds.contains(post.id),
                                                onLikeClick = {
                                                    if (uiState.currentUserId == null) {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        coroutineScope.launch { snackbarHostState.showSnackbar("Sign in to like articles") }
                                                    } else {
                                                        viewModel.onLikeClick(post.id, uiState.likedPostIds.contains(post.id))
                                                    }
                                                },
                                                onBookmarkClick = {
                                                    if (uiState.currentUserId == null) {
                                                        snackbarHostState.currentSnackbarData?.dismiss()
                                                        coroutineScope.launch { snackbarHostState.showSnackbar("Sign in to save articles") }
                                                    } else {
                                                        viewModel.onBookmarkClick(post.id, uiState.bookmarkedPostIds.contains(post.id))
                                                    }
                                                },
                                                onCommentClick = { onPostClick(post.id) },
                                                onShareClick = { sharePost(context, post) },
                                                onPostClick = { onPostClick(post.id) },
                                                canEdit = uiState.isAdmin || (uiState.currentUserId != null && post.authorId == uiState.currentUserId),
                                                canDelete = uiState.isAdmin || (uiState.currentUserId != null && post.authorId == uiState.currentUserId),
                                                onEditClick = { onEditPostClick(post.id) },
                                                onDeleteClick = { postToDeleteId = post.id },
                                                onAuthorClick = onAuthorClick
                                            )
                                        }
                                    }
                                }
                                }
                                PullToRefreshContainer(
                                    state = feedPullRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                    }
                    2 -> {
                        // Page 2: Search (Articles & Accounts)
                        SearchScreen(
                            onPostClick = onPostClick,
                            onUserClick = onAuthorClick,
                            onBackClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            },
                            showBackButton = false
                        )
                    }
                    3 -> {
                        // Page 3: Profile
                        ProfileScreen(
                            onPostClick = onPostClick,
                            onDraftClick = onDraftClick,
                            onSettingsClick = onSettingsClick,
                            onEditProfileClick = onEditProfileClick,
                            onFollowersClick = onFollowersClick,
                            onFollowingClick = onFollowingClick,
                            onBackClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            },
                            showBackButton = false
                        )
                    }
                }
            }
        }
    }
}
