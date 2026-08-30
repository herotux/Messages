package org.fossify.messages

import android.app.Activity
import android.app.Application
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.activities.MainActivity
import org.fossify.messages.activities.SettingsActivity
import org.fossify.messages.activities.ThreadActivity
import org.fossify.messages.extensions.rescheduleAllScheduledMessages
import org.fossify.messages.helpers.BankAccountsFeature
import org.fossify.messages.helpers.ConversationFolderManager
import org.fossify.messages.helpers.MessagingCache

class App : FossifyApp() {
    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(folderUiLifecycleCallbacks)
        if (hasPermission(PERMISSION_READ_CONTACTS)) {
            listOf(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Data.CONTENT_URI, ContactsContract.DisplayPhoto.CONTENT_URI).forEach {
                try { contentResolver.registerContentObserver(it, true, contactsObserver) } catch (_: Exception) { }
            }
        }
        ensureBackgroundThread { rescheduleAllScheduledMessages() }
    }

    private val folderUiLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity is SettingsActivity) addFolderSettingsSection(activity)
        }
        override fun onActivityResumed(activity: Activity) {
            if (activity is MainActivity) activity.findViewById<View>(R.id.folder_tabs)?.visibility = if (ConversationFolderManager.areFoldersVisible(activity)) View.VISIBLE else View.GONE
            if (activity is SettingsActivity) { addFolderSettingsSection(activity); addBankSettingsSection(activity) }
            if (activity is ThreadActivity) { BankAccountsFeature.addPickerButton(activity); BankAccountsFeature.installMessageCardLinks(activity) }
        }
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private fun addFolderSettingsSection(activity: SettingsActivity) {
        val holder = activity.findViewById<LinearLayout>(R.id.settings_holder) ?: return
        if (holder.findViewWithTag<View>("conversation_folders_settings") != null) return
        val primary = activity.getProperPrimaryColor(); val density = activity.resources.displayMetrics.density
        val section = LinearLayout(activity).apply { tag="conversation_folders_settings"; orientation=LinearLayout.VERTICAL; setPadding((16*density).toInt(),(14*density).toInt(),(16*density).toInt(),0) }
        section.addView(TextView(activity).apply { text=activity.getString(R.string.conversation_folders); setTextColor(primary); textSize=13f; setTypeface(typeface,android.graphics.Typeface.BOLD); gravity=Gravity.START; setPadding(0,0,0,(8*density).toInt()) },LinearLayout.LayoutParams(-1,-2))
        section.addView(MaterialSwitch(activity).apply { text=activity.getString(R.string.show_conversation_folders); isChecked=ConversationFolderManager.areFoldersVisible(activity); minHeight=(56*density).toInt(); setPadding(0,0,0,(8*density).toInt()); setOnCheckedChangeListener { _,checked -> ConversationFolderManager.setFoldersVisible(activity,checked) } },LinearLayout.LayoutParams(-1,-2))
        section.addView(View(activity).apply { setBackgroundColor(Color.argb(30,128,128,128)) },LinearLayout.LayoutParams(-1,1))
        val archivedLabel=holder.findViewById<View>(R.id.settings_archived_messages_label); val index=if(archivedLabel!=null) holder.indexOfChild(archivedLabel) else holder.childCount; holder.addView(section,index.coerceAtLeast(0))
    }

    private fun addBankSettingsSection(activity: SettingsActivity) {
        val holder = activity.findViewById<LinearLayout>(R.id.settings_holder) ?: return
        if (holder.findViewWithTag<View>("bank_accounts_settings_section") != null) return
        val density = activity.resources.displayMetrics.density
        val section = LinearLayout(activity).apply {
            tag = "bank_accounts_settings_section"
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), 0)
        }
        section.addView(TextView(activity).apply {
            text = BankAccountsFeature.title(activity, "کارت‌های بانکی", "Bank cards")
            setTextColor(activity.getProperPrimaryColor())
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.START
            setPadding(0, 0, 0, (8 * density).toInt())
        }, LinearLayout.LayoutParams(-1, -2))
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (4 * density).toInt(), 0, (10 * density).toInt())
            setOnClickListener { BankAccountsFeature.showBankAccountManager(activity) }
        }
        row.addView(TextView(activity).apply {
            text = BankAccountsFeature.title(activity, "مدیریت کارت‌های بانکی", "Manage bank cards")
            textSize = 16f
            setTextColor(activity.getProperPrimaryColor())
        }, LinearLayout.LayoutParams(-1, -2))
        row.addView(TextView(activity).apply {
            text = BankAccountsFeature.title(activity, "افزودن، ویرایش و اسکن کارت بانکی", "Add, edit or scan a bank card")
            textSize = 13f
            setTextColor(Color.GRAY)
        }, LinearLayout.LayoutParams(-1, -2))
        section.addView(row, LinearLayout.LayoutParams(-1, -2))
        section.addView(View(activity).apply { setBackgroundColor(Color.argb(30,128,128,128)) }, LinearLayout.LayoutParams(-1,1))
        val archivedLabel = holder.findViewById<View>(R.id.settings_archived_messages_label)
        val index = if (archivedLabel != null) holder.indexOfChild(archivedLabel) else holder.childCount
        holder.addView(section, index.coerceAtLeast(0))
    }

    private val contactsObserver=object:ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange:Boolean,uri:Uri?){ MessagingCache.namePhoto.evictAll(); MessagingCache.participantsCache.evictAll() } }
}
