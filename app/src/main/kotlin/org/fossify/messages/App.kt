package org.fossify.messages

import android.app.Activity
import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.activities.BankCardsActivity
import org.fossify.messages.activities.MainActivity
import org.fossify.messages.activities.SettingsActivity
import org.fossify.messages.activities.ThemeBuilderActivity
import org.fossify.messages.activities.ThreadActivity
import org.fossify.messages.extensions.rescheduleAllScheduledMessages
import org.fossify.messages.helpers.AppLanguageManager
import org.fossify.messages.helpers.BankAccountsFeature
import org.fossify.messages.helpers.BankCardsCrashLogger
import org.fossify.messages.helpers.ConversationFolderManager
import org.fossify.messages.helpers.HomaViewSystem
import org.fossify.messages.helpers.MessagingCache
import org.fossify.messages.helpers.PersianThreadFontInstaller
import org.fossify.messages.helpers.TapsellAds

class App : FossifyApp() {
    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        // Initialize the app language before the first Activity is created.
        AppLanguageManager.initialize(this)
        applyUiNightModePreference()
        BankCardsCrashLogger.install(this)
        TapsellAds.initialize()
        registerActivityLifecycleCallbacks(folderUiLifecycleCallbacks)
        if (hasPermission(PERMISSION_READ_CONTACTS)) {
            listOf(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Data.CONTENT_URI, ContactsContract.DisplayPhoto.CONTENT_URI).forEach {
                try { contentResolver.registerContentObserver(it, true, contactsObserver) } catch (_: Exception) { }
            }
        }
        ensureBackgroundThread { rescheduleAllScheduledMessages() }
    }

    private fun applyUiNightModePreference() {
        val prefs = getSharedPreferences("messages_settings_ui", MODE_PRIVATE)
        if (!prefs.contains("dark_mode")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            return
        }
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.getBoolean("dark_mode", false)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private val folderUiLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityResumed(activity: Activity) {
            AppLanguageManager.apply(activity)

            // Legacy View screens keep their feature implementations, but their chrome
            // is now governed by the shared Homa contract.
            if (activity is SettingsActivity || activity is ThemeBuilderActivity) {
                HomaViewSystem.apply(activity)
            } else if (activity is BankCardsActivity) {
                // Bank cards already owns its inset contract; only normalize its surfaces/cards.
                HomaViewSystem.style(activity)
            }

            if (activity is MainActivity) {
                activity.findViewById<android.view.View>(R.id.folder_tabs)?.visibility =
                    if (ConversationFolderManager.areFoldersVisible(activity)) android.view.View.VISIBLE else android.view.View.GONE
                BankAccountsFeature.installPersianFonts(activity)
                TapsellAds.showBanner(activity)
                clearOverflowButtonBackgrounds(activity)
            }
            if (activity is ThreadActivity) {
                TapsellAds.hideBanner()
                BankAccountsFeature.installMessageCardLinks(activity)
                BankAccountsFeature.installPersianFonts(activity)
                PersianThreadFontInstaller.install(activity)
            }
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private fun clearOverflowButtonBackgrounds(activity: Activity) {
        val root = activity.findViewById<View>(R.id.main_menu) as? ViewGroup ?: return
        clearOverflowButtonBackgrounds(root)
    }

    private fun clearOverflowButtonBackgrounds(parent: ViewGroup) {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val description = child.contentDescription?.toString().orEmpty()
            if (description.contains("more", ignoreCase = true) ||
                description.contains("options", ignoreCase = true) ||
                description.contains("گزینه", ignoreCase = true)) {
                child.background = null
            }
            if (child is ViewGroup) {
                clearOverflowButtonBackgrounds(child)
            }
        }
    }

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            MessagingCache.namePhoto.evictAll()
            MessagingCache.participantsCache.evictAll()
        }
    }
}