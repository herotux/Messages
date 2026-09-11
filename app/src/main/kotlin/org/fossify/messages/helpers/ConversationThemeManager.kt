package org.fossify.messages.helpers

import android.content.Context

/** Stores an optional theme override for each SMS/MMS conversation thread. */
object ConversationThemeManager {
    private const val PREFS = "conversation_themes"
    private const val DEFAULT_ID = ""

    fun getThemeId(context: Context, threadId: Long): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(threadId.toString(), DEFAULT_ID)
            ?.takeIf { it.isNotBlank() }

    fun getTheme(context: Context, threadId: Long): ThemeManager.ThemeDefinition? =
        getThemeId(context, threadId)?.let { ThemeManager.find(context, it) }

    fun setTheme(context: Context, threadId: Long, themeId: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (themeId.isNullOrBlank()) remove(threadId.toString())
            else putString(threadId.toString(), themeId)
        }.apply()
    }

    fun clearTheme(context: Context, threadId: Long) = setTheme(context, threadId, null)
}
