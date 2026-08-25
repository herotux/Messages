package org.fossify.messages.helpers

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val FILE_NAME = "messages_debug.log"
    private const val DOWNLOADS_SUBPATH = "Download/"
    private val lock = Any()

    fun write(context: Context, message: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            synchronized(lock) {
                context.openFileOutput(FILE_NAME, Context.MODE_APPEND).bufferedWriter().use {
                    it.append(timestamp).append(" | ").append(message).append('\n')
                }
                exportToDownloads(context)
            }
        } catch (_: Exception) {
        }
    }

    fun clear(context: Context) {
        try {
            synchronized(lock) {
                context.deleteFile(FILE_NAME)
                deleteExportedFile(context)
            }
        } catch (_: Exception) {
        }
    }

    fun read(context: Context): String = try {
        context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        ""
    }

    /**
     * Copies the current private debug log to the public Downloads collection.
     * No storage permission is required on Android 10+.
     */
    fun exportToDownloads(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        try {
            val file = context.getFileStreamPath(FILE_NAME)
            if (!file.exists()) return

            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(FILE_NAME, DOWNLOADS_SUBPATH)

            var uri = resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    ContentUris.withAppendedId(collection, id)
                } else null
            }

            if (uri == null) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, DOWNLOADS_SUBPATH)
                }
                uri = resolver.insert(collection, values) ?: return
            }

            resolver.openOutputStream(uri, "wt")?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }
        } catch (_: Exception) {
        }
    }

    private fun deleteExportedFile(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
            resolver.query(collection, arrayOf(MediaStore.Downloads._ID), selection, arrayOf(FILE_NAME, DOWNLOADS_SUBPATH), null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    resolver.delete(ContentUris.withAppendedId(collection, id), null, null)
                }
            }
        } catch (_: Exception) {
        }
    }
}
