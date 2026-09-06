package org.fossify.commons.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.appbar.MaterialToolbar
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.checkAppIconColor
import org.fossify.commons.extensions.getThemeId
import org.fossify.commons.helpers.APP_ICON_IDS
import org.fossify.commons.helpers.APP_LAUNCHER_NAME
import org.fossify.messages.R
import org.fossify.messages.helpers.AppThemeManager

class CustomizationActivity : BaseSimpleActivity() {
    private lateinit var list: LinearLayout

    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        } catch (_: Exception) { }
        AppThemeManager.setBackgroundImageUri(this, uri)
        refresh()
    }

    override fun getAppIconIDs() = intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: arrayListOf()
    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""
    override fun getRepositoryName() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        baseConfig.isSystemThemeEnabled = false
        setTheme(getThemeId(baseConfig.primaryColor))
        super.onCreate(savedInstanceState)
        useDynamicTheme = false
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = MaterialToolbar(this).apply {
            title = getString(R.string.customize_colors)
            navigationIcon = resources.getDrawable(R.drawable.ic_arrow_left_vector, theme)
            setNavigationOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(-1, dp(56))
        }
        root.addView(toolbar)
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }
        scroll.addView(list)
        root.addView(scroll)
        setContentView(root)
    }

    private fun refresh() {
        baseConfig.isSystemThemeEnabled = false
        setTheme(getThemeId(baseConfig.primaryColor))
        AppThemeManager.apply(this)
        list.removeAllViews()
        section("تم و رنگ‌ها")
        action("تم آماده", "انتخاب ترکیب کامل") { presets() }
        action("رنگ متن", hex(baseConfig.textColor)) { color("رنگ متن", baseConfig.textColor) }
        action("رنگ پس‌زمینه", hex(baseConfig.backgroundColor)) { color("رنگ پس‌زمینه", baseConfig.backgroundColor) }
        action("رنگ اصلی", hex(baseConfig.primaryColor)) { color("رنگ اصلی", baseConfig.primaryColor) }
        action("رنگ Accent", hex(baseConfig.accentColor)) { color("رنگ Accent", baseConfig.accentColor) }
        section("پس‌زمینه")
        action("تصویر پس‌زمینه", if (AppThemeManager.getBackgroundImageUri(this) == null) "استفاده از رنگ" else "تصویر انتخاب شده") { background() }
        section("آیکن برنامه")
        action("رنگ آیکن برنامه", hex(baseConfig.appIconColor)) { iconColors() }
        section("حالت تم")
        info("تم انتخاب‌شده منبع اصلی رنگ‌هاست و Dark/Light سیستم روی آن غلبه نمی‌کند.")
    }

    private fun section(text: String) = list.addView(TextView(this).apply {
        this.text = text; textSize = 14f; setTypeface(typeface, 1); setPadding(dp(4), dp(20), dp(4), dp(8))
    })

    private fun action(title: String, value: String, click: () -> Unit) {
        list.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); isClickable = true
            setBackgroundResource(android.R.drawable.list_selector_background); setOnClickListener { click() }
            val a = TextView(context).apply { text = title; textSize = 16f; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            val b = TextView(context).apply { text = value; textSize = 14f }
            addView(a); addView(b)
        })
    }

    private fun info(text: String) = list.addView(TextView(this).apply { this.text = text; alpha = .7f; setPadding(dp(4), dp(4), dp(4), dp(12)) })

    private fun presets() {
        val names = arrayOf("روشن", "تیره", "آبی", "سبز", "بنفش", "قرمز", "سفید")
        AlertDialog.Builder(this).setTitle("تم آماده").setItems(names) { _, i ->
            when (i) {
                0 -> preset(Color.DKGRAY, Color.WHITE, Color.rgb(67,160,71), Color.rgb(67,160,71))
                1 -> preset(Color.WHITE, Color.rgb(22,22,22), Color.rgb(67,160,71), Color.rgb(105,240,174))
                2 -> preset(Color.DKGRAY, Color.WHITE, Color.rgb(25,118,210), Color.rgb(25,118,210))
                3 -> preset(Color.DKGRAY, Color.WHITE, Color.rgb(46,125,50), Color.rgb(46,125,50))
                4 -> preset(Color.DKGRAY, Color.WHITE, Color.rgb(123,31,162), Color.rgb(123,31,162))
                5 -> preset(Color.WHITE, Color.rgb(55,20,20), Color.rgb(211,47,47), Color.rgb(255,82,82))
                6 -> preset(Color.DKGRAY, Color.WHITE, Color.WHITE, Color.DKGRAY)
            }
        }.show()
    }

    private fun preset(text: Int, bg: Int, primary: Int, accent: Int) {
        baseConfig.textColor = text; baseConfig.backgroundColor = bg; baseConfig.primaryColor = primary; baseConfig.accentColor = accent
        baseConfig.isSystemThemeEnabled = false; refresh()
    }

    private fun color(title: String, current: Int) {
        val input = EditText(this).apply { setSingleLine(); setText(hex(current)); selectAll() }
        AlertDialog.Builder(this).setTitle(title).setView(input).setNegativeButton("انصراف", null).setPositiveButton("اعمال") { _, _ ->
            try {
                val c = Color.parseColor(input.text.toString().trim())
                when (title) {
                    "رنگ متن" -> baseConfig.textColor = c
                    "رنگ پس‌زمینه" -> baseConfig.backgroundColor = c
                    "رنگ اصلی" -> baseConfig.primaryColor = c
                    "رنگ Accent" -> baseConfig.accentColor = c
                }
                baseConfig.isSystemThemeEnabled = false; refresh()
            } catch (_: Exception) { }
        }.show()
    }

    private fun background() {
        val has = AppThemeManager.getBackgroundImageUri(this) != null
        val items = if (has) arrayOf("انتخاب تصویر", "حذف تصویر") else arrayOf("انتخاب تصویر")
        AlertDialog.Builder(this).setTitle("پس‌زمینه").setItems(items) { _, i ->
            if (i == 0) imagePicker.launch(arrayOf("image/*")) else { AppThemeManager.clearBackgroundImage(this); refresh() }
        }.show()
    }

    private fun iconColors() {
        val t = resources.obtainTypedArray(R.array.md_app_icon_colors)
        val colors = IntArray(t.length()) { t.getColor(it, baseConfig.appIconColor) }
        t.recycle()
        val labels = Array(colors.size) { hex(colors[it]) }
        AlertDialog.Builder(this).setTitle("رنگ آیکن برنامه").setItems(labels) { _, i ->
            baseConfig.appIconColor = colors[i]
            try { checkAppIconColor() } catch (_: Exception) { }
            refresh()
        }.show()
    }

    private fun hex(c: Int) = String.format("#%06X", c and 0xFFFFFF)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
