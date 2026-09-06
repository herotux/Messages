#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
settings = ROOT / "app/src/main/kotlin/org/fossify/messages/activities/SettingsActivity.kt"
manifest = ROOT / "app/src/main/AndroidManifest.xml"

s = settings.read_text()
if "PersianFontCatalog" not in s:
    s = s.replace(
        "import org.fossify.messages.helpers.LOCK_SCREEN_SENDER_MESSAGE\n",
        "import org.fossify.messages.helpers.LOCK_SCREEN_SENDER_MESSAGE\nimport org.fossify.messages.helpers.PersianFontCatalog\n"
    )

new_choose_font = '''    private fun chooseFont() {
        val builtIns = PersianFontCatalog.fonts
        val labels = buildList {
            add(t("فونت پیش‌فرض سیستم", "System default font"))
            addAll(builtIns.map { it.title })
            add(t("فونت سفارشی…", "Custom font…"))
        }.toTypedArray()
        val selected = when {
            config.fontType != FONT_TYPE_CUSTOM -> 0
            builtIns.indexOfFirst { it.fileName == config.fontName } >= 0 -> 1 + builtIns.indexOfFirst { it.fileName == config.fontName }
            else -> labels.lastIndex
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(t("فونت برنامه", "App font"))
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                if (which == 0) {
                    config.fontType = FONT_TYPE_SYSTEM_DEFAULT
                    config.fontName = ""
                    FontHelper.clearCache()
                    dialog.dismiss()
                    recreate()
                    return@setSingleChoiceItems
                }

                if (which <= builtIns.size) {
                    val spec = builtIns[which - 1]
                    dialog.dismiss()
                    Thread {
                        val result = PersianFontCatalog.install(this, spec)
                        runOnUiThread {
                            if (result.isSuccess) {
                                config.fontType = FONT_TYPE_CUSTOM
                                config.fontName = spec.fileName
                                FontHelper.clearCache()
                                recreate()
                            } else {
                                MaterialAlertDialogBuilder(this)
                                    .setTitle(t("خطا در دریافت فونت", "Font download failed"))
                                    .setMessage(t("این فونت فعلاً قابل دریافت نیست. بعداً دوباره تلاش کنید.", "This font could not be downloaded right now. Please try again later."))
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                            }
                        }
                    }.start()
                    return@setSingleChoiceItems
                }

                dialog.dismiss()
                pickCustomFont.launch(arrayOf("font/*", "application/octet-stream"))
            }
            .show()
    }
'''

pattern = re.compile(r"    private fun chooseFont\(\) \{.*?\n    \}\n\n    private fun resolveFontFileName", re.S)
s, count = pattern.subn(new_choose_font + "\n    private fun resolveFontFileName", s, count=1)
if count != 1:
    raise SystemExit("Could not patch chooseFont")

old_label = '    private fun fontLabel() = if (config.fontType == FONT_TYPE_CUSTOM) t("سفارشی: ${config.fontName}", "Custom: ${config.fontName}") else t("پیش‌فرض سیستم", "System default")'
new_label = '''    private fun fontLabel(): String {
        if (config.fontType != FONT_TYPE_CUSTOM) return t("پیش‌فرض سیستم", "System default")
        PersianFontCatalog.fonts.firstOrNull { it.fileName == config.fontName }?.let {
            return it.title
        }
        return t("سفارشی: ${config.fontName}", "Custom: ${config.fontName}")
    }'''
s = s.replace(old_label, new_label)

old_apply = '''    private fun applyPersianFont(view: View) {
        val typeface = ResourcesCompat.getFont(this, R.font.vazirmatn_regular) ?: return
        if (view is TextView) view.typeface = typeface
        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) applyPersianFont(view.getChildAt(i))
    }'''
new_apply = '''    private fun applyPersianFont(view: View) {
        val typeface = if (config.fontType == FONT_TYPE_CUSTOM) {
            FontHelper.getTypeface(this)
        } else {
            ResourcesCompat.getFont(this, R.font.vazirmatn_regular) ?: Typeface.DEFAULT
        }
        if (view is TextView) view.typeface = typeface
        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) applyPersianFont(view.getChildAt(i))
    }'''
s = s.replace(old_apply, new_apply)
settings.write_text(s)

m = manifest.read_text()
if "android.permission.INTERNET" not in m:
    m = m.replace(
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android"',
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android"',
        1,
    )
    marker = '    <uses-permission android:name="android.permission.READ_SMS" />\n'
    m = m.replace(marker, '    <uses-permission android:name="android.permission.INTERNET" />\n' + marker, 1)
manifest.write_text(m)

print("SETTINGS_FONT_PICKER_PATCH_OK")
