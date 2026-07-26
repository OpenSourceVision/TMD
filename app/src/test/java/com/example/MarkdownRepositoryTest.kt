package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.MarkdownDatabase
import com.example.data.MarkdownDocument
import com.example.data.MarkdownRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MarkdownRepositoryTest {

    private lateinit var db: MarkdownDatabase
    private lateinit var repository: MarkdownRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MarkdownDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MarkdownRepository(db.markdownDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveDocument() = runBlocking {
        val doc = MarkdownDocument(
            title = "Test Doc",
            content = "# Hello World",
            isSample = false
        )
        val insertedId = repository.insert(doc)

        val retrieved = repository.getDocumentById(insertedId.toInt()).first()
        assertNotNull(retrieved)
        assertEquals("Test Doc", retrieved?.title)
        assertEquals("# Hello World", retrieved?.content)
    }

    @Test
    fun testFindDuplicateDocument() = runBlocking {
        val doc = MarkdownDocument(
            title = "Sample Title",
            content = "Unique content string",
            isSample = false
        )
        repository.insert(doc)

        val duplicateByTitleAndContent = repository.findDuplicate("Sample Title", "Unique content string")
        assertNotNull("Should find duplicate by exact title and content", duplicateByTitleAndContent)

        val duplicateByContentOnly = repository.findDuplicate("Different Title", "Unique content string")
        assertNotNull("Should find duplicate by content match", duplicateByContentOnly)

        val nonDuplicate = repository.findDuplicate("New Title", "New Content")
        assertNull("Should return null for non-duplicate", nonDuplicate)
    }

    @Test
    fun testDeleteDocument() = runBlocking {
        val doc = MarkdownDocument(
            title = "To Delete",
            content = "Delete me",
            isSample = false
        )
        val id = repository.insert(doc).toInt()

        val userDocsBefore = repository.userDocuments.first()
        assertEquals(1, userDocsBefore.size)

        val insertedDoc = repository.getDocumentById(id).first()
        assertNotNull(insertedDoc)
        repository.delete(insertedDoc!!)

        val userDocsAfter = repository.userDocuments.first()
        assertEquals(0, userDocsAfter.size)
    }
}
