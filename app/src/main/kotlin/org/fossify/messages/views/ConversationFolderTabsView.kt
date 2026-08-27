package org.fossify.messages.views

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.CheckBox
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
import org.fossify.messages.helpers.ConversationFolderManager
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Messages-native conversation folders.
 *
 * Folder membership is explicit and persistent. Switching folders only filters the
 * already-loaded in-memory conversation list; it never queries Telephony again.
 */
class ConversationFolderTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : HorizontalScrollView(context, attrs) {
    private val tabsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var selectedId = ConversationFolderManager.getSelectedFolderId(context)
    private var adapter: BaseConversationsAdapter? = null
    private var folders = mutableListOf<ConversationFolderManager.Folder>()
    private var bindAttempts = 0
    private var downX = 0f
    private var downY = 0f
    private var swipeHandled = false

    init {
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setBackgroundColor(context.getProperBackgroundColor())
        addView(tabsContainer, LayoutParams(LayoutParams.WRAP_CONTENT, dp(48)))
        folders = ConversationFolderManager.getFolders(context)
        normalizeSelection()
        rebuildTabs()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bindAdapterWhenReady()
        attachConversationSwipe()
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

    private fun attachConversationSwipe() {
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.conversations_list) ?: run {
            postDelayed({ attachConversationSwipe() }, ADAPTER_BIND_RETRY_MS)
            return
        }
        recyclerView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    swipeHandled = false
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    val threshold = dp(72).toFloat()
                    if (!swipeHandled && abs(dx) >= threshold && abs(dx) > abs(dy) * 1.25f) {
                        swipeHandled = true
                        if (dx < 0) selectRelative(1) else selectRelative(-1)
                    }
                }
            }
            false
        }
    }

    fun bindAdapter(newAdapter: BaseConversationsAdapter) {
        adapter = newAdapter
        applySelectedFilter()
    }

    fun refreshForNewConversations() {
        folders = ConversationFolderManager.getFolders(context)
        normalizeSelection()
        rebuildTabs()
        applySelectedFilter()
    }

    private fun rebuildTabs() {
        tabsContainer.removeAllViews()
        folders.filter { it.enabled }.forEach { folder ->
            addTab(folder.id, folder.name)
        }
        addActionTab("＋") { showCreateFolderDialog() }
        addActionTab("⚙") { showFolderManagerDialog() }
        updateSelection()
    }

    private fun addTab(id: String, title: String) {
        val view = TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
            textSize = 14f
            isSingleLine = true
            setOnClickListener { selectFolder(id) }
        }
        view.tag = id
        tabsContainer.addView(
            view,
            LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        )
    }

    private fun addActionTab(title: String, action: () -> Unit) {
        val view = TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            textSize = if (title == "⚙") 18f else 20f
            isSingleLine = true
            setOnClickListener { action() }
        }
        view.tag = ACTION_ID + title
        tabsContainer.addView(
            view,
            LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        )
    }

    private fun selectFolder(id: String) {
        if (folders.none { it.id == id && it.enabled }) return
        selectedId = id
        ConversationFolderManager.setSelectedFolderId(context, id)
        updateSelection()
        applySelectedFilter()
        scrollSelectedIntoView()
    }

    private fun selectRelative(delta: Int) {
        val visible = folders.filter { it.enabled }
        if (visible.isEmpty()) return
        val currentIndex = visible.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
        val nextIndex = (currentIndex + delta).coerceIn(0, visible.lastIndex)
        if (nextIndex != currentIndex || selectedId != visible[nextIndex].id) {
            selectFolder(visible[nextIndex].id)
        }
    }

    private fun normalizeSelection() {
        val enabled = folders.filter { it.enabled }
        if (enabled.isEmpty()) {
            selectedId = ConversationFolderManager.ALL_ID
            return
        }
        if (enabled.none { it.id == selectedId }) {
            selectedId = enabled.first().id
            ConversationFolderManager.setSelectedFolderId(context, selectedId)
        }
    }

    private fun updateSelection() {
        val primary = context.getProperPrimaryColor()
        for (i in 0 until tabsContainer.childCount) {
            val tab = tabsContainer.getChildAt(i) as TextView
            val selected = tab.tag == selectedId
            tab.setTextColor(if (selected) primary else Color.GRAY)
            tab.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            tab.setBackgroundColor(
                if (selected) withAlpha(primary, 0.10f) else Color.TRANSPARENT
            )
        }
    }

    private fun applySelectedFilter() {
        val currentAdapter = adapter ?: return
        when (selectedId) {
            ConversationFolderManager.ALL_ID -> currentAdapter.clearConversationFilter()
            ConversationFolderManager.UNREAD_ID -> currentAdapter.filterConversations { !it.read }
            ConversationFolderManager.BANKS_ID -> currentAdapter.filterConversations {
                currentAdapter.isBankConversation(it)
            }
            ConversationFolderManager.PERSONAL_ID -> currentAdapter.filterConversations {
                !it.isGroupConversation && !currentAdapter.isBankConversation(it)
            }
            else -> currentAdapter.filterConversations { conversation ->
                ConversationFolderManager.getFolderMembership(context, conversation.threadId)
                    .contains(selectedId)
            }
        }
    }

    private fun scrollSelectedIntoView() {
        post {
            val selected = tabsContainer.findViewWithTag<View>(selectedId) ?: return@post
            smoothScrollTo(selected.left.coerceAtLeast(0), 0)
        }
    }

    private fun showCreateFolderDialog() {
        val nameInput = EditText(context).apply {
            hint = "نام پوشه"
            isSingleLine = true
        }

        AlertDialog.Builder(context)
            .setTitle("پوشه جدید")
            .setView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(8), dp(24), 0)
                addView(nameInput)
            })
            .setNegativeButton("لغو", null)
            .setPositiveButton("ایجاد") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(context, "نام پوشه را وارد کنید", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val current = ConversationFolderManager.getFolders(context)
                current.add(
                    ConversationFolderManager.Folder(
                        id = "custom_${System.currentTimeMillis()}",
                        name = name,
                        enabled = true,
                        system = false,
                    )
                )
                ConversationFolderManager.saveFolders(context, current)
                folders = current
                selectedId = current.last().id
                ConversationFolderManager.setSelectedFolderId(context, selectedId)
                rebuildTabs()
                applySelectedFilter()
                scrollSelectedIntoView()
            }
            .show()
    }

    private fun showFolderManagerDialog() {
        val working = ConversationFolderManager.getFolders(context).map {
            ConversationFolderManager.Folder(it.id, it.name, it.enabled, it.system)
        }.toMutableList()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), 0)
        }

        fun rebuildRows() {
            container.removeAllViews()
            working.forEachIndexed { index, folder ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val check = CheckBox(context).apply {
                    isChecked = folder.enabled
                    text = folder.name
                    isSingleLine = true
                    layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
                    setOnCheckedChangeListener { _, checked -> folder.enabled = checked }
                }
                val up = TextView(context).apply {
                    text = "▲"
                    gravity = Gravity.CENTER
                    setPadding(dp(10), 0, dp(10), 0)
                    isEnabled = index > 0
                    setOnClickListener {
                        if (index > 0) {
                            val moved = working.removeAt(index)
                            working.add(index - 1, moved)
                            rebuildRows()
                        }
                    }
                }
                val down = TextView(context).apply {
                    text = "▼"
                    gravity = Gravity.CENTER
                    setPadding(dp(10), 0, dp(10), 0)
                    isEnabled = index < working.lastIndex
                    setOnClickListener {
                        if (index < working.lastIndex) {
                            val moved = working.removeAt(index)
                            working.add(index + 1, moved)
                            rebuildRows()
                        }
                    }
                }
                row.addView(check)
                row.addView(up)
                row.addView(down)
                container.addView(row)
            }
        }

        rebuildRows()

        AlertDialog.Builder(context)
            .setTitle("مدیریت پوشه‌ها")
            .setMessage("پوشه‌ها را فعال یا غیرفعال کنید و با ▲ ▼ ترتیب نمایش را تغییر دهید.")
            .setView(container)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                ConversationFolderManager.saveFolders(context, working)
                folders = working
                normalizeSelection()
                rebuildTabs()
                applySelectedFilter()
            }
            .show()
    }

    fun showAssignFoldersDialog(threadIds: List<Long>, onSaved: (() -> Unit)? = null) {
        val customFolders = ConversationFolderManager.getFolders(context).filter { !it.system }
        if (customFolders.isEmpty()) {
            Toast.makeText(context, "ابتدا یک پوشه بسازید", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = customFolders.map { it.name }.toTypedArray()
        val checked = customFolders.map { folder ->
            threadIds.all {
                ConversationFolderManager.getFolderMembership(context, it).contains(folder.id)
            }
        }.toBooleanArray()

        AlertDialog.Builder(context)
            .setTitle("قرار دادن در پوشه‌ها")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                threadIds.forEach { threadId ->
                    val memberships = checked.mapIndexedNotNull { index, selected ->
                        if (selected) customFolders[index].id else null
                    }.toSet()
                    ConversationFolderManager.setFolderMembership(context, threadId, memberships)
                }
                refreshForNewConversations()
                onSaved?.invoke()
            }
            .show()
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb(
            (Color.alpha(color) * alpha).roundToInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val PREFS_NAME = "messages_folder_tabs"
        private const val ACTION_ID = "__action__"
        private const val ADAPTER_BIND_RETRY_MS = 100L
        private const val MAX_BIND_ATTEMPTS = 50
    }
}
