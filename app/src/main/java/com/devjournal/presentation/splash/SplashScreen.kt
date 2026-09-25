package com.devjournal.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    initialPostId: String? = null,
    initialTargetUid: String? = null,
    onNavigate: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val scaleAnim = remember { Animatable(0.85f) }
    val alphaAnim = remember { Animatable(0f) }

    // Blinking cursor transition
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // Animated glow pulse
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    LaunchedEffect(Unit) {
        // Entrance animation
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(500)
        )

        // Staggered terminal typing sequence
        delay(250)
        step = 1 // $ devjournal --init
        delay(400)
        step = 2 // > Modules: [Compose, Firebase, Hilt] ✔
        delay(450)
        step = 3 // > Connecting developers... 200 OK
        delay(450)
        step = 4 // Ready

        delay(600)
        // Determine destination
        val target = if (!initialPostId.isNullOrBlank()) {
            "postdetail/$initialPostId"
        } else if (!initialTargetUid.isNullOrBlank()) {
            "profile?uid=$initialTargetUid"
        } else if (viewModel.isUserAuthenticated()) {
            "feed"
        } else {
            "login"
        }
        onNavigate(target)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14)),
        contentAlignment = Alignment.Center
    ) {
        // Background Ambient Glows
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(glowScale)
                .blur(70.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4F46E5).copy(alpha = 0.35f),
                            Color(0xFF4CD7F6).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Logo with Code Brackets
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF4CD7F6), fontWeight = FontWeight.Bold)) {
                            append("<")
                        }
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                            append("Dev")
                        }
                        withStyle(SpanStyle(color = Color(0xFFC3C0FF), fontWeight = FontWeight.ExtraBold)) {
                            append("Journal")
                        }
                        withStyle(SpanStyle(color = Color(0xFF4CD7F6), fontWeight = FontWeight.Bold)) {
                            append(" />")
                        }
                    },
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Tagline
            Text(
                text = "The Engineer's Knowledge Feed",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8E99B4),
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Terminal Window Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Terminal Header Bar with Window Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFFF5F56), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFFFBD2E), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF27C93F), CircleShape)
                            )
                        }

                        Text(
                            text = "bash ~ devjournal.sh",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.width(26.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Terminal Logs
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedVisibility(
                            visible = step >= 1,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color(0xFF38BDF8))) { append("$ ") }
                                    withStyle(SpanStyle(color = Color(0xFFF1F5F9))) { append("devjournal --boot --sync") }
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        AnimatedVisibility(
                            visible = step >= 2,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color(0xFF4ADE80))) { append("✔ ") }
                                    withStyle(SpanStyle(color = Color(0xFF94A3B8))) { append("Modules loaded: [Compose, Firestore, Markdown]") }
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        AnimatedVisibility(
                            visible = step >= 3,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color(0xFF4ADE80))) { append("✔ ") }
                                    withStyle(SpanStyle(color = Color(0xFF94A3B8))) { append("Connecting engineers... ") }
                                    withStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) { append("200 OK") }
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        AnimatedVisibility(
                            visible = step >= 4,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = Color(0xFFA78BFA), fontWeight = FontWeight.SemiBold)) {
                                            append("> Ready. Launching DevJournal")
                                        }
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "_",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.alpha(cursorAlpha)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Tech Stack Badge Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kotlin",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "•", fontSize = 11.sp, color = Color(0xFF475569))
                Text(
                    text = "Jetpack Compose",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "•", fontSize = 11.sp, color = Color(0xFF475569))
                Text(
                    text = "Firebase",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
