package com.devjournal.presentation.util

import android.content.Context
import android.content.Intent
import com.devjournal.data.model.Post

fun sharePost(context: Context, post: Post) {
    val shareUrl = DeepLinkUtils.buildPostWebUrl(post.id) + "?v=${post.updatedAt?.seconds ?: 0}"
    val shareText = "${post.title}\n\n$shareUrl"

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Article")
    context.startActivity(shareIntent)
}
