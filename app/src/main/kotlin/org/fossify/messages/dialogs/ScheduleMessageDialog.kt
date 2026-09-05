package org.fossify.messages.dialogs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.text.format.DateFormat
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getDatePickerDialogTheme
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.getTimeFormat
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.messages.R
import org.fossify.messages.databinding.ScheduleMessageDialogBinding
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.roundToClosestMultipleOf
import org.fossify.messages.helpers.PersianDateHelper
import org.joda.time.DateTime
import java.util.Calendar
import java.util.Locale

class ScheduleMessageDialog(
    private val activity: BaseSimpleActivity,
    private var dateTime: DateTime? = null,
    private val callback: (dateTime: DateTime?) -> Unit
) {
    private val binding = ScheduleMessageDialogBinding.inflate(activity.layoutInflater)
    private val textColor = activity.getProperTextColor()
    private var previewDialog: AlertDialog? = null
    private var previewShown = false
    private var isNewMessage = dateTime == null
    private val calendar = Calendar.getInstance()

    init {
        arrayOf(binding.subtitle, binding.editTime, binding.editDate).forEach { it.setTextColor(textColor) }
        arrayOf(binding.dateImage, binding.timeImage).forEach { it.applyColorFilter(textColor) }
        binding.editDate.setOnClickListener { showDatePicker() }
        binding.editTime.setOnClickListener { showTimePicker() }
        val targetDateTime = dateTime ?: DateTime.now().plusHours(1)
        updateTexts(targetDateTime)
        if (isNewMessage) showDatePicker() else showPreview()
    }

    private fun updateTexts(value: DateTime) {
        if (activity.config.usePersianCalendar) {
            val locale = Locale.getDefault()
            val date = PersianDateHelper.format(value.millis, includeTime = false)
            binding.editDate.text = date.split("/").let { parts -> if (parts.size == 3) PersianDateHelper.toPersianDigits("${parts[0]}/${parts[1]}/${parts[2]}") else date }
        } else {
            binding.editDate.text = value.toString(activity.config.dateFormat)
        }
        binding.editTime.text = value.toString(activity.getTimeFormat())
    }

    private fun showPreview() {
        if (previewShown) return
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.messages.R.string.ok, null)
            .setNegativeButton(org.fossify.messages.R.string.cancel, null)
            .apply {
                previewShown = true
                activity.setupDialogStuff(binding.root, this, R.string.schedule_message) { dialog ->
                    previewDialog = dialog
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (validateDateTime()) { callback(dateTime); dialog.dismiss() }
                    }
                    dialog.setOnDismissListener { previewShown = false; previewDialog = null }
                }
            }
    }

    private fun showDatePicker() {
        if (activity.config.usePersianCalendar) {
            showJalaliDatePicker()
            return
        }
        val year = dateTime?.year ?: calendar.get(Calendar.YEAR)
        val monthOfYear = dateTime?.monthOfYear?.minus(1) ?: calendar.get(Calendar.MONTH)
        val dayOfMonth = dateTime?.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, y, m, d -> dateSet(y, m, d) }
        TimeAwareDatePickerDialog(activity, dateSetListener, year, monthOfYear, dayOfMonth).show()
    }

    private fun showJalaliDatePicker() {
        val currentJalali = PersianDateHelper.toJalaliDate(calendar)
        val selected = dateTime?.let { PersianDateHelper.toJalaliDate(Calendar.getInstance().apply { timeInMillis = it.millis }) } ?: currentJalali
        val yearPicker = NumberPicker(activity).apply { minValue = currentJalali.first; maxValue = currentJalali.first + 10; value = selected.first.coerceIn(minValue, maxValue); wrapSelectorWheel = false }
        val monthPicker = NumberPicker(activity).apply { minValue = 1; maxValue = 12; value = selected.second; wrapSelectorWheel = false }
        val dayPicker = NumberPicker(activity).apply { minValue = 1; maxValue = PersianDateHelper.daysInJalaliMonth(selected.first, selected.second); value = selected.third.coerceAtMost(maxValue); wrapSelectorWheel = false }
        yearPicker.setOnValueChangedListener { _, _, new -> dayPicker.maxValue = PersianDateHelper.daysInJalaliMonth(new, monthPicker.value); if (dayPicker.value > dayPicker.maxValue) dayPicker.value = dayPicker.maxValue }
        monthPicker.setOnValueChangedListener { _, _, new -> dayPicker.maxValue = PersianDateHelper.daysInJalaliMonth(yearPicker.value, new); if (dayPicker.value > dayPicker.maxValue) dayPicker.value = dayPicker.maxValue }
        val container = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(24, 8, 24, 8)
            addView(yearPicker, android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(monthPicker, android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(dayPicker, android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        val dialog = activity.getAlertDialogBuilder().setTitle("انتخاب تاریخ").setView(container).setNegativeButton(org.fossify.messages.R.string.cancel, null).setPositiveButton(org.fossify.messages.R.string.ok, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val gregorian = PersianDateHelper.jalaliToGregorian(yearPicker.value, monthPicker.value, dayPicker.value)
                if (gregorian != null) { dateSet(gregorian.get(Calendar.YEAR), gregorian.get(Calendar.MONTH), gregorian.get(Calendar.DAY_OF_MONTH)); dialog.dismiss() }
                else activity.toast(R.string.must_pick_time_in_the_future)
            }
        }
        dialog.show()
    }

    private fun showTimePicker() {
        val hourOfDay = dateTime?.hourOfDay ?: getNextHour()
        val minute = dateTime?.minuteOfHour ?: getNextMinute()
        if (activity.isDynamicTheme()) {
            val timeFormat = if (DateFormat.is24HourFormat(activity)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
            val timePicker = MaterialTimePicker.Builder().setTimeFormat(timeFormat).setHour(hourOfDay).setMinute(minute).build()
            timePicker.addOnPositiveButtonClickListener { timeSet(timePicker.hour, timePicker.minute) }
            timePicker.show(activity.supportFragmentManager, "")
        } else {
            val timeSetListener = OnTimeSetListener { _, hours, minutes -> timeSet(hours, minutes) }
            TimePickerDialog(activity, activity.getDatePickerDialogTheme(), timeSetListener, hourOfDay, minute, DateFormat.is24HourFormat(activity)).apply {
                show()
                getButton(AlertDialog.BUTTON_NEGATIVE).apply { text = activity.getString(org.fossify.messages.R.string.cancel); setOnClickListener { dismiss() } }
            }
        }
    }

    private fun dateSet(year: Int, monthOfYear: Int, dayOfMonth: Int) {
        if (isNewMessage) showTimePicker()
        dateTime = DateTime.now().withDate(year, monthOfYear + 1, dayOfMonth).run {
            if (dateTime != null) withTime(dateTime!!.hourOfDay, dateTime!!.minuteOfHour, 0, 0) else withTime(getNextHour(), getNextMinute(), 0, 0)
        }
        if (!isNewMessage) validateDateTime()
        isNewMessage = false
        updateTexts(dateTime!!)
    }

    private fun timeSet(hourOfDay: Int, minute: Int) {
        dateTime = dateTime?.withHourOfDay(hourOfDay)?.withMinuteOfHour(minute)
        if (validateDateTime()) { updateTexts(dateTime!!); showPreview() } else showTimePicker()
    }

    private fun validateDateTime(): Boolean {
        return if (dateTime?.isAfterNow == false) { activity.toast(R.string.must_pick_time_in_the_future); false } else true
    }

    private fun getNextHour(): Int = (calendar.get(Calendar.HOUR_OF_DAY) + 1).coerceIn(0, 23)
    private fun getNextMinute(): Int = (calendar.get(Calendar.MINUTE) + 5).roundToClosestMultipleOf(5).coerceIn(0, 59)
}

private class TimeAwareDatePickerDialog(
    activity: BaseSimpleActivity,
    listener: DatePickerDialog.OnDateSetListener,
    year: Int,
    month: Int,
    day: Int
) : DatePickerDialog(activity, activity.getDatePickerDialogTheme(), listener, year, month, day) {
    init { datePicker.minDate = System.currentTimeMillis() }
}
