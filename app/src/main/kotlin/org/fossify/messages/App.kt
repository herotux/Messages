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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.PERMISSION_READ_SMS
import org.fossify.commons.helpers.ensureBackgroundThread
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

            val advancedButton = activity.findViewById<View>(R.id.advanced_search_button)
            advancedButton?.apply {
                isClickable = true
                isFocusable = true
                bringToFront()
                setOnClickListener {
                    activity.startActivity(Intent(activity, AdvancedSearchActivity::class.java))
                }
            }

            // The stock MainActivity search is Room-backed. Attach a second watcher
            // after MainActivity has initialized its own listener and supplement its
            // results with the real Android SMS Provider, so unopened threads are searchable.
            if (!hasPermission(PERMISSION_READ_SMS)) return
            val searchEditText = activity.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
                ?: return
            val resultsView = activity.findViewById<RecyclerView>(R.id.search_results_list) ?: return

            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s?.toString().orEmpty().trim()
                    if (query.length < 2) return

                    // Let MainActivity finish its Room query first, then merge provider results.
                    resultsView.postDelayed({
                        ProviderSearchBridge.search(activity, query) { providerResults ->
                            val adapter = resultsView.adapter as? SearchResultsAdapter ?: return@search
                            ProviderSearchBridge.mergeIntoAdapter(adapter, providerResults, query)
                        }
                    }, 40L)
                }

                override fun afterTextChanged(s: Editable?) = Unit
            })
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
