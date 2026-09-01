package org.fossify.messages.helpers

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Captures uncaught crashes whose stack trace belongs to the standalone bank-card feature. */
object BankCardsCrashLogger {
    private const val LOG_DIR = "Messages/BankCardsLogs"
    private const val MAX_LOGS = 10

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isBankCardsCrash(throwable)) {
                runCatching { write(context.applicationContext, thread, throwable) }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun isBankCardsCrash(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        repeat(8) {
            val stack = current?.stackTrace?.joinToString("\n") ?: return false
            if (stack.contains("BankCardsActivity") ||
                stack.contains("BankCardsRepository") ||
                stack.contains("BankCardScannerActivity")) return true
            current = current?.cause
        }
        return false
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS", Locale.US).format(Date())
        val name = "bank_cards_crash_$stamp.txt"
        val text = buildString {
            appendLine("=== Messages Bank Cards Crash ===")
            appendLine("Date: $stamp")
            appendLine("Thread: ${thread.name}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("Exception: ${throwable::class.java.name}")
            appendLine("Message: ${redact(throwable.message.orEmpty())}")
            appendLine()
            appendLine("Stack trace:")
            appendLine(redact(throwable.stackTraceToString()))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/$LOG_DIR")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            try {
                resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            return
        }

        val dir = File(context.getExternalStorageDirectoryCompat(), LOG_DIR).apply { mkdirs() }
        File(dir, name).writeText(text)
        dir.listFiles { f -> f.isFile && f.name.startsWith("bank_cards_crash_") && f.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_LOGS)
            ?.forEach { it.delete() }
    }

    private fun redact(text: String): String = text
        .replace(Regex("\\b\\d{16}\\b"), "[CARD_NUMBER_REDACTED]")
        .replace(Regex("IR\\d{22}"), "[IBAN_REDACTED]")

    private fun Context.getExternalStorageDirectoryCompat(): File =
        File(android.os.Environment.getExternalStorageDirectory(), "")
}
