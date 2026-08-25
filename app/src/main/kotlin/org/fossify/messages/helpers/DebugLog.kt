package org.fossify.messages.helpers

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val FILE_NAME = "messages_debug.log"
    private val lock = Any()

    fun write(context: Context, message: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            synchronized(lock) {
                context.openFileOutput(FILE_NAME, Context.MODE_APPEND).bufferedWriter().use {
                    it.append(timestamp).append(" | ").append(message).append('\n')
                }
            }
        } catch (_: Exception) {
        }
    }

    fun clear(context: Context) {
        try {
            context.deleteFile(FILE_NAME)
        } catch (_: Exception) {
        }
    }

    fun read(context: Context): String = try {
        context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        ""
    }
}
