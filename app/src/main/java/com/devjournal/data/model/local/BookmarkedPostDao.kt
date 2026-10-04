package com.devjournal.data.model.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkedPostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: BookmarkedPostEntity)

    @Query("DELETE FROM bookmarked_posts WHERE id = :postId")
    suspend fun deleteById(postId: String)

    @Query("SELECT * FROM bookmarked_posts ORDER BY bookmarkedAtMs DESC")
    fun getAllBookmarkedPosts(): Flow<List<BookmarkedPostEntity>>

    @Query("SELECT * FROM bookmarked_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): BookmarkedPostEntity?

    @Query("SELECT * FROM bookmarked_posts WHERE id = :postId LIMIT 1")
    fun observePostById(postId: String): Flow<BookmarkedPostEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_posts WHERE id = :postId)")
    fun isBookmarked(postId: String): Flow<Boolean>

    @Query("DELETE FROM bookmarked_posts")
    suspend fun clearAll()
}
