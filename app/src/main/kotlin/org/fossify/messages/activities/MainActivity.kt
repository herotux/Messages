package org.fossify.messages.activities

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.provider.Telephony
import android.text.TextUtils
import androidx.appcompat.content.res.AppCompatResources
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.appLaunched
import org.fossify.commons.extensions.appLockManager
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.areSystemAnimationsEnabled
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.checkAppSideloading
import org.fossify.commons.extensions.checkWhatsNew
import org.fossify.commons.extensions.convertToBitmap
import org.fossify.commons.extensions.fadeIn
import org.fossify.commons.extensions.formatDateOrTime
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.underlineText
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.LICENSE_EVENT_BUS
import org.fossify.commons.helpers.LICENSE_INDICATOR_FAST_SCROLL
import org.fossify.commons.helpers.LICENSE_SMS_MMS
import org.fossify.commons.helpers.LOWER_ALPHA
import org.fossify.commons.helpers.MyContactsContentProvider
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.PERMISSION_READ_SMS
import org.fossify.commons.helpers.PERMISSION_SEND_SMS
import org.fossify.commons.helpers.SHORT_ANIMATION_DURATION
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isQPlus
import org.fossify.commons.models.FAQItem
import org.fossify.commons.models.Release
import org.fossify.messages.BuildConfig
import org.fossify.messages.R
import org.fossify.messages.adapters.ConversationsAdapter
import org.fossify.messages.adapters.SearchResultsAdapter
import org.fossify.messages.databinding.ActivityMainBinding
import org.fossify.messages.extensions.checkAndDeleteOldRecycleBinMessages
import org.fossify.messages.extensions.clearAllMessagesIfNeeded
import org.fossify.messages.extensions.clearExpiredScheduledMessages
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.conversationsDB
import org.fossify.messages.extensions.getConversations
import org.fossify.messages.extensions.getMessages
import org.fossify.messages.extensions.insertOrUpdateConversation
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.helpers.SEARCHED_MESSAGE_ID
import org.fossify.messages.helpers.THREAD_ID
import org.fossify.messages.helpers.THREAD_TITLE
import org.fossify.messages.models.Conversation
import org.fossify.messages.models.Events
import org.fossify.messages.models.Message
import org.fossify.messages.models.SearchResult
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : SimpleActivity() {
    override var isSearchBarEnabled = true
    private val MAKE_DEFAULT_APP_REQUEST = 1
    private var storedTextColor = 0
    private var storedFontSize = 0
    private var lastSearchedText = ""
    private var bus: EventBus? = null
    private val binding by viewBinding(ActivityMainBinding::inflate)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.advancedSearchButton.setOnClickListener { startActivity(Intent(this, AdvancedSearchActivity::class.java)) }
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.conversationsList))
        checkAndDeleteOldRecycleBinMessages()
        clearAllMessagesIfNeeded { loadMessages() }
        if (checkAppSideloading()) return
    }

    override fun onResume() {
        super.onResume()
        updateMenuColors()
        refreshMenuItems()
        getOrCreateConversationsAdapter().apply {
            if (storedTextColor != getProperTextColor()) updateTextColor(getProperTextColor())
            if (storedFontSize != config.fontSize) updateFontSize()
        }
    }

    private fun getNewConversations(cachedConversations: ArrayList<Conversation>) {
        val privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
        ensureBackgroundThread {
            val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
            val conversations = getConversations(privateContacts = privateContacts)
            conversations.forEach { clonedConversation ->
                val threadIds = cachedConversations.map { it.threadId }
                if (!threadIds.contains(clonedConversation.threadId)) { conversationsDB.insertOrUpdate(clonedConversation); cachedConversations.add(clonedConversation) }
            }
            cachedConversations.forEach { cachedConversation ->
                val threadId = cachedConversation.threadId
                val isTemporaryThread = cachedConversation.isScheduled
                val isConversationDeleted = !conversations.map { it.threadId }.contains(threadId)
                if (isConversationDeleted && !isTemporaryThread) conversationsDB.deleteThreadId(threadId)
                val newConversation = conversations.find { it.phoneNumber == cachedConversation.phoneNumber }
                if (isTemporaryThread && newConversation != null) {
                    conversationsDB.deleteThreadId(threadId)
                    messagesDB.getScheduledThreadMessages(threadId).forEach { message -> messagesDB.insertOrUpdate(message.copy(threadId = newConversation.threadId)) }
                    insertOrUpdateConversation(newConversation, cachedConversation)
                }
            }
            cachedConversations.forEach { cachedConv ->
                val conv = conversations.find { it.threadId == cachedConv.threadId && !Conversation.areContentsTheSame(old = cachedConv, new = it) }
                if (conv != null) insertOrUpdateConversation(conv)
            }
            val allConversations = conversationsDB.getNonArchived() as ArrayList<Conversation>
            runOnUiThread { setupConversations(allConversations) }
            if (config.appRunCount == 1) {
                conversations.map { it.threadId }.forEach { threadId -> getMessages(threadId, includeScheduledMessages = false).chunked(30).forEach { currentMessages -> messagesDB.insertMessages(*currentMessages.toTypedArray()) } }
            }
        }
    }

    private fun getOrCreateConversationsAdapter(): ConversationsAdapter {
        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = ConversationsAdapter(activity = this, recyclerView = binding.conversationsList, onRefresh = { notifyDatasetChanged() }, itemClick = { handleConversationClick(it) })
            binding.conversationsList.adapter = currAdapter
            if (areSystemAnimationsEnabled) binding.conversationsList.scheduleLayoutAnimation()
        }
        return currAdapter as ConversationsAdapter
    }

    private fun setupConversations(conversations: ArrayList<Conversation>, cached: Boolean = false) {
        val sortedConversations = conversations.sortedWith(compareByDescending<Conversation> { config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending { it.date }).toMutableList() as ArrayList<Conversation>
        if (cached && config.appRunCount == 1) showOrHideProgress(conversations.isEmpty()) else { showOrHideProgress(false); showOrHidePlaceholder(conversations.isEmpty()) }
        try { getOrCreateConversationsAdapter().apply { updateConversations(sortedConversations) { if (!cached) showOrHidePlaceholder(currentList.isEmpty()) } } } catch (_: Exception) { }
    }

    private fun showOrHideProgress(show: Boolean) { if (show) { binding.conversationsProgressBar.show(); binding.noConversationsPlaceholder.beVisible(); binding.noConversationsPlaceholder.text = getString(R.string.loading_messages) } else { binding.conversationsProgressBar.hide(); binding.noConversationsPlaceholder.beGone() } }
    private fun showOrHidePlaceholder(show: Boolean) { binding.conversationsFastscroller.beGoneIf(show); binding.noConversationsPlaceholder.beVisibleIf(show); binding.noConversationsPlaceholder.text = getString(R.string.no_conversations_found); binding.noConversationsPlaceholder2.beVisibleIf(show) }
    private fun fadeOutSearch() { binding.searchHolder.animate().alpha(0f).setDuration(SHORT_ANIMATION_DURATION).withEndAction { binding.searchHolder.beGone(); searchTextChanged("", true) }.start() }
    @SuppressLint("NotifyDataSetChanged") private fun notifyDatasetChanged() { getOrCreateConversationsAdapter().notifyDataSetChanged() }
    private fun handleConversationClick(any: Any) { Intent(this, ThreadActivity::class.java).apply { val conversation = any as Conversation; putExtra(THREAD_ID, conversation.threadId); putExtra(THREAD_TITLE, conversation.title); startActivity(this) } }
}
