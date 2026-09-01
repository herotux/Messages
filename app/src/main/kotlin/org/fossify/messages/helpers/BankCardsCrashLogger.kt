package org.fossify.messages.helpers

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Captures uncaught crashes whose stack trace belongs to the standalone bank-card feature. */
object BankCardsCrashLogger {
    private const val LOG_DIR = "BankCardsLogs"
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
        repeat(6) {
            if (current == null) return false
            val stack = current!!.stackTrace.joinToString("\n")
            if (stack.contains("BankCardsActivity") ||
                stack.contains("BankCardsRepository") ||
                stack.contains("BankCardScannerActivity")) return true
            current = current!!.cause
        }
        return false
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(base, LOG_DIR).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS", Locale.US).format(Date())
        val file = File(dir, "bank_cards_crash_$stamp.txt")
        val safeMessage = throwable.message?.replace(Regex("\\b\\d{16}\\b"), "[CARD_NUMBER_REDACTED]")
            ?.replace(Regex("IR\\d{22}"), "[IBAN_REDACTED]")
        file.writeText(buildString {
            appendLine("=== Messages Bank Cards Crash ===")
            appendLine("Date: $stamp")
            appendLine("Thread: ${thread.name}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("Exception: ${throwable::class.java.name}")
            appendLine("Message: $safeMessage")
            appendLine()
            appendLine("Stack trace:")
            appendLine(redact(throwable.stackTraceToString()))
        })
        dir.listFiles { f -> f.isFile && f.name.startsWith("bank_cards_crash_") && f.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_LOGS)
            ?.forEach { it.delete() }
    }

    private fun redact(text: String): String = text
        .replace(Regex("\\b\\d{16}\\b"), "[CARD_NUMBER_REDACTED]")
        .replace(Regex("IR\\d{22}"), "[IBAN_REDACTED]")
}
