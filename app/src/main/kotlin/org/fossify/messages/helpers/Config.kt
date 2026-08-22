package org.fossify.messages.helpers

import android.content.Context
import org.fossify.commons.helpers.BaseConfig
import org.fossify.messages.extensions.getDefaultKeyboardHeight
import org.fossify.messages.models.Conversation

class Config(context: Context) : BaseConfig(context) {
    companion object { fun newInstance(context: Context) = Config(context) }

    fun saveUseSIMIdAtNumber(number: String, SIMId: Int) { prefs.edit().putInt(USE_SIM_ID_PREFIX + number, SIMId).apply() }
    fun getUseSIMIdAtNumber(number: String) = prefs.getInt(USE_SIM_ID_PREFIX + number, 0)

    var showCharacterCounter: Boolean
        get() = prefs.getBoolean(SHOW_CHARACTER_COUNTER, false)
        set(value) = prefs.edit().putBoolean(SHOW_CHARACTER_COUNTER, value).apply()
    var useSimpleCharacters: Boolean
        get() = prefs.getBoolean(USE_SIMPLE_CHARACTERS, false)
        set(value) = prefs.edit().putBoolean(USE_SIMPLE_CHARACTERS, value).apply()
    var sendOnEnter: Boolean
        get() = prefs.getBoolean(SEND_ON_ENTER, false)
        set(value) = prefs.edit().putBoolean(SEND_ON_ENTER, value).apply()
    var enableDeliveryReports: Boolean
        get() = prefs.getBoolean(ENABLE_DELIVERY_REPORTS, false)
        set(value) = prefs.edit().putBoolean(ENABLE_DELIVERY_REPORTS, value).apply()
    var sendLongMessageMMS: Boolean
        get() = prefs.getBoolean(SEND_LONG_MESSAGE_MMS, false)
        set(value) = prefs.edit().putBoolean(SEND_LONG_MESSAGE_MMS, value).apply()
    var sendGroupMessageMMS: Boolean
        get() = prefs.getBoolean(SEND_GROUP_MESSAGE_MMS, false)
        set(value) = prefs.edit().putBoolean(SEND_GROUP_MESSAGE_MMS, value).apply()
    val isGroupMessageMmsPreferenceResolved: Boolean get() = prefs.contains(SEND_GROUP_MESSAGE_MMS)
    var lockScreenVisibilitySetting: Int
        get() = prefs.getInt(LOCK_SCREEN_VISIBILITY, LOCK_SCREEN_SENDER_MESSAGE)
        set(value) = prefs.edit().putInt(LOCK_SCREEN_VISIBILITY, value).apply()
    var mmsFileSizeLimit: Long
        get() = prefs.getLong(MMS_FILE_SIZE_LIMIT, FILE_SIZE_200_KB)
        set(value) = prefs.edit().putLong(MMS_FILE_SIZE_LIMIT, value).apply()
    var pinnedConversations: Set<String>
        get() = prefs.getStringSet(PINNED_CONVERSATIONS, HashSet())!!
        set(value) = prefs.edit().putStringSet(PINNED_CONVERSATIONS, value).apply()
    fun addPinnedConversationByThreadId(threadId: Long) { pinnedConversations = pinnedConversations.plus(threadId.toString()) }
    fun addPinnedConversations(conversations: List<Conversation>) { pinnedConversations = pinnedConversations.plus(conversations.map { it.threadId.toString() }) }
    fun removePinnedConversationByThreadId(threadId: Long) { pinnedConversations = pinnedConversations.minus(threadId.toString()) }
    fun removePinnedConversations(conversations: List<Conversation>) { pinnedConversations = pinnedConversations.minus(conversations.map { it.threadId.toString() }) }
    var blockedKeywords: Set<String>
        get() = prefs.getStringSet(BLOCKED_KEYWORDS, HashSet())!!
        set(value) = prefs.edit().putStringSet(BLOCKED_KEYWORDS, value).apply()
    fun addBlockedKeyword(keyword: String) { blockedKeywords = blockedKeywords.plus(keyword) }
    fun removeBlockedKeyword(keyword: String) { blockedKeywords = blockedKeywords.minus(keyword) }
    var exportSms: Boolean
        get() = prefs.getBoolean(EXPORT_SMS, true)
        set(value) = prefs.edit().putBoolean(EXPORT_SMS, value).apply()
    var exportMms: Boolean
        get() = prefs.getBoolean(EXPORT_MMS, true)
        set(value) = prefs.edit().putBoolean(EXPORT_MMS, value).apply()
    var importSms: Boolean
        get() = prefs.getBoolean(IMPORT_SMS, true)
        set(value) = prefs.edit().putBoolean(IMPORT_SMS, value).apply()
    var importMms: Boolean
        get() = prefs.getBoolean(IMPORT_MMS, true)
        set(value) = prefs.edit().putBoolean(IMPORT_MMS, value).apply()
    var wasDbCleared: Boolean
        get() = prefs.getBoolean(WAS_DB_CLEARED, false)
        set(value) = prefs.edit().putBoolean(WAS_DB_CLEARED, value).apply()
    var keyboardHeight: Int
        get() = prefs.getInt(SOFT_KEYBOARD_HEIGHT, context.getDefaultKeyboardHeight())
        set(value) = prefs.edit().putInt(SOFT_KEYBOARD_HEIGHT, value).apply()
    var useRecycleBin: Boolean
        get() = prefs.getBoolean(USE_RECYCLE_BIN, false)
        set(value) = prefs.edit().putBoolean(USE_RECYCLE_BIN, value).apply()
    var lastRecycleBinCheck: Long
        get() = prefs.getLong(LAST_RECYCLE_BIN_CHECK, 0L)
        set(value) = prefs.edit().putLong(LAST_RECYCLE_BIN_CHECK, value).apply()
    var isArchiveAvailable: Boolean
        get() = prefs.getBoolean(IS_ARCHIVE_AVAILABLE, true)
        set(value) = prefs.edit().putBoolean(IS_ARCHIVE_AVAILABLE, value).apply()
    var customNotifications: Set<String>
        get() = prefs.getStringSet(CUSTOM_NOTIFICATIONS, HashSet())!!
        set(value) = prefs.edit().putStringSet(CUSTOM_NOTIFICATIONS, value).apply()
    fun addCustomNotificationsByThreadId(threadId: Long) { customNotifications = customNotifications.plus(threadId.toString()) }
    fun removeCustomNotificationsByThreadId(threadId: Long) { customNotifications = customNotifications.minus(threadId.toString()) }
    var lastBlockedKeywordExportPath: String
        get() = prefs.getString(LAST_BLOCKED_KEYWORD_EXPORT_PATH, "")!!
        set(value) = prefs.edit().putString(LAST_BLOCKED_KEYWORD_EXPORT_PATH, value).apply()
    var keepConversationsArchived: Boolean
        get() = prefs.getBoolean(KEEP_CONVERSATIONS_ARCHIVED, false)
        set(value) = prefs.edit().putBoolean(KEEP_CONVERSATIONS_ARCHIVED, value).apply()
    var usePersianCalendar: Boolean
        get() = prefs.getBoolean(USE_PERSIAN_CALENDAR, false)
        set(value) = prefs.edit().putBoolean(USE_PERSIAN_CALENDAR, value).apply()
}
