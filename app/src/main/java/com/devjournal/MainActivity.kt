package com.devjournal

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.devjournal.presentation.auth.LoginScreen
import com.devjournal.presentation.editor.PostEditorScreen
import com.devjournal.presentation.feed.FeedScreen
import com.devjournal.presentation.notifications.NotificationsScreen
import com.devjournal.presentation.postdetail.PostDetailScreen
import com.devjournal.presentation.followlist.FollowListScreen
import com.devjournal.presentation.profile.ProfileScreen
import com.devjournal.presentation.profile.EditProfileScreen
import com.devjournal.presentation.settings.SettingsScreen
import com.devjournal.presentation.settings.PrivacySettingsScreen
import com.devjournal.presentation.settings.LinkedAccountsScreen
import com.devjournal.presentation.support.ReportBugScreen
import com.devjournal.presentation.support.HelpCenterScreen
import com.devjournal.presentation.support.MyReportsScreen
import com.devjournal.presentation.guide.AppGuideScreen
import com.devjournal.presentation.search.SearchScreen
import com.devjournal.presentation.splash.SplashScreen
import com.devjournal.presentation.util.DeepLinkUtils
import androidx.core.view.WindowCompat
import com.devjournal.ui.theme.DevJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var targetPostId by mutableStateOf<String?>(null)
    private var targetUid by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableHighRefreshRate()
        val resolvedPostId = extractPostIdFromIntent(intent)
        targetPostId = resolvedPostId
        targetUid = intent?.getStringExtra("targetUid")

        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { result ->
            Log.d("DevJournal", "Current user UID: ${FirebaseAuth.getInstance().currentUser?.uid}")
            Log.d("DevJournal", "Current user claims: ${result.claims}")
        }?.addOnFailureListener {
            Log.e("DevJournal", "Failed to get token claims", it)
        }

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsState()
            val unreadNotificationCount by mainViewModel.unreadNotificationCount.collectAsState()

            DevJournalTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                LaunchedEffect(targetPostId) {
                    val currentTarget = targetPostId
                    if (!currentTarget.isNullOrBlank()) {
                        navController.navigate("postdetail/$currentTarget")
                        targetPostId = null
                    }
                }

                LaunchedEffect(targetUid) {
                    val currentTarget = targetUid
                    if (!currentTarget.isNullOrBlank()) {
                        navController.navigate("profile?uid=$currentTarget")
                        targetUid = null
                    }
                }

                val initialPostId = extractPostIdFromIntent(intent)
                val initialTargetUid = intent?.getStringExtra("targetUid")

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        SplashScreen(
                            initialPostId = initialPostId,
                            initialTargetUid = initialTargetUid,
                            onNavigate = { destination ->
                                navController.navigate(destination) {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onAuthenticated = {
                                navController.navigate("feed") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToAppGuide = {
                                navController.navigate("app-guide") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("feed") {
                        FeedScreen(
                            onPostClick = { postId ->
                                navController.navigate("postdetail/$postId")
                            },
                            onNewPostClick = {
                                navController.navigate("editor")
                            },
                            onEditPostClick = { postId ->
                                navController.navigate("editor/$postId")
                            },
                            onNotificationsClick = {
                                navController.navigate("notifications")
                            },
                            unreadNotificationCount = unreadNotificationCount,
                            onAuthorClick = { authorId ->
                                navController.navigate("profile?uid=$authorId")
                            },
                            onDraftClick = { draftId ->
                                navController.navigate("editor?draftId=$draftId")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            },
                            onEditProfileClick = {
                                navController.navigate("editprofile")
                            },
                            onFollowersClick = { uid ->
                                navController.navigate("followlist/$uid/followers")
                            },
                            onFollowingClick = { uid ->
                                navController.navigate("followlist/$uid/following")
                            }
                        )
                    }
                    composable(
                        route = "postdetail/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) {
                        PostDetailScreen(
                            onAuthorClick = { authorId ->
                                navController.navigate("profile?uid=$authorId")
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = "profile?uid={uid}",
                        arguments = listOf(navArgument("uid") { type = NavType.StringType; nullable = true; defaultValue = null })
                    ) {
                        ProfileScreen(
                            onPostClick = { postId ->
                                navController.navigate("postdetail/$postId")
                            },
                            onDraftClick = { draftId ->
                                navController.navigate("editor?draftId=$draftId")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            },
                            onEditProfileClick = {
                                navController.navigate("editprofile")
                            },
                            onFollowersClick = { uid ->
                                navController.navigate("followlist/$uid/followers")
                            },
                            onFollowingClick = { uid ->
                                navController.navigate("followlist/$uid/following")
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = "followlist/{uid}/{type}",
                        arguments = listOf(
                            navArgument("uid") { type = NavType.StringType },
                            navArgument("type") { type = NavType.StringType }
                        )
                    ) {
                        FollowListScreen(
                            onUserClick = { userId ->
                                navController.navigate("profile?uid=$userId")
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() },
                            onEditProfileClick = {
                                navController.navigate("editprofile")
                            },
                            onPrivacyClick = {
                                navController.navigate("settings/privacy")
                            },
                            onLinkedAccountsClick = {
                                navController.navigate("settings/linked-accounts")
                            },
                            onReportBugClick = {
                                navController.navigate("report-bug")
                            },
                            onMyReportsClick = {
                                navController.navigate("my-reports")
                            },
                            onHelpCenterClick = {
                                navController.navigate("help-center")
                            },
                            onAppGuideClick = {
                                navController.navigate("app-guide")
                            },
                            onSignedOut = {
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("report-bug") {
                        ReportBugScreen(
                            onBackClick = { navController.popBackStack() },
                            onViewReportsClick = { navController.navigate("my-reports") }
                        )
                    }
                    composable("my-reports") {
                        MyReportsScreen(
                            onBackClick = { navController.popBackStack() },
                            onNewReportClick = { navController.navigate("report-bug") }
                        )
                    }
                    composable("help-center") {
                        HelpCenterScreen(
                            onBackClick = { navController.popBackStack() },
                            onReportBugClick = { navController.navigate("report-bug") }
                        )
                    }
                    composable("app-guide") {
                        AppGuideScreen(
                            onFinish = {
                                val previousRoute = navController.previousBackStackEntry?.destination?.route
                                if (previousRoute != null && (previousRoute == "settings" || previousRoute.startsWith("settings"))) {
                                    navController.popBackStack()
                                } else {
                                    navController.navigate("feed") {
                                        popUpTo("app-guide") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable("editprofile") {
                        EditProfileScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("settings/privacy") {
                        PrivacySettingsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("settings/linked-accounts") {
                        LinkedAccountsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("notifications") {
                        NotificationsScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onNavigateToPost = { postId ->
                                navController.navigate("postdetail/$postId")
                            },
                            onNavigateToProfile = { userId ->
                                navController.navigate("profile?uid=$userId")
                            }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            onPostClick = { postId ->
                                navController.navigate("postdetail/$postId")
                            },
                            onUserClick = { userId ->
                                navController.navigate("profile?uid=$userId")
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("editor") {
                        PostEditorScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onSaved = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = "editor/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) {
                        PostEditorScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onSaved = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = "editor?draftId={draftId}",
                        arguments = listOf(navArgument("draftId") { type = NavType.StringType; nullable = true; defaultValue = null })
                    ) {
                        PostEditorScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onSaved = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val postId = extractPostIdFromIntent(intent)
        if (!postId.isNullOrBlank()) {
            targetPostId = postId
        }
        val uid = intent.getStringExtra("targetUid")
        if (!uid.isNullOrBlank()) {
            targetUid = uid
        }
    }

    private fun extractPostIdFromIntent(intent: Intent?): String? {
        return DeepLinkUtils.extractPostIdFromIntent(intent)
    }

    private fun enableHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val modes = currentDisplay?.supportedModes ?: emptyArray()
                val maxMode = modes.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.refreshRate > 60f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setPreferMinimalPostProcessing(true)
            }
        } catch (e: Exception) {
            Log.w("DevJournal", "Could not set high refresh rate: ${e.message}")
        }
    }
}