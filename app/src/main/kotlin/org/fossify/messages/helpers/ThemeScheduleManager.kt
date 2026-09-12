package org.fossify.messages.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** Persists and schedules automatic global theme changes for day/night time slots. */
object ThemeScheduleManager {
    private const val PREFS = "theme_schedule"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_DAY_THEME = "day_theme_id"
    private const val KEY_NIGHT_THEME = "night_theme_id"
    private const val KEY_DAY_MINUTES = "day_minutes"
    private const val KEY_NIGHT_MINUTES = "night_minutes"
    private const val REQUEST_CODE = 42031

    const val ACTION_THEME_SCHEDULE = "org.fossify.messages.action.THEME_SCHEDULE"

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)
    fun dayThemeId(context: Context): String = prefs(context).getString(KEY_DAY_THEME, ThemeManager.DEFAULT_ID) ?: ThemeManager.DEFAULT_ID
    fun nightThemeId(context: Context): String = prefs(context).getString(KEY_NIGHT_THEME, ThemeManager.MIDNIGHT_ID) ?: ThemeManager.MIDNIGHT_ID
    fun dayMinutes(context: Context): Int = prefs(context).getInt(KEY_DAY_MINUTES, 7 * 60).coerceIn(0, 1439)
    fun nightMinutes(context: Context): Int = prefs(context).getInt(KEY_NIGHT_MINUTES, 19 * 60).coerceIn(0, 1439)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) { applyCurrent(context); scheduleNext(context) } else cancel(context)
    }

    fun setDayTheme(context: Context, id: String) {
        if (ThemeManager.find(context, id) == null) return
        prefs(context).edit().putString(KEY_DAY_THEME, id).apply()
        if (isEnabled(context)) { applyCurrent(context); scheduleNext(context) }
    }

    fun setNightTheme(context: Context, id: String) {
        if (ThemeManager.find(context, id) == null) return
        prefs(context).edit().putString(KEY_NIGHT_THEME, id).apply()
        if (isEnabled(context)) { applyCurrent(context); scheduleNext(context) }
    }

    fun setDayMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_DAY_MINUTES, minutes.coerceIn(0, 1439)).apply()
        if (isEnabled(context)) scheduleNext(context)
    }

    fun setNightMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_NIGHT_MINUTES, minutes.coerceIn(0, 1439)).apply()
        if (isEnabled(context)) scheduleNext(context)
    }

    fun applyCurrent(context: Context) {
        if (!isEnabled(context)) return
        val themeId = if (isDayPeriod(context)) dayThemeId(context) else nightThemeId(context)
        if (ThemeManager.find(context, themeId) != null) ThemeManager.select(context, themeId)
        context.sendBroadcast(Intent(ACTION_THEME_SCHEDULE).setPackage(context.packageName))
    }

    fun scheduleNext(context: Context) {
        if (!isEnabled(context)) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val now = Calendar.getInstance()
        val candidates = listOf(dayMinutes(context), nightMinutes(context)).map { minutes ->
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minutes / 60)
                set(Calendar.MINUTE, minutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val triggerAt = candidates.minByOrNull { it.timeInMillis }?.timeInMillis ?: return
        val pendingIntent = pendingIntent(context)
        alarmManager.cancel(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancel(context: Context) { context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context)) }

    internal fun isDayPeriodAtMinutes(day: Int, night: Int, now: Int): Boolean {
        val safeDay = day.coerceIn(0, 1439)
        val safeNight = night.coerceIn(0, 1439)
        val safeNow = now.coerceIn(0, 1439)
        if (safeDay == safeNight) return safeNow < safeDay
        return if (safeDay < safeNight) safeNow in safeDay until safeNight else safeNow >= safeDay || safeNow < safeNight
    }

    private fun isDayPeriod(context: Context): Boolean {
        val calendar = Calendar.getInstance()
        val now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return isDayPeriodAtMinutes(dayMinutes(context), nightMinutes(context), now)
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, org.fossify.messages.receivers.ThemeScheduleReceiver::class.java).setAction(ACTION_THEME_SCHEDULE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
