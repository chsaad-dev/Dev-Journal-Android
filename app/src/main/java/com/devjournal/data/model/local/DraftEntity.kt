package com.devjournal.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.devjournal.data.model.Post

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val excerpt: String,
    val tags: String, // comma separated
    val coverImageUri: String,
    val lastUpdated: Long
)

fun DraftEntity.toPost(authorId: String): Post {
    return Post(
        id = id,
        title = title,
        content = content,
        excerpt = excerpt,
        tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        coverImageUrl = coverImageUri.takeIf { it.startsWith("http") } ?: "",
        coverImagePublicId = "",
        authorId = authorId,
        published = false,
        likeCount = 0,
        commentCount = 0,
        readTimeMinutes = 1
    )
}

fun Post.toDraftEntity(): DraftEntity {
    return DraftEntity(
        id = id,
        title = title,
        content = content,
        excerpt = excerpt,
        tags = tags.joinToString(","),
        coverImageUri = coverImageUrl,
        lastUpdated = System.currentTimeMillis()
    )
}
