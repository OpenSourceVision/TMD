package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MarkdownDocument::class], version = 2, exportSchema = false)
abstract class MarkdownDatabase : RoomDatabase() {
    abstract fun markdownDao(): MarkdownDao

    companion object {
        @Volatile
        private var INSTANCE: MarkdownDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE markdown_documents ADD COLUMN importedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                } catch (e: Exception) {
                    // Column might already exist in schema
                }
            }
        }

        fun getDatabase(context: Context): MarkdownDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarkdownDatabase::class.java,
                    "tmd_markdown_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
