package com.devjournal.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.devjournal.data.model.Post
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "bookmarked_posts")
data class BookmarkedPostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val slug: String = "",
    val content: String,
    val excerpt: String = "",
    val coverImageUrl: String = "",
    val coverImagePublicId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val tags: String = "", // comma-separated
    val published: Boolean = true,
    val readTimeMinutes: Int = 1,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val createdAtMs: Long = 0L,
    val bookmarkedAtMs: Long = System.currentTimeMillis()
)

fun BookmarkedPostEntity.toPost(): Post {
    return Post(
        id = id,
        title = title,
        slug = slug,
        content = content,
        excerpt = excerpt,
        coverImageUrl = coverImageUrl,
        coverImagePublicId = coverImagePublicId,
        authorId = authorId,
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        published = published,
        readTimeMinutes = readTimeMinutes,
        likeCount = likeCount,
        commentCount = commentCount,
        viewCount = viewCount,
        createdAt = if (createdAtMs > 0) Timestamp(Date(createdAtMs)) else null
    )
}

fun Post.toBookmarkedPostEntity(
    authorName: String = "",
    authorPhotoUrl: String = "",
    bookmarkedAtMs: Long = System.currentTimeMillis()
): BookmarkedPostEntity {
    return BookmarkedPostEntity(
        id = id,
        title = title,
        slug = slug,
        content = content,
        excerpt = excerpt,
        coverImageUrl = coverImageUrl,
        coverImagePublicId = coverImagePublicId,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        tags = tags.joinToString(","),
        published = published,
        readTimeMinutes = readTimeMinutes,
        likeCount = likeCount,
        commentCount = commentCount,
        viewCount = viewCount,
        createdAtMs = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        bookmarkedAtMs = bookmarkedAtMs
    )
}
