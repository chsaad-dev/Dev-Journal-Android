package com.devjournal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.devjournal.presentation.profile.ProfileScreen
import com.devjournal.presentation.settings.SettingsScreen
import com.devjournal.presentation.search.SearchScreen
import com.devjournal.presentation.splash.SplashScreen
import com.devjournal.ui.theme.DevJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var targetPostId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        targetPostId = intent?.getStringExtra("postId")

        setContent {
            DevJournalTheme {
                val navController = rememberNavController()

                LaunchedEffect(targetPostId) {
                    val currentTarget = targetPostId
                    if (!currentTarget.isNullOrBlank()) {
                        navController.navigate("postdetail/$currentTarget")
                        targetPostId = null
                    }
                }

                val initialPostId = intent?.getStringExtra("postId")

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        SplashScreen(
                            initialPostId = initialPostId,
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
                            }
                        )
                    }
                    composable("feed") {
                        FeedScreen(
                            onPostClick = { postId ->
                                navController.navigate("postdetail/$postId")
                            },
                            onProfileClick = {
                                navController.navigate("profile")
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
                            onSearchClick = {
                                navController.navigate("search")
                            },
                            onAuthorClick = { authorId ->
                                navController.navigate("profile?uid=$authorId")
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
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() },
                            onSignedOut = {
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("notifications") {
                        NotificationsScreen(
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            onPostClick = { postId ->
                                navController.navigate("postdetail/$postId")
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
        val postId = intent.getStringExtra("postId")
        if (!postId.isNullOrBlank()) {
            targetPostId = postId
        }
    }
}