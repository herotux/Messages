package org.fossify.messages

import android.app.Activity
import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.activities.MainActivity
import org.fossify.messages.activities.ThreadActivity
import org.fossify.messages.extensions.rescheduleAllScheduledMessages
import org.fossify.messages.helpers.BankAccountsFeature
import org.fossify.messages.helpers.BankCardsCrashLogger
import org.fossify.messages.helpers.ConversationFolderManager
import org.fossify.messages.helpers.MessagingCache
import org.fossify.messages.helpers.PersianThreadFontInstaller

class App : FossifyApp() {
    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        BankCardsCrashLogger.install(this)
        registerActivityLifecycleCallbacks(folderUiLifecycleCallbacks)
        if (hasPermission(PERMISSION_READ_CONTACTS)) {
            listOf(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Data.CONTENT_URI, ContactsContract.DisplayPhoto.CONTENT_URI).forEach {
                try { contentResolver.registerContentObserver(it, true, contactsObserver) } catch (_: Exception) { }
            }
        }
        ensureBackgroundThread { rescheduleAllScheduledMessages() }
    }

    private val folderUiLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityResumed(activity: Activity) {
            if (activity is MainActivity) {
                activity.findViewById<android.view.View>(R.id.folder_tabs)?.visibility =
                    if (ConversationFolderManager.areFoldersVisible(activity)) android.view.View.VISIBLE else android.view.View.GONE
                BankAccountsFeature.installPersianFonts(activity)
            }
            if (activity is ThreadActivity) {
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

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            MessagingCache.namePhoto.evictAll()
            MessagingCache.participantsCache.evictAll()
        }
    }
}
