package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database holder for PDF Z Scanner.
 * Stores [PdfDocumentEntity] metadata, file paths, page counts, and thumbnail paths.
 */
@Database(
    entities = [PdfDocumentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PdfDatabase : RoomDatabase() {

    abstract fun pdfDao(): PdfDao

    companion object {
        private const val DATABASE_NAME = "pdf_z_scanner.db"

        @Volatile
        private var INSTANCE: PdfDatabase? = null

        fun getInstance(context: Context): PdfDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PdfDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
