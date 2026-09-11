package org.fossify.messages.activities

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.ThemeManager
import org.fossify.messages.helpers.ThemeScheduleManager
import java.util.Locale

/** UI for configuring automatic day/night theme changes. */
class ThemeScheduleActivity : SimpleActivity() {
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) render()
    }

    private fun english() = config.useEnglish
    private fun t(fa: String, en: String) = if (english()) en else fa

    private fun render() {
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (english()) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(14), dp(20), dp(28))
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = MaterialToolbar(this).apply {
            title = t("زمان‌بندی خودکار تم", "Automatic theme schedule")
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
            elevation = 0f
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        addSection(t("زمان‌بندی", "Schedule"))
        val enabled = ThemeScheduleManager.isEnabled(this)
        addToggle(
            t("فعال‌سازی زمان‌بندی", "Enable automatic scheduling"),
            t("تم روز و شب در ساعت‌های تعیین‌شده خودکار عوض می‌شود", "Automatically switch between day and night themes"),
            enabled
        ) { value ->
            if (value && !canScheduleExactAlarms()) {
                showExactAlarmPermission()
            } else {
                ThemeScheduleManager.setEnabled(this, value)
                render()
            }
        }

        addTimeRow(t("شروع تم روز", "Day theme starts"), ThemeScheduleManager.dayMinutes(this)) {
            chooseTime(true)
        }
        addTimeRow(t("شروع تم شب", "Night theme starts"), ThemeScheduleManager.nightMinutes(this)) {
            chooseTime(false)
        }

        addSection(t("تم‌ها", "Themes"))
        addThemeRow(t("تم روز", "Day theme"), ThemeScheduleManager.dayThemeId(this), true)
        addThemeRow(t("تم شب", "Night theme"), ThemeScheduleManager.nightThemeId(this), false)

        if (enabled) {
            addInfo(t("زمان‌بندی فعال است و با راه‌اندازی مجدد گوشی، تغییر ساعت و تغییر منطقه زمانی دوباره تنظیم می‌شود.", "The schedule is active and is restored after reboot, time changes and timezone changes."))
        }
    }

    private fun chooseTime(day: Boolean) {
        val minutes = if (day) ThemeScheduleManager.dayMinutes(this) else ThemeScheduleManager.nightMinutes(this)
        TimePickerDialog(this, { _, hour, minute ->
            val value = hour * 60 + minute
            if (day) ThemeScheduleManager.setDayMinutes(this, value) else ThemeScheduleManager.setNightMinutes(this, value)
            if (ThemeScheduleManager.isEnabled(this)) ThemeScheduleManager.applyCurrent(this)
            render()
        }, minutes / 60, minutes % 60, true).show()
    }

    private fun addThemeRow(title: String, selectedId: String, day: Boolean) {
        val selected = ThemeManager.find(this, selectedId)
        addRow(title, selected?.let { if (english()) it.nameEn else it.nameFa } ?: t("پیش‌فرض", "Default")) {
            val themes = ThemeManager.allThemes(this)
            val labels = themes.map { if (english()) it.nameEn else it.nameFa }.toTypedArray()
            val index = themes.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, index) { dialog, which ->
                    val id = themes[which].id
                    if (day) ThemeScheduleManager.setDayTheme(this, id) else ThemeScheduleManager.setNightTheme(this, id)
                    if (ThemeScheduleManager.isEnabled(this)) ThemeScheduleManager.applyCurrent(this)
                    dialog.dismiss()
                    render()
                }.show()
        }
    }

    private fun addTimeRow(title: String, minutes: Int, action: () -> Unit) {
        addRow(title, String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60), action)
    }

    private fun addRow(title: String, summary: String, action: () -> Unit) {
        val card = com.google.android.material.card.MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant))
            setOnClickListener { action() }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
        }
        box.addView(label(title, 16f, true))
        box.addView(label(summary, 13f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)))
        card.addView(box)
        content.addView(card, margins(0, 8))
    }

    private fun addToggle(title: String, summary: String, checked: Boolean, changed: (Boolean) -> Unit) {
        val card = com.google.android.material.card.MaterialCardView(this).apply {
            radius = dp(16).toFloat(); cardElevation = 0f
            setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant))
        }
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(8), dp(10), dp(8)) }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(title, 16f, true))
        texts.addView(label(summary, 12f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)))
        line.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(MaterialSwitch(this).apply { isChecked = checked; setOnCheckedChangeListener { _, value -> changed(value) } }, LinearLayout.LayoutParams(dp(64), -2))
        card.addView(line)
        content.addView(card, margins(0, 8))
    }

    private fun addSection(title: String) = content.addView(label(title, 13f, true, color(androidx.appcompat.R.attr.colorPrimary)), margins(4, 10))
    private fun addInfo(text: String) = content.addView(label(text, 12f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)), margins(4, 12))
    private fun label(text: String, size: Float, bold: Boolean, textColor: Int = color(com.google.android.material.R.attr.colorOnSurface)) = TextView(this).apply { this.text = text; textSize = size; setTextColor(textColor); if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private fun margins(top: Int, bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(top), 0, dp(bottom)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun color(attr: Int): Int { val value = android.util.TypedValue(); theme.resolveAttribute(attr, value, true); return if (value.resourceId != 0) getColor(value.resourceId) else value.data }

    private fun canScheduleExactAlarms(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    private fun showExactAlarmPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle(t("مجوز زمان‌بندی دقیق", "Exact alarm permission"))
            .setMessage(t("برای تغییر خودکار تم در ساعت دقیق، دسترسی زمان‌بندی دقیق لازم است.", "Exact alarm access is required to switch themes at the configured time."))
            .setNegativeButton(t("لغو", "Cancel"), null)
            .setPositiveButton(t("باز کردن تنظیمات", "Open settings")) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(android.net.Uri.parse("package:$packageName")))
                }
            }.show()
    }
}
