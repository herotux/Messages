package org.fossify.messages.helpers

import java.util.Calendar
import java.util.Locale

object PersianDateHelper {
    private val persianMonths = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val latinMonthAbbreviations = arrayOf(
        "far", "ord", "kho", "tir", "mor", "sha",
        "meh", "aba", "azr", "dey", "bah", "esf"
    )

    fun format(timestampMillis: Long, includeTime: Boolean = true, includeSeconds: Boolean = false): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val (year, month, day) = toJalali(calendar)
        val date = "$year/$month/$day"
        if (!includeTime) return date
        val time = formatTime(calendar, use24Hour = true)
        return if (includeSeconds) "$date $time:${calendar.get(Calendar.SECOND).toString().padStart(2, '0')}" else "$date $time"
    }

    fun formatConversationList(timestampMillis: Long, dateFormat: String, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val now = Calendar.getInstance()
        val targetJalali = toJalali(target)
        val nowJalali = toJalali(now)
        if (targetJalali == nowJalali) return toDigits(formatTime(target, use24Hour), locale)
        val includeYear = targetJalali.first != nowJalali.first
        return toDigits(formatDateParts(targetJalali.first, targetJalali.second, targetJalali.third, dateFormat, includeYear), locale)
    }

    fun formatThreadDateTime(timestampMillis: Long, dateFormat: String, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val now = Calendar.getInstance()
        val targetJalali = toJalali(target)
        val nowJalali = toJalali(now)
        val time = formatTime(target, use24Hour)
        if (targetJalali == nowJalali) return toDigits(time, locale)
        val date = formatDateParts(targetJalali.first, targetJalali.second, targetJalali.third, dateFormat, targetJalali.first != nowJalali.first)
        return toDigits("$date $time", locale)
    }

    fun formatMonthName(timestampMillis: Long, locale: Locale = Locale.getDefault()): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val (year, month, day) = toJalali(calendar)
        val monthName = if (locale.language == "fa") persianMonths[month - 1] else latinMonthAbbreviations[month - 1]
        return toDigits("$day $monthName $year", locale)
    }

    fun toPersianDigits(value: String): String = value.map {
        when (it) { '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'; '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'; else -> it }
    }.joinToString("")

    private fun toDigits(value: String, locale: Locale): String = if (locale.language == "fa") toPersianDigits(value) else value

    fun toJalaliDate(calendar: Calendar): Triple<Int, Int, Int> = toJalali(calendar)

    fun jalaliToGregorian(year: Int, month: Int, day: Int): Calendar? {
        // Search a small window around the corresponding Gregorian year using the
        // same conversion routine used for display. This avoids a second conversion
        // algorithm and keeps picker/storage conversion consistent.
        val candidate = Calendar.getInstance().apply {
            clear()
            set(year + 621, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(800) {
            if (toJalali(candidate) == Triple(year, month, day)) return candidate.clone() as Calendar
            candidate.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }

    fun daysInJalaliMonth(year: Int, month: Int): Int {
        if (month <= 6) return 31
        if (month <= 11) return 30
        return if (isJalaliLeapYear(year)) 30 else 29
    }

    private fun isJalaliLeapYear(year: Int): Boolean {
        val start = jalaliToGregorian(year, 1, 1) ?: return false
        val next = jalaliToGregorian(year + 1, 1, 1) ?: return false
        return ((next.timeInMillis - start.timeInMillis) / 86_400_000L) > 365
    }

    private fun toJalali(calendar: Calendar): Triple<Int, Int, Int> = gregorianToJalali(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

    private fun formatTime(calendar: Calendar, use24Hour: Boolean): String {
        val hour = if (use24Hour) calendar.get(Calendar.HOUR_OF_DAY) else calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = calendar.get(Calendar.MINUTE)
        val base = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        return if (use24Hour) base else "$base ${if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"}"
    }

    private fun formatDateParts(year: Int, month: Int, day: Int, dateFormat: String, includeYear: Boolean): String {
        val separator = when { dateFormat.contains('.') -> "."; dateFormat.contains('/') -> "/"; else -> "-" }
        val dayPart = day.toString().padStart(2, '0')
        val monthPart = month.toString().padStart(2, '0')
        val yearPart = year.toString()
        if (!includeYear) return if (dateFormat.startsWith("MM")) "$monthPart$separator$dayPart" else "$dayPart$separator$monthPart"
        return when {
            dateFormat.startsWith("yyyy") -> "$yearPart$separator$monthPart$separator$dayPart"
            dateFormat.startsWith("MM") -> "$monthPart$separator$dayPart$separator$yearPart"
            else -> "$dayPart$separator$monthPart$separator$yearPart"
        }
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gregorianMonthDays = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var gYear = gy
        var jYear: Int
        if (gYear > 1600) { jYear = 979; gYear -= 1600 } else { gYear -= 621; jYear = 0 }
        val adjustedYear = if (gm > 2) gYear + 1 else gYear
        var days = 365 * gYear + (adjustedYear + 3) / 4 - (adjustedYear + 99) / 100 + (adjustedYear + 399) / 400 - 80 + gd + gregorianMonthDays[gm - 1]
        jYear += 33 * (days / 12053)
        days %= 12053
        jYear += 4 * (days / 1461)
        days %= 1461
        if (days > 365) { jYear += (days - 1) / 365; days = (days - 1) % 365 }
        val jMonth: Int
        val jDay: Int
        if (days < 186) { jMonth = 1 + days / 31; jDay = 1 + days % 31 }
        else { jMonth = 7 + (days - 186) / 30; jDay = 1 + (days - 186) % 30 }
        return Triple(jYear, jMonth, jDay)
    }
}
