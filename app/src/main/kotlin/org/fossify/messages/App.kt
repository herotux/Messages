package org.fossify.messages

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.PERMISSION_READ_SMS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MySearchMenu
import org.fossify.messages.activities.AdvancedSearchActivity
import org.fossify.messages.activities.MainActivity
import org.fossify.messages.adapters.SearchResultsAdapter
import org.fossify.messages.extensions.rescheduleAllScheduledMessages
import org.fossify.messages.helpers.MessagingCache
import org.fossify.messages.helpers.ProviderSearchBridge

class App : FossifyApp() {
    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        if (hasPermission(PERMISSION_READ_CONTACTS)) {
            listOf(
                ContactsContract.Contacts.CONTENT_URI,
                ContactsContract.Data.CONTENT_URI,
                ContactsContract.DisplayPhoto.CONTENT_URI
            ).forEach {
                try {
                    contentResolver.registerContentObserver(it, true, contactsObserver)
                } catch (_: Exception) {
                }
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
            if (activity !is MainActivity) return

            val advancedButton = activity.findViewById<android.view.View>(R.id.advanced_search_button)
            advancedButton?.apply {
                isClickable = true
                isFocusable = true
                bringToFront()
                setOnClickListener {
                    activity.startActivity(Intent(activity, AdvancedSearchActivity::class.java))
                }
            }

            if (!hasPermission(PERMISSION_READ_SMS)) return
            val searchMenu = activity.findViewById<MySearchMenu>(R.id.main_menu) ?: return
            val originalListener = searchMenu.onSearchTextChangedListener

            searchMenu.onSearchTextChangedListener = { text ->
                originalListener?.invoke(text)
                if (text.trim().length < 2) return@onSearchTextChangedListener

                ProviderSearchBridge.search(activity, text) { providerResults ->
                    val resultsView = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.search_results_list)
                    val adapter = resultsView?.adapter as? SearchResultsAdapter ?: return@search
                    ProviderSearchBridge.mergeIntoAdapter(adapter, providerResults, text)
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
