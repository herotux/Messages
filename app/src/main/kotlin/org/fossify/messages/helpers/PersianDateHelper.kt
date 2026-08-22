package org.fossify.messages.helpers

import android.icu.util.PersianCalendar
import java.util.Calendar
import java.util.Locale

object PersianDateHelper {
    private val persianMonths = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun format(timestampMillis: Long, includeTime: Boolean = true): String {
        val calendar = PersianCalendar(Locale("fa", "IR")).apply {
            timeInMillis = timestampMillis
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val date = "$year/$month/$day"
        return if (includeTime) "$date ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}" else date
    }

    fun formatMonthName(timestampMillis: Long): String {
        val calendar = PersianCalendar(Locale("fa", "IR")).apply {
            timeInMillis = timestampMillis
        }
        return "${calendar.get(Calendar.DAY_OF_MONTH)} ${persianMonths[calendar.get(Calendar.MONTH)]} ${calendar.get(Calendar.YEAR)}"
    }

    fun toPersianDigits(value: String): String {
        return value.map {
            when (it) {
                '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
                '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
                else -> it
            }
        }.joinToString("")
    }
}
