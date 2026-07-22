package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkdownDao {
    @Query("SELECT * FROM markdown_documents ORDER BY importedAt DESC")
    fun getAllDocuments(): Flow<List<MarkdownDocument>>

    @Query("SELECT * FROM markdown_documents WHERE isSample = 1 ORDER BY id ASC")
    fun getSampleDocuments(): Flow<List<MarkdownDocument>>

    @Query("SELECT * FROM markdown_documents WHERE isSample = 0 ORDER BY importedAt DESC")
    fun getUserDocuments(): Flow<List<MarkdownDocument>>

    @Query("SELECT * FROM markdown_documents WHERE title = :title AND content = :content LIMIT 1")
    suspend fun findDocumentByTitleAndContent(title: String, content: String): MarkdownDocument?

    @Query("SELECT * FROM markdown_documents WHERE content = :content LIMIT 1")
    suspend fun findDocumentByContent(content: String): MarkdownDocument?

    @Query("SELECT * FROM markdown_documents WHERE id = :id")
    fun getDocumentById(id: Int): Flow<MarkdownDocument?>

    @Query("SELECT COUNT(*) FROM markdown_documents WHERE isSample = 1")
    suspend fun getSamplesCount(): Int

    @Query("DELETE FROM markdown_documents WHERE isSample = 1")
    suspend fun deleteSampleDocuments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: MarkdownDocument): Long

    @Query("UPDATE markdown_documents SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarked(id: Int, isBookmarked: Boolean)

    @Query("UPDATE markdown_documents SET lastViewed = :lastViewed WHERE id = :id")
    suspend fun updateLastViewed(id: Int, lastViewed: Long)

    @Delete
    suspend fun deleteDocument(document: MarkdownDocument)

    @Query("DELETE FROM markdown_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)
}
