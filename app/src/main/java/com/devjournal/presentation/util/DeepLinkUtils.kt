package com.devjournal.presentation.util

import android.content.Intent
import android.net.Uri

object DeepLinkUtils {

    const val WEB_HOST = "devjournal-web.vercel.app"
    const val LEGACY_HOST = "devjournal.app"
    const val CUSTOM_SCHEME = "devjournal"

    fun buildPostWebUrl(postId: String): String {
        return "https://$WEB_HOST/posts/$postId"
    }

    fun buildPostDeepLink(postId: String): String {
        return "$CUSTOM_SCHEME://posts/$postId"
    }

    fun extractPostIdFromIntent(intent: Intent?): String? {
        if (intent == null) return null

        // 1. Direct extra from notification payload
        val extraPostId = intent.getStringExtra("postId")
        if (!extraPostId.isNullOrBlank()) {
            return extraPostId
        }

        // 2. Data URI
        val data = intent.data ?: return null
        return extractPostIdFromUri(data)
    }

    fun extractPostIdFromUri(data: Uri?): String? {
        if (data == null) return null
        return extractPostId(
            scheme = data.scheme,
            host = data.host,
            path = data.path,
            queryPostId = try {
                data.getQueryParameter("postId") ?: data.getQueryParameter("id")
            } catch (_: Exception) {
                null
            }
        )
    }

    fun extractPostId(
        scheme: String?,
        host: String?,
        path: String?,
        queryPostId: String? = null
    ): String? {
        if (!queryPostId.isNullOrBlank()) {
            return queryPostId
        }

        val normalizedScheme = scheme?.lowercase() ?: return null
        val normalizedHost = host?.lowercase() ?: ""
        val cleanPath = path?.trim('/') ?: ""
        val segments = cleanPath.split('/').filter { it.isNotBlank() }

        // App Link: https://devjournal-web.vercel.app/posts/{postId}
        if (normalizedScheme == "https" || normalizedScheme == "http") {
            if (normalizedHost == WEB_HOST || normalizedHost == LEGACY_HOST) {
                val postsIndex = segments.indexOf("posts")
                if (postsIndex != -1 && postsIndex + 1 < segments.size) {
                    val candidate = segments[postsIndex + 1]
                    if (candidate.isNotBlank()) {
                        return candidate
                    }
                }
            }
        }

        // Custom Scheme: devjournal://posts/{postId}
        if (normalizedScheme == CUSTOM_SCHEME) {
            if (normalizedHost == "posts") {
                val candidate = segments.firstOrNull()
                if (!candidate.isNullOrBlank()) {
                    return candidate
                }
            } else {
                val postsIndex = segments.indexOf("posts")
                if (postsIndex != -1 && postsIndex + 1 < segments.size) {
                    val candidate = segments[postsIndex + 1]
                    if (candidate.isNotBlank()) {
                        return candidate
                    }
                }
            }
        }

        return null
    }
}
