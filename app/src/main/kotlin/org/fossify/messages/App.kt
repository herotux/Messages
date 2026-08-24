package org.fossify.messages

import android.app.Activity
import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.content.Intent
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.activities.AdvancedSearchActivity
import org.fossify.messages.activities.MainActivity
import org.fossify.messages.extensions.rescheduleAllScheduledMessages
import org.fossify.messages.helpers.MessagingCache

class App : FossifyApp() {
    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        if (hasPermission(PERMISSION_READ_CONTACTS)) {
            listOf(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Data.CONTENT_URI, ContactsContract.DisplayPhoto.CONTENT_URI).forEach {
                try { contentResolver.registerContentObserver(it, true, contactsObserver) } catch (_: Exception) { }
            }
        }
        registerActivityLifecycleCallbacks(advancedSearchCallbacks)
        ensureBackgroundThread { rescheduleAllScheduledMessages() }
    }

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            MessagingCache.namePhoto.evictAll()
            MessagingCache.participantsCache.evictAll()
        }
    }

    private val advancedSearchCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, state: Bundle?) {
            if (activity is MainActivity) {
                activity.findViewById<android.view.View>(R.id.advanced_search_button)?.setOnClickListener {
                    activity.startActivity(Intent(activity, AdvancedSearchActivity::class.java))
                }
            }
        }
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
