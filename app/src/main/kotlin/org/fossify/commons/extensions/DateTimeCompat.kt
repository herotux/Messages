package org.fossify.commons.extensions

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDateOrTime(
    timestampMillis: Long,
    context: Context,
    hideTimeOnOtherDays: Boolean = false,
    showCurrentYear: Boolean = true
): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val target = Date(timestampMillis)
    val now = Date()
    val sameDay = SimpleDateFormat("yyyyMMdd", locale).format(target) == SimpleDateFormat("yyyyMMdd", locale).format(now)
    val sameYear = SimpleDateFormat("yyyy", locale).format(target) == SimpleDateFormat("yyyy", locale).format(now)

    if (sameDay) return SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale).format(target)

    val pattern = when {
        hideTimeOnOtherDays && !showCurrentYear && sameYear -> "MMM d"
        hideTimeOnOtherDays && !showCurrentYear -> "MMM d, yyyy"
        hideTimeOnOtherDays -> if (showCurrentYear || sameYear) "MMM d" else "MMM d, yyyy"
        showCurrentYear && sameYear -> "MMM d, HH:mm"
        else -> "MMM d, yyyy, HH:mm"
    }
    return SimpleDateFormat(pattern, locale).format(target)
}
