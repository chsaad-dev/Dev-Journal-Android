package com.devjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devjournal.presentation.auth.LoginScreen
import com.devjournal.presentation.editor.PostEditorScreen
import com.devjournal.presentation.feed.FeedScreen
import com.devjournal.presentation.postdetail.PostDetailScreen
import com.devjournal.presentation.profile.ProfileScreen
import com.devjournal.ui.theme.DevJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DevJournalTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
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
                            }
                        )
                    }
                    composable(
                        route = "postdetail/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString("postId") ?: ""
                        PostDetailScreen(
                            postId = postId,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("profile") {
                        ProfileScreen(
                            onPostClick = { postId ->
                                navController.navigate("postdetail/$postId")
                            },
                            onSignedOut = {
                                navController.navigate("login") {
                                    popUpTo("feed") { inclusive = true }
                                }
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("editor") {
                        PostEditorScreen(
                            postId = null,
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
                    ) { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString("postId") ?: ""
                        PostEditorScreen(
                            postId = postId,
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
}

@Composable
fun ScreenPlaceholder(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}