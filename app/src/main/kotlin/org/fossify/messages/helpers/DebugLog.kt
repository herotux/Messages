package org.fossify.messages.helpers

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small diagnostic logger used while investigating conversation loading, search and bank
 * detection. The log is kept privately and, on Android 10+, automatically mirrored to
 * Download/messages_debug.log so it can be collected without adb or a special UI screen.
 */
object DebugLog {
    private const val TAG = "MessagesDebugLog"
    private const val FILE_NAME = "messages_debug.log"
    private const val DOWNLOADS_SUBPATH = "Download/"
    private const val AUTO_EXPORT_MIN_INTERVAL_MS = 1_000L
    private val lock = Any()
    private var lastAutoExportAt = 0L
    private var exportInProgress = false

    fun write(context: Context, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        try {
            synchronized(lock) {
                context.openFileOutput(FILE_NAME, Context.MODE_APPEND).bufferedWriter().use {
                    it.append(timestamp).append(" | ").append(message).append('\n')
                }
            }

            // Export independently of which subsystem produced the message. Previously this
            // was tied to ADAPTER_BIND, so a session that never reached a bind produced no
            // visible file in Downloads.
            scheduleAutoExport(context.applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "write failed", e)
        }
    }

    private fun scheduleAutoExport(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val now = System.currentTimeMillis()
        synchronized(lock) {
            if (exportInProgress || now - lastAutoExportAt < AUTO_EXPORT_MIN_INTERVAL_MS) return
            lastAutoExportAt = now
            exportInProgress = true
        }

        Thread {
            try {
                val success = exportToDownloads(context)
                if (!success) Log.w(TAG, "automatic export did not complete")
            } finally {
                synchronized(lock) {
                    exportInProgress = false
                }
            }
        }.start()
    }

    fun clear(context: Context) {
        try {
            synchronized(lock) {
                context.deleteFile(FILE_NAME)
                deleteExportedFile(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "clear failed", e)
        }
    }

    fun read(context: Context): String = try {
        context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        Log.e(TAG, "read failed", e)
        ""
    }

    /**
     * Copies the private diagnostic file to the public Downloads collection on Android 10+.
     * The same file is updated on every automatic export, so the user always has one stable
     * file: Download/messages_debug.log.
     */
    fun exportToDownloads(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

        return try {
            synchronized(lock) {
                val file = context.getFileStreamPath(FILE_NAME)
                if (!file.exists() || file.length() == 0L) return false

                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Downloads._ID)
                val selection =
                    "${MediaStore.Downloads.DISPLAY_NAME} = ? AND " +
                        "${MediaStore.Downloads.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(FILE_NAME, DOWNLOADS_SUBPATH)

                var uri = resolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                        ContentUris.withAppendedId(collection, id)
                    } else {
                        null
                    }
                }

                if (uri == null) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(MediaStore.Downloads.RELATIVE_PATH, DOWNLOADS_SUBPATH)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    uri = resolver.insert(collection, values) ?: return false
                }

                resolver.openOutputStream(uri, "wt")?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: return false

                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                    null,
                    null
                )
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "exportToDownloads failed", e)
            false
        }
    }

    private fun deleteExportedFile(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val selection =
                "${MediaStore.Downloads.DISPLAY_NAME} = ? AND " +
                    "${MediaStore.Downloads.RELATIVE_PATH} = ?"
            resolver.query(
                collection,
                arrayOf(MediaStore.Downloads._ID),
                selection,
                arrayOf(FILE_NAME, DOWNLOADS_SUBPATH),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    resolver.delete(ContentUris.withAppendedId(collection, id), null, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteExportedFile failed", e)
        }
    }
}
