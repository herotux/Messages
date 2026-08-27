package org.fossify.messages.views

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.messages.R
import org.fossify.messages.adapters.BaseConversationsAdapter
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Messages-native conversation folder tabs.
 *
 * The navigation model follows a familiar, efficient folder-based messenger UX,
 * while the visual identity remains Fossify Messages. Folder switching is strictly
 * an in-memory operation and never triggers a Telephony/Room reload.
 */
class ConversationFolderTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : HorizontalScrollView(context, attrs) {
    private val tabsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var selectedId = ALL_ID
    private var adapter: BaseConversationsAdapter? = null
    private var folders = mutableListOf<Folder>()
    private var bindAttempts = 0

    init {
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setBackgroundColor(context.getProperBackgroundColor())
        addView(tabsContainer, LayoutParams(LayoutParams.WRAP_CONTENT, dp(48)))
        folders = loadFolders()
        rebuildTabs()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bindAdapterWhenReady()
    }

    private fun bindAdapterWhenReady() {
        if (adapter != null || bindAttempts >= MAX_BIND_ATTEMPTS) return
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.conversations_list)
        val currentAdapter = recyclerView?.adapter as? BaseConversationsAdapter
        if (currentAdapter != null) {
            bindAdapter(currentAdapter)
            return
        }
        bindAttempts++
        postDelayed({ bindAdapterWhenReady() }, ADAPTER_BIND_RETRY_MS)
    }

    /** Bind after the conversation adapter exists. Safe to call repeatedly. */
    fun bindAdapter(newAdapter: BaseConversationsAdapter) {
        adapter = newAdapter
        applySelectedFilter()
    }

    private fun rebuildTabs() {
        tabsContainer.removeAllViews()
        addTab(ALL_ID, "همه")
        addTab(UNREAD_ID, "خوانده‌نشده")
        addTab(BANKS_ID, "بانک‌ها")
        addTab(PERSONAL_ID, "شخصی")
        folders.forEach { addTab(it.id, it.name) }
        addTab(ADD_ID, "＋")
        updateSelection()
    }

    private fun addTab(id: String, title: String) {
        val view = TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
            textSize = 14f
            isSingleLine = true
            typeface = Typeface.DEFAULT
            setOnClickListener {
                if (id == ADD_ID) {
                    showCreateFolderDialog()
                } else {
                    selectedId = id
                    updateSelection()
                    applySelectedFilter()
                }
            }
            setOnLongClickListener {
                val folder = folders.firstOrNull { it.id == id }
                if (folder != null) {
                    showDeleteFolderDialog(folder)
                    true
                } else {
                    false
                }
            }
        }
        view.tag = id
        tabsContainer.addView(view, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    private fun updateSelection() {
        val primary = context.getProperPrimaryColor()
        for (i in 0 until tabsContainer.childCount) {
            val tab = tabsContainer.getChildAt(i) as TextView
            val selected = tab.tag == selectedId
            tab.setTextColor(if (selected) primary else Color.GRAY)
            tab.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            tab.setBackgroundColor(if (selected) withAlpha(primary, 0.10f) else Color.TRANSPARENT)
        }
    }

    private fun applySelectedFilter() {
        val currentAdapter = adapter ?: return
        when (selectedId) {
            ALL_ID -> currentAdapter.clearConversationFilter()
            UNREAD_ID -> currentAdapter.filterConversations { !it.read }
            BANKS_ID -> currentAdapter.filterConversations { currentAdapter.isBankConversation(it) }
            PERSONAL_ID -> currentAdapter.filterConversations {
                !it.isGroupConversation && !currentAdapter.isBankConversation(it)
            }
            else -> {
                val folder = folders.firstOrNull { it.id == selectedId } ?: return
                val query = folder.keyword.trim()
                currentAdapter.filterConversations { conversation ->
                    query.isNotEmpty() && (
                        conversation.title.contains(query, ignoreCase = true) ||
                            conversation.phoneNumber.contains(query, ignoreCase = true) ||
                            conversation.snippet.contains(query, ignoreCase = true)
                        )
                }
            }
        }
    }

    fun refreshForNewConversations() {
        applySelectedFilter()
    }

    private fun showCreateFolderDialog() {
        val nameInput = EditText(context).apply {
            hint = "نام پوشه"
            isSingleLine = true
        }
        val keywordInput = EditText(context).apply {
            hint = "کلمه یا عبارت برای فیلتر"
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(nameInput)
            addView(keywordInput)
        }

        AlertDialog.Builder(context)
            .setTitle("پوشه جدید")
            .setMessage("پوشه روی مکالمات از قبل بارگذاری‌شده فیلتر می‌شود و برای تعویض پوشه دوباره پیامک‌ها را نمی‌خواند.")
            .setView(box)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ایجاد") { _, _ ->
                val name = nameInput.text.toString().trim()
                val keyword = keywordInput.text.toString().trim()
                if (name.isEmpty() || keyword.isEmpty()) {
                    Toast.makeText(context, "نام و فیلتر پوشه را وارد کنید", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val folder = Folder("custom_${System.currentTimeMillis()}", name, keyword)
                folders.add(folder)
                saveFolders()
                selectedId = folder.id
                rebuildTabs()
                applySelectedFilter()
            }
            .show()
    }

    private fun showDeleteFolderDialog(folder: Folder) {
        AlertDialog.Builder(context)
            .setTitle("حذف پوشه")
            .setMessage("پوشه «${folder.name}» حذف شود؟")
            .setNegativeButton("لغو", null)
            .setPositiveButton("حذف") { _, _ ->
                folders.removeAll { it.id == folder.id }
                saveFolders()
                selectedId = ALL_ID
                rebuildTabs()
                applySelectedFilter()
            }
            .show()
    }

    private fun loadFolders(): MutableList<Folder> {
        val raw = preferences.getString(FOLDERS_KEY, null) ?: return mutableListOf()
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { index ->
                val item = array.getJSONObject(index)
                Folder(item.getString("id"), item.getString("name"), item.getString("keyword"))
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun saveFolders() {
        val array = JSONArray()
        folders.forEach {
            array.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("keyword", it.keyword)
            })
        }
        preferences.edit().putString(FOLDERS_KEY, array.toString()).apply()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((Color.alpha(color) * alpha).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))

    private data class Folder(val id: String, val name: String, val keyword: String)

    companion object {
        private const val PREFS_NAME = "messages_folder_tabs"
        private const val FOLDERS_KEY = "folders"
        private const val ALL_ID = "__all__"
        private const val UNREAD_ID = "__unread__"
        private const val BANKS_ID = "__banks__"
        private const val PERSONAL_ID = "__personal__"
        private const val ADD_ID = "__add__"
        private const val ADAPTER_BIND_RETRY_MS = 100L
        private const val MAX_BIND_ATTEMPTS = 50
    }
}
