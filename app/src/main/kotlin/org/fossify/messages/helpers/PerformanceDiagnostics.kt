package org.fossify.messages.helpers

import android.content.Context
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Temporary, self-contained startup profiler used to diagnose slow conversation loading.
 * It writes to DebugLog and automatically exports messages_debug.log to Downloads.
 */
object PerformanceDiagnostics {
    private const val SAMPLE_INTERVAL_MS = 500L
    private const val MAX_DURATION_MS = 60_000L
    private val running = AtomicBoolean(false)

    fun start(context: Context) {
        if (!running.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        DebugLog.clear(appContext)
        val startedAt = System.nanoTime()
        DebugLog.write(appContext, "PERF_DIAGNOSTICS_START pid=${android.os.Process.myPid()}")

        Thread {
            try {
                val deadline = System.nanoTime() + MAX_DURATION_MS * 1_000_000L
                var lastSignature = ""

                while (System.nanoTime() < deadline) {
                    Thread.sleep(SAMPLE_INTERVAL_MS)
                    val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L
                    val samples = Thread.getAllStackTraces()
                    val interesting = samples.entries
                        .asSequence()
                        .filter { (_, stack) -> stack.any { frame -> isRelevant(frame.className, frame.methodName) } }
                        .map { (thread, stack) ->
                            val relevant = stack.firstOrNull { frame ->
                                isRelevant(frame.className, frame.methodName)
                            }
                            val top = stack.take(6).joinToString(" <- ") {
                                "${it.className}.${it.methodName}:${it.lineNumber}"
                            }
                            "thread=${thread.name} state=${thread.state} relevant=${relevant?.className}.${relevant?.methodName} top=$top"
                        }
                        .toList()

                    if (interesting.isNotEmpty()) {
                        val signature = interesting.joinToString(" | ")
                        if (signature != lastSignature) {
                            lastSignature = signature
                            DebugLog.write(
                                appContext,
                                "PERF_SAMPLE elapsedMs=$elapsedMs ${signature.take(12000)}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                DebugLog.write(
                    appContext,
                    "PERF_DIAGNOSTICS_ERROR type=${e.javaClass.simpleName} message=${e.message}"
                )
            } finally {
                val totalMs = (System.nanoTime() - startedAt) / 1_000_000L
                DebugLog.write(appContext, "PERF_DIAGNOSTICS_FINISHED elapsedMs=$totalMs")
                DebugLog.exportToDownloads(appContext)
                running.set(false)
            }
        }.apply {
            name = "Messages-PerformanceDiagnostics"
            isDaemon = true
            start()
        }
    }

    private fun isRelevant(className: String, methodName: String): Boolean {
        val value = "$className.$methodName".lowercase(Locale.US)
        return value.contains("getconversations") ||
            value.contains("getmycontactscursor") ||
            value.contains("getthreadcontactnames") ||
            value.contains("getphotourifromphonenumber") ||
            value.contains("getunreadcountsbythread") ||
            value.contains("insertorupdateconversation") ||
            value.contains("getnewconversations") ||
            value.contains("getcachedconversations") ||
            value.contains("setupconversations") ||
            value.contains("updateconversations") ||
            value.contains("getmessageswithtext")
    }
}
