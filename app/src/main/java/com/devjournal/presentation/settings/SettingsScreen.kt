package com.devjournal.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devjournal.BuildConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onSignedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onSignedOut()
        }
    }

    val showStubMessage: () -> Unit = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Coming soon", duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // SECTION: Account
            item { SettingsSectionHeader("ACCOUNT") }
            item {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "Change Email",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Link,
                    title = "Linked Accounts",
                    trailing = { Text("Google", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = showStubMessage
                )
            }

            // SECTION: Notifications
            item { SettingsSectionHeader("NOTIFICATIONS") }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Push Notifications",
                    initialValue = true
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "New Comments",
                    initialValue = true
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.FavoriteBorder,
                    title = "New Likes",
                    initialValue = true
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.PersonAdd,
                    title = "New Followers",
                    initialValue = true
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "New Posts from people you follow",
                    initialValue = false
                )
            }

            // SECTION: Appearance
            item { SettingsSectionHeader("APPEARANCE") }
            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    trailing = { Text("System", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.FormatSize,
                    title = "Reading Font Size",
                    trailing = { Text("Medium", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = showStubMessage
                )
            }

            // SECTION: Privacy
            item { SettingsSectionHeader("PRIVACY") }
            item {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Lock,
                    title = "Private Account",
                    initialValue = false
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = "Blocked Users",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Chat,
                    title = "Who Can Comment",
                    trailing = { Text("Everyone", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = showStubMessage
                )
            }

            // SECTION: Content
            item { SettingsSectionHeader("CONTENT") }
            item {
                SettingsItem(
                    icon = Icons.Default.BookmarkBorder,
                    title = "Saved Posts",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.FavoriteBorder,
                    title = "Liked Posts",
                    onClick = showStubMessage
                )
            }
            if (uiState.isAdmin) {
                item {
                    SettingsItem(
                        icon = Icons.Default.Edit,
                        title = "My Posts / Drafts",
                        onClick = showStubMessage
                    )
                }
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Download My Data",
                    onClick = showStubMessage
                )
            }

            // SECTION: Support
            item { SettingsSectionHeader("SUPPORT") }
            item {
                SettingsItem(
                    icon = Icons.Default.HelpOutline,
                    title = "Help Center",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Flag,
                    title = "Report a Problem",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Feedback,
                    title = "Send Feedback",
                    onClick = showStubMessage
                )
            }

            // SECTION: About
            item { SettingsSectionHeader("ABOUT") }
            item {
                ListItem(
                    headlineContent = { Text("App Version") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    trailingContent = {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    onClick = showStubMessage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Open Source Licenses",
                    onClick = showStubMessage
                )
            }

            // SECTION: Danger Zone
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { viewModel.onSignOutClick() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { showStubMessage() }
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = trailing,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    initialValue: Boolean
) {
    var checked by remember { mutableStateOf(initialValue) }
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null // handled by ListItem click
            )
        },
        modifier = Modifier.clickable { checked = !checked }
    )
}
