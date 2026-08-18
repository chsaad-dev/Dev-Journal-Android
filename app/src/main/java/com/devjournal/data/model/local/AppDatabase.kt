package com.devjournal.data.model.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DraftEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
}
