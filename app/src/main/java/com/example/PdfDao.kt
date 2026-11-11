package com.example

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for querying and mutating [PdfDocumentEntity] records.
 * Supports reactive streams for All Recent files, Downloads, Opened documents, and search.
 */
@Dao
interface PdfDao {

    /**
     * Streams all PDF documents ordered by most recently opened/created first.
     */
    @Query("SELECT * FROM pdf_documents ORDER BY lastOpenedAt DESC")
    fun getAllRecentDocuments(): Flow<List<PdfDocumentEntity>>

    /**
     * Streams only PDF documents marked as downloaded/imported (`isDownloaded = 1`),
     * ordered by most recent first.
     */
    @Query("SELECT * FROM pdf_documents WHERE isDownloaded = 1 ORDER BY lastOpenedAt DESC")
    fun getDownloadedDocuments(): Flow<List<PdfDocumentEntity>>

    /**
     * Streams only PDF documents that have been opened in the viewer/editor (`isOpened = 1`),
     * ordered by most recently opened first.
     */
    @Query("SELECT * FROM pdf_documents WHERE isOpened = 1 ORDER BY lastOpenedAt DESC")
    fun getOpenedDocuments(): Flow<List<PdfDocumentEntity>>

    /**
     * Searches PDF documents by name (case-insensitive) ordered by most recent first.
     */
    @Query("SELECT * FROM pdf_documents WHERE name LIKE '%' || :query || '%' ORDER BY lastOpenedAt DESC")
    fun getDocumentsBySearchQuery(query: String): Flow<List<PdfDocumentEntity>>

    /**
     * Retrieves a single [PdfDocumentEntity] by its primary key [id], or `null` if not found.
     */
    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): PdfDocumentEntity?

    /**
     * Retrieves a single [PdfDocumentEntity] by its absolute [filePath], or `null` if not found.
     */
    @Query("SELECT * FROM pdf_documents WHERE filePath = :filePath LIMIT 1")
    suspend fun getDocumentByFilePath(filePath: String): PdfDocumentEntity?

    /**
     * Returns the total count of PDF documents stored in the database.
     */
    @Query("SELECT COUNT(*) FROM pdf_documents")
    suspend fun getDocumentCount(): Int

    /**
     * Inserts or replaces a [PdfDocumentEntity] and returns its row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: PdfDocumentEntity): Long

    /**
     * Updates an existing [PdfDocumentEntity] record.
     */
    @Update
    suspend fun updateDocument(document: PdfDocumentEntity)

    /**
     * Marks a document as opened and updates its [lastOpenedAt] timestamp.
     */
    @Query("UPDATE pdf_documents SET isOpened = 1, lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun markDocumentAsOpened(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Updates the generated cover thumbnail path and page count for a document.
     */
    @Query("UPDATE pdf_documents SET thumbnailPath = :thumbnailPath, pageCount = :pageCount WHERE id = :id")
    suspend fun updateThumbnailAndPageCount(id: Long, thumbnailPath: String?, pageCount: Int)

    /**
     * Renames a document in the database.
     */
    @Query("UPDATE pdf_documents SET name = :newName WHERE id = :id")
    suspend fun renameDocument(id: Long, newName: String)

    /**
     * Deletes a [PdfDocumentEntity] record.
     */
    @Delete
    suspend fun deleteDocument(document: PdfDocumentEntity)

    /**
     * Deletes a document by its primary key [id].
     */
    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
}
