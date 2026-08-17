package com.devjournal.presentation.util

import android.content.Context
import android.content.Intent
import com.devjournal.data.model.Post

fun sharePost(context: Context, post: Post) {
    val authorName = if (post.authorId.isNotBlank()) post.authorId else "Unknown"
    val excerpt = if (post.excerpt.isNotBlank()) post.excerpt else post.content.take(120)
    val url = "https://devjournal.app/post/${post.id}"
    
    val shareText = """
        Check out this article on DevJournal!
        
        ${post.title}
        By: $authorName
        
        $excerpt...
        
        Read more at: $url
    """.trimIndent()

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    
    val shareIntent = Intent.createChooser(sendIntent, "Share Article")
    context.startActivity(shareIntent)
}
