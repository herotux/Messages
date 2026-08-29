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

    fun format(
        timestampMillis: Long,
        includeTime: Boolean = true,
        includeSeconds: Boolean = false
    ): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val (year, month, day) = toJalali(calendar)
        val date = "$year/$month/$day"
        if (!includeTime) return date

        val time = formatTime(calendar, use24Hour = true)
        return if (includeSeconds) {
            "$date $time:${calendar.get(Calendar.SECOND).toString().padStart(2, '0')}"
        } else {
            "$date $time"
        }
    }

    /** Standard conversation-list format: today/current year are compact; old years include year. */
    fun formatConversationList(
        timestampMillis: Long,
        dateFormat: String,
        use24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val now = Calendar.getInstance()
        val (year, month, day) = toJalali(target)
        val (_, currentMonth, currentDay) = toJalali(now)
        val currentYear = toJalali(now).first

        if (target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        ) {
            return toDigits(formatTime(target, use24Hour), locale)
        }

        val includeYear = year != currentYear
        val date = formatDateParts(year, month, day, dateFormat, includeYear)
        return toDigits(date, locale)
    }

    /** Standard conversation-thread separator format. */
    fun formatThreadDateTime(
        timestampMillis: Long,
        dateFormat: String,
        use24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val now = Calendar.getInstance()
        val (year, month, day) = toJalali(target)
        val (currentYear, _, _) = toJalali(now)
        val time = formatTime(target, use24Hour)

        if (target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        ) {
            return toDigits(time, locale)
        }

        val date = formatDateParts(year, month, day, dateFormat, year != currentYear)
        return toDigits("$date $time", locale)
    }

    fun formatMonthName(
        timestampMillis: Long,
        locale: Locale = Locale.getDefault()
    ): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val (year, month, day) = toJalali(calendar)
        val monthName = if (locale.language == "fa") persianMonths[month - 1] else latinMonthAbbreviations[month - 1]
        return toDigits("$day $monthName $year", locale)
    }

    fun toPersianDigits(value: String): String = value.map {
        when (it) {
            '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
            '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
            else -> it
        }
    }.joinToString("")

    private fun toDigits(value: String, locale: Locale): String =
        if (locale.language == "fa") toPersianDigits(value) else value

    private fun toJalali(calendar: Calendar): Triple<Int, Int, Int> = gregorianToJalali(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    private fun formatTime(calendar: Calendar, use24Hour: Boolean): String {
        val hour = if (use24Hour) calendar.get(Calendar.HOUR_OF_DAY) else {
            val value = calendar.get(Calendar.HOUR)
            if (value == 0) 12 else value
        }
        val minute = calendar.get(Calendar.MINUTE)
        val base = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        if (use24Hour) return base
        return "$base ${if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"}"
    }

    private fun formatDateParts(year: Int, month: Int, day: Int, dateFormat: String, includeYear: Boolean): String {
        val separator = when {
            dateFormat.contains('.') -> "."
            dateFormat.contains('/') -> "/"
            else -> "-"
        }
        val dayPart = day.toString().padStart(2, '0')
        val monthPart = month.toString().padStart(2, '0')
        val yearPart = year.toString()

        val date = when {
            dateFormat.startsWith("yyyy") -> listOf(yearPart, monthPart, dayPart)
            dateFormat.startsWith("MM") -> listOf(monthPart, dayPart, yearPart)
            else -> listOf(dayPart, monthPart, yearPart)
        }
        return if (includeYear) date.joinToString(separator) else {
            when {
                dateFormat.startsWith("yyyy") -> listOf(monthPart, dayPart).joinToString(separator)
                dateFormat.startsWith("MM") -> listOf(monthPart, dayPart).joinToString(separator)
                else -> listOf(dayPart, monthPart).joinToString(separator)
            }
        }
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gregorianMonthDays = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var gYear = gy
        var jYear = 0

        if (gYear > 1600) {
            jYear = 979
            gYear -= 1600
        } else {
            gYear -= 621
        }

        val adjustedYear = if (gm > 2) gYear + 1 else gYear
        var days = 365 * gYear +
            (adjustedYear + 3) / 4 -
            (adjustedYear + 99) / 100 +
            (adjustedYear + 399) / 400 -
            80 + gd + gregorianMonthDays[gm - 1]

        jYear += 33 * (days / 12053)
        days %= 12053
        jYear += 4 * (days / 1461)
        days %= 1461

        if (days > 365) {
            jYear += (days - 1) / 365
            days = (days - 1) % 365
        }

        val jMonth: Int
        val jDay: Int
        if (days < 186) {
            jMonth = 1 + days / 31
            jDay = 1 + days % 31
        } else {
            jMonth = 7 + (days - 186) / 30
            jDay = 1 + (days - 186) % 30
        }

        return Triple(jYear, jMonth, jDay)
    }
}
