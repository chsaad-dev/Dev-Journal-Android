package com.devjournal.data.model.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DraftEntity::class, BookmarkedPostEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
    abstract fun bookmarkedPostDao(): BookmarkedPostDao
}
