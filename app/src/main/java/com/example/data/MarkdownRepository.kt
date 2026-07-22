package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class MarkdownRepository(private val markdownDao: MarkdownDao) {
    val allDocuments: Flow<List<MarkdownDocument>> = markdownDao.getAllDocuments()
    val sampleDocuments: Flow<List<MarkdownDocument>> = markdownDao.getSampleDocuments()
    val userDocuments: Flow<List<MarkdownDocument>> = markdownDao.getUserDocuments()

    fun getDocumentById(id: Int): Flow<MarkdownDocument?> {
        return markdownDao.getDocumentById(id)
    }

    suspend fun findDuplicate(title: String, content: String): MarkdownDocument? {
        return markdownDao.findDocumentByTitleAndContent(title, content)
            ?: markdownDao.findDocumentByContent(content)
    }

    suspend fun insert(document: MarkdownDocument): Long {
        return markdownDao.insertDocument(document)
    }

    suspend fun updateBookmarked(id: Int, isBookmarked: Boolean) {
        markdownDao.updateBookmarked(id, isBookmarked)
    }

    suspend fun updateLastViewed(id: Int, lastViewed: Long) {
        markdownDao.updateLastViewed(id, lastViewed)
    }

    suspend fun delete(document: MarkdownDocument) {
        markdownDao.deleteDocument(document)
    }

    suspend fun deleteById(id: Int) {
        markdownDao.deleteDocumentById(id)
    }

    suspend fun clearSampleDocuments() {
        markdownDao.deleteSampleDocuments()
    }
}
