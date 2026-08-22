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
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
        }
        val (year, month, day) = gregorianToJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        val date = "$year/$month/$day"
        if (!includeTime) return date

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val time = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        return if (includeSeconds) {
            "$date $time:${second.toString().padStart(2, '0')}"
        } else {
            "$date $time"
        }
    }

    fun formatMonthName(
        timestampMillis: Long,
        locale: Locale = Locale.getDefault()
    ): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
        }
        val (year, month, day) = gregorianToJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        val monthName = if (locale.language == "fa") {
            persianMonths[month - 1]
        } else {
            latinMonthAbbreviations[month - 1]
        }
        val value = "$day $monthName $year"
        return if (locale.language == "fa") toPersianDigits(value) else value
    }

    fun toPersianDigits(value: String): String {
        if (persianMonths.none { value.contains(it) }) return value
        return value.map {
            when (it) {
                '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
                '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
                else -> it
            }
        }.joinToString("")
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
