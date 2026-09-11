package org.fossify.messages.helpers

import android.content.Context

/** Stores an optional theme override for each SMS/MMS conversation thread. */
object ConversationThemeManager {
    private const val PREFS = "conversation_themes"
    private const val DEFAULT_ID = ""

    fun getThemeId(context: Context, threadId: Long): String? {
        if (threadId <= 0L) return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(threadId.toString(), DEFAULT_ID)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun getTheme(context: Context, threadId: Long): ThemeManager.ThemeDefinition? =
        getThemeId(context, threadId)?.let { ThemeManager.find(context, it) }

    fun setTheme(context: Context, threadId: Long, themeId: String?) {
        if (threadId <= 0L) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (themeId.isNullOrBlank() || ThemeManager.find(context, themeId) == null) {
                remove(threadId.toString())
            } else {
                putString(threadId.toString(), themeId.trim())
            }
        }.apply()
    }

    fun clearTheme(context: Context, threadId: Long) = setTheme(context, threadId, null)
}
