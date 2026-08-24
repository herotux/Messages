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
import org.fossify.commons.helpers.LICENSE_INDICATOR_FAST_SCROLLER
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
import org.fossify.messages.helpers.ProviderSearchBridge
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
            updateDrafts()
        }
        updateTextColors(binding.mainCoordinator)
        binding.searchHolder.setBackgroundColor(getProperBackgroundColor())
        val properPrimaryColor = getProperPrimaryColor()
        binding.noConversationsPlaceholder2.setTextColor(properPrimaryColor)
        binding.noConversationsPlaceholder2.underlineText()
        binding.conversationsFastscroller.updateColors(properPrimaryColor)
        binding.conversationsProgressBar.setIndicatorColor(properPrimaryColor)
        binding.conversationsProgressBar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)
        checkShortcut()
    }

    override fun onPause() { super.onPause(); storeStateVariables() }
    override fun onDestroy() { super.onDestroy(); bus?.unregister(this) }

    override fun onBackPressedCompat(): Boolean {
        return if (binding.mainMenu.isSearchOpen) {
            binding.mainMenu.closeSearch(); true
        } else { appLockManager.lock(); false }
    }

    private fun setupOptionsMenu() {
        binding.mainMenu.requireToolbar().inflateMenu(R.menu.menu_main)
        binding.mainMenu.toggleHideOnScroll(true)
        binding.mainMenu.setupMenu()
        binding.mainMenu.onSearchClosedListener = { fadeOutSearch() }
        binding.mainMenu.onSearchTextChangedListener = { text ->
            if (text.isNotEmpty()) {
                if (binding.searchHolder.alpha < 1f) binding.searchHolder.fadeIn()
            } else fadeOutSearch()
            searchTextChanged(text)
        }
        binding.advancedSearchButton.setOnClickListener { launchAdvancedSearch(lastSearchedText) }
        binding.mainMenu.requireToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.show_recycle_bin -> launchRecycleBin()
                R.id.show_archived -> launchArchivedConversations()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
    }

    private fun launchAdvancedSearch(query: String = "") {
        hideKeyboard()
        startActivity(Intent(this, AdvancedSearchActivity::class.java).apply {
            putExtra("advanced_search_query", query)
        })
    }

    private fun refreshMenuItems() {
        binding.mainMenu.requireToolbar().menu.apply {
            findItem(R.id.show_recycle_bin).isVisible = config.useRecycleBin
            findItem(R.id.show_archived).isVisible = config.isArchiveAvailable
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == RESULT_OK) askPermissions() else finish()
        }
    }

    private fun storeStateVariables() { storedTextColor = getProperTextColor(); storedFontSize = config.fontSize }
    private fun updateMenuColors() { binding.mainMenu.updateColors() }

    private fun loadMessages() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) askPermissions()
                else startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS), MAKE_DEFAULT_APP_REQUEST)
            } else { toast(org.fossify.commons.R.string.unknown_error_occurred); finish() }
        } else if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) askPermissions()
        else startActivityForResult(Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        }, MAKE_DEFAULT_APP_REQUEST)
    }

    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) handlePermission(PERMISSION_SEND_SMS) {
                if (it) handlePermission(PERMISSION_READ_CONTACTS) {
                    handleNotificationPermission { granted ->
                        if (!granted) PermissionRequiredDialog(this, org.fossify.commons.R.string.allow_notifications_incoming_messages) { openNotificationSettings() }
                    }
                    initMessenger(); bus = EventBus.getDefault()
                    try { bus!!.register(this) } catch (_: Exception) {}
                } else finish()
            } else finish()
        }
    }

    private fun initMessenger() {
        checkWhatsNewDialog(); storeStateVariables(); getCachedConversations()
        binding.noConversationsPlaceholder2.setOnClickListener { launchNewConversation() }
        binding.conversationsFab.setOnClickListener { launchNewConversation() }
    }

    private fun getCachedConversations() {
        ensureBackgroundThread {
            val conversations = try { conversationsDB.getNonArchived().toMutableList() as ArrayList<Conversation> } catch (_: Exception) { ArrayList() }
            val archived = try { conversationsDB.getAllArchived() } catch (_: Exception) { listOf() }
            runOnUiThread {
                setupConversations(conversations, cached = true)
                getNewConversations((conversations + archived).toMutableList() as ArrayList<Conversation>)
            }
            conversations.forEach { clearExpiredScheduledMessages(it.threadId) }
        }
    }

    private fun getNewConversations(cachedConversations: ArrayList<Conversation>) {
        val privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
        ensureBackgroundThread {
            val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
            val conversations = getConversations(privateContacts = privateContacts)
            conversations.forEach { clonedConversation ->
                if (!cachedConversations.map { it.threadId }.contains(clonedConversation.threadId)) {
                    conversationsDB.insertOrUpdate(clonedConversation); cachedConversations.add(clonedConversation)
                }
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
            if (config.appRunCount == 1) conversations.map { it.threadId }.forEach { threadId ->
                getMessages(threadId, includeScheduledMessages = false).chunked(30).forEach { messagesDB.insertMessages(*it.toTypedArray()) }
            }
        }
    }

    private fun getOrCreateConversationsAdapter(): ConversationsAdapter {
        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = ConversationsAdapter(this, binding.conversationsList, { notifyDatasetChanged() }) { handleConversationClick(it) }
            binding.conversationsList.adapter = currAdapter
            if (areSystemAnimationsEnabled) binding.conversationsList.scheduleLayoutAnimation()
        }
        return currAdapter as ConversationsAdapter
    }

    private fun setupConversations(conversations: ArrayList<Conversation>, cached: Boolean = false) {
        val sortedConversations = conversations.sortedWith(compareByDescending<Conversation> { config.pinnedConversations.contains(it.threadId.toString()) }.thenByDescending { it.date }).toMutableList() as ArrayList<Conversation>
        if (cached && config.appRunCount == 1) showOrHideProgress(conversations.isEmpty())
        else { showOrHideProgress(false); showOrHidePlaceholder(conversations.isEmpty()) }
        try { getOrCreateConversationsAdapter().updateConversations(sortedConversations) { if (!cached) showOrHidePlaceholder(currentList.isEmpty()) } } catch (_: Exception) {}
    }

    private fun showOrHideProgress(show: Boolean) {
        if (show) { binding.conversationsProgressBar.show(); binding.noConversationsPlaceholder.beVisible(); binding.noConversationsPlaceholder.text = getString(R.string.loading_messages) }
        else { binding.conversationsProgressBar.hide(); binding.noConversationsPlaceholder.beGone() }
    }
    private fun showOrHidePlaceholder(show: Boolean) {
        binding.conversationsFastscroller.beGoneIf(show); binding.noConversationsPlaceholder.beVisibleIf(show); binding.noConversationsPlaceholder.text = getString(R.string.no_conversations_found); binding.noConversationsPlaceholder2.beVisibleIf(show)
    }
    private fun fadeOutSearch() {
        binding.searchHolder.animate().alpha(0f).setDuration(SHORT_ANIMATION_DURATION).withEndAction { binding.searchHolder.beGone(); searchTextChanged("", true) }.start()
    }
    @SuppressLint("NotifyDataSetChanged") private fun notifyDatasetChanged() { getOrCreateConversationsAdapter().notifyDataSetChanged() }

    private fun handleConversationClick(any: Any) {
        val conversation = any as Conversation
        startActivity(Intent(this, ThreadActivity::class.java).apply { putExtra(THREAD_ID, conversation.threadId); putExtra(THREAD_TITLE, conversation.title) })
    }
    private fun launchNewConversation() { hideKeyboard(); startActivity(Intent(this, NewConversationActivity::class.java)) }

    @SuppressLint("NewApi") private fun checkShortcut() {
        val appIconColor = config.appIconColor
        if (config.lastHandledShortcutColor != appIconColor) {
            val newConversation = getCreateNewContactShortcut(appIconColor)
            try { getSystemService(ShortcutManager::class.java).dynamicShortcuts = listOf(newConversation); config.lastHandledShortcutColor = appIconColor } catch (_: Exception) {}
        }
    }
    @SuppressLint("NewApi") private fun getCreateNewContactShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_conversation)
        val drawable = AppCompatResources.getDrawable(this, org.fossify.commons.R.drawable.shortcut_plus)
        (drawable as LayerDrawable).findDrawableByLayerId(org.fossify.commons.R.id.shortcut_plus_background).applyColorFilter(appIconColor)
        return ShortcutInfo.Builder(this, "new_conversation").setShortLabel(newEvent).setLongLabel(newEvent).setIcon(Icon.createWithBitmap(drawable.convertToBitmap())).setIntent(Intent(this, NewConversationActivity::class.java).apply { action = Intent.ACTION_VIEW }).setRank(0).build()
    }

    private fun searchTextChanged(text: String, forceUpdate: Boolean = false) {
        if (!binding.mainMenu.isSearchOpen && !forceUpdate) return
        lastSearchedText = text
        binding.searchPlaceholder2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            ensureBackgroundThread {
                val searchQuery = "%$text%"
                val messages = messagesDB.getMessagesWithText(searchQuery)
                val conversations = conversationsDB.getConversationsWithText(searchQuery)
                if (text != lastSearchedText) return@ensureBackgroundThread
                showSearchResults(messages, conversations, text, emptyList())
                ProviderSearchBridge.search(this, text) { providerResults ->
                    if (text == lastSearchedText) showSearchResults(messages, conversations, text, providerResults)
                }
            }
        } else {
            binding.searchPlaceholder.beVisible(); binding.searchResultsList.beGone()
        }
    }

    private fun showSearchResults(messages: List<Message>, conversations: List<Conversation>, searchedText: String, providerResults: List<SearchResult>) {
        val searchResults = ArrayList<SearchResult>()
        conversations.forEach { conversation ->
            searchResults.add(SearchResult(-1, conversation.title, conversation.phoneNumber, (conversation.date * 1000L).formatDateOrTime(this, true, true), conversation.threadId, conversation.photoUri))
        }
        messages.sortedByDescending { it.id }.forEach { message ->
            var recipient = message.senderName
            if (recipient.isEmpty() && message.participants.isNotEmpty()) recipient = TextUtils.join(", ", message.participants.map { it.name })
            searchResults.add(SearchResult(message.id, recipient, message.body, (message.date * 1000L).formatDateOrTime(this, true, true), message.threadId, message.senderPhotoUri))
        }
        val seen = HashSet<Long>()
        searchResults.forEach { if (it.messageId >= 0) seen.add(it.messageId) }
        providerResults.forEach { if (it.messageId < 0 || seen.add(it.messageId)) searchResults.add(it) }
        searchResults.sortByDescending { it.date }
        runOnUiThread {
            binding.searchResultsList.beVisibleIf(searchResults.isNotEmpty())
            binding.searchPlaceholder.beVisibleIf(searchResults.isEmpty())
            val currAdapter = binding.searchResultsList.adapter
            if (currAdapter == null) {
                SearchResultsAdapter(this, searchResults, binding.searchResultsList, searchedText) {
                    hideKeyboard()
                    val result = it as SearchResult
                    startActivity(Intent(this, ThreadActivity::class.java).apply {
                        putExtra(THREAD_ID, result.threadId); putExtra(THREAD_TITLE, result.title); putExtra(SEARCHED_MESSAGE_ID, result.messageId)
                    })
                }.also { binding.searchResultsList.adapter = it }
            } else (currAdapter as SearchResultsAdapter).updateItems(searchResults, searchedText)
        }
    }

    private fun launchRecycleBin() { hideKeyboard(); startActivity(Intent(applicationContext, RecycleBinConversationsActivity::class.java)) }
    private fun launchArchivedConversations() { hideKeyboard(); startActivity(Intent(applicationContext, ArchivedConversationsActivity::class.java)) }
    private fun launchSettings() { hideKeyboard(); startActivity(Intent(applicationContext, SettingsActivity::class.java)) }

    private fun launchAbout() {
        val licenses = LICENSE_EVENT_BUS or LICENSE_SMS_MMS or LICENSE_INDICATOR_FAST_SCROLLER
        val faqItems = arrayListOf(
            FAQItem(R.string.faq_2_title, R.string.faq_2_text),
            FAQItem(R.string.faq_3_title, R.string.faq_3_text),
            FAQItem(R.string.faq_4_title, R.string.faq_4_text),
            FAQItem(org.fossify.commons.R.string.faq_9_title_commons, org.fossify.commons.R.string.faq_9_text_commons)
        )
        if (!resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(org.fossify.commons.R.string.faq_2_title_commons, org.fossify.commons.R.string.faq_2_text_commons))
            faqItems.add(FAQItem(org.fossify.commons.R.string.faq_6_title_commons, org.fossify.commons.R.string.faq_6_text_commons))
        }
        startAboutActivity(appNameId = R.string.app_name, licenseMask = licenses, versionName = BuildConfig.VERSION_NAME, faqItems = faqItems, showFAQBeforeMail = true)
    }
    @Subscribe(threadMode = ThreadMode.MAIN) fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) { initMessenger() }
    private fun checkWhatsNewDialog() { arrayListOf<Release>().apply { checkWhatsNew(this, BuildConfig.VERSION_CODE) } }
}
