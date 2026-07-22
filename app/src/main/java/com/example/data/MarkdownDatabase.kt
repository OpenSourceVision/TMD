package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MarkdownDocument::class], version = 2, exportSchema = false)
abstract class MarkdownDatabase : RoomDatabase() {
    abstract fun markdownDao(): MarkdownDao

    companion object {
        @Volatile
        private var INSTANCE: MarkdownDatabase? = null

        fun getDatabase(context: Context): MarkdownDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarkdownDatabase::class.java,
                    "tmd_markdown_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
