package org.fossify.messages.views

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.messages.R
import org.fossify.messages.adapters.BaseConversationsAdapter
import org.fossify.messages.helpers.ConversationFolderManager
import org.fossify.messages.helpers.ConversationFolderRuleManager
import org.fossify.messages.helpers.PersonalConversationClassifier
import kotlin.math.abs
import kotlin.math.roundToInt

/** Messages-native conversation folders and automatic routing rules. */
class ConversationFolderTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : HorizontalScrollView(context, attrs) {
    private val tabsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }
    private val personalClassifier = PersonalConversationClassifier(context)
    private var selectedId = ConversationFolderManager.getSelectedFolderId(context)
    private var adapter: BaseConversationsAdapter? = null
    private var folders = mutableListOf<ConversationFolderManager.Folder>()
    private var bindAttempts = 0

    init {
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setBackgroundColor(context.getProperBackgroundColor())
        addView(tabsContainer, LayoutParams(LayoutParams.WRAP_CONTENT, dp(48)))
        folders = ConversationFolderManager.getFolders(context)
        normalizeSelection()
        rebuildTabs()
        personalClassifier.ensureLoaded {
            if (selectedId == ConversationFolderManager.PERSONAL_ID) applySelectedFilter()
        }
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

    /**
     * Use RecyclerView.OnItemTouchListener rather than setOnTouchListener.
     * RecyclerView can intercept the gesture itself; OnItemTouchListener is invoked
     * before RecyclerView's own scrolling and can safely take over horizontal swipes.
     */
    private fun attachConversationSwipe() {
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.conversations_list) ?: run {
            postDelayed({ attachConversationSwipe() }, ADAPTER_BIND_RETRY_MS)
            return
        }
        if (recyclerView.getTag(SWIPE_LISTENER_TAG) == true) return
        var downX = 0f
        var downY = 0f
        var claimed = false
        val listener = object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        claimed = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (claimed) return true
                        val dx = event.x - downX
                        val dy = event.y - downY
                        val threshold = dp(48).toFloat()
                        if (abs(dx) >= threshold && abs(dx) > abs(dy) * 1.2f) {
                            claimed = true
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            selectRelative(if (dx < 0) 1 else -1)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> claimed = false
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    claimed = false
                }
            }
        }
        recyclerView.addOnItemTouchListener(listener)
        recyclerView.setTag(SWIPE_LISTENER_TAG, true)
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
        folders.filter { it.enabled }.forEach { folder -> addTab(folder.id, folder.name) }
        addActionTab("＋") { showCreateFolderDialog() }
        addActionTab("⚙") { showFolderManagerDialog() }
        updateSelection()
    }

    private fun addTab(id: String, title: String) {
        tabsContainer.addView(TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
            textSize = 14f
            isSingleLine = true
            tag = id
            setOnClickListener { selectFolder(id) }
        }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    private fun addActionTab(title: String, action: () -> Unit) {
        tabsContainer.addView(TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            textSize = if (title == "⚙") 18f else 20f
            isSingleLine = true
            tag = ACTION_ID + title
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
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
        if (visible.size < 2) return
        val index = visible.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        val next = (index + delta).let { if (it < 0) visible.lastIndex else if (it > visible.lastIndex) 0 else it }
        if (next != index) selectFolder(visible[next].id)
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
            tab.setBackgroundColor(if (selected) withAlpha(primary, 0.10f) else Color.TRANSPARENT)
        }
    }

    private fun applySelectedFilter() {
        val currentAdapter = adapter ?: return
        when (selectedId) {
            ConversationFolderManager.ALL_ID -> currentAdapter.clearConversationFilter()
            ConversationFolderManager.UNREAD_ID -> currentAdapter.filterConversations { !it.read }
            ConversationFolderManager.BANKS_ID -> currentAdapter.filterConversations { currentAdapter.isBankConversation(it) }
            ConversationFolderManager.PERSONAL_ID -> currentAdapter.filterConversations {
                personalClassifier.isPersonal(it, currentAdapter.isBankConversation(it))
            }
            else -> currentAdapter.filterConversations { conversation ->
                ConversationFolderManager.getFolderMembership(context, conversation.threadId).contains(selectedId)
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
        val input = EditText(context).apply { hint = "نام پوشه"; isSingleLine = true }
        AlertDialog.Builder(context)
            .setTitle("پوشه جدید")
            .setView(input)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ایجاد") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val current = ConversationFolderManager.getFolders(context)
                val folder = ConversationFolderManager.Folder("custom_${System.currentTimeMillis()}", name, true, false)
                current.add(folder)
                ConversationFolderManager.saveFolders(context, current)
                folders = current
                selectFolder(folder.id)
                rebuildTabs()
            }.show()
    }

    private fun showFolderManagerDialog() {
        val working = ConversationFolderManager.getFolders(context).map {
            ConversationFolderManager.Folder(it.id, it.name, it.enabled, it.system)
        }.toMutableList()
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(4), dp(16), 0) }

        fun rebuild() {
            container.removeAllViews()
            working.forEachIndexed { index, folder ->
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                row.addView(CheckBox(context).apply {
                    text = folder.name
                    isChecked = folder.enabled
                    layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
                    setOnCheckedChangeListener { _, value -> folder.enabled = value }
                })
                row.addView(TextView(context).apply {
                    text = "▲"; gravity = Gravity.CENTER; setPadding(dp(10), 0, dp(10), 0); isEnabled = index > 0
                    setOnClickListener { if (index > 0) { working.add(index - 1, working.removeAt(index)); rebuild() } }
                })
                row.addView(TextView(context).apply {
                    text = "▼"; gravity = Gravity.CENTER; setPadding(dp(10), 0, dp(10), 0); isEnabled = index < working.lastIndex
                    setOnClickListener { if (index < working.lastIndex) { working.add(index + 1, working.removeAt(index)); rebuild() } }
                })
                container.addView(row)
            }
        }
        rebuild()
        AlertDialog.Builder(context)
            .setTitle("مدیریت پوشه‌ها")
            .setMessage("فعال‌سازی و ترتیب پوشه‌ها")
            .setView(container)
            .setNegativeButton("لغو", null)
            .setNeutralButton("قوانین خودکار") { _, _ -> showRulesDialog() }
            .setPositiveButton("ذخیره") { _, _ ->
                ConversationFolderManager.saveFolders(context, working)
                folders = working
                normalizeSelection()
                rebuildTabs()
                applySelectedFilter()
            }.show()
    }

    private fun showRulesDialog() {
        val custom = ConversationFolderManager.getFolders(context).filter { !it.system }
        if (custom.isEmpty()) {
            Toast.makeText(context, "ابتدا یک پوشه بسازید", Toast.LENGTH_SHORT).show()
            return
        }
        val working = ConversationFolderRuleManager.getRules(context).map {
            ConversationFolderRuleManager.Rule(it.id, it.folderId, it.keywords.toList(), it.mode, it.fields.toSet(), it.enabled)
        }.toMutableList()
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), 0) }

        fun rebuild() {
            container.removeAllViews()
            container.addView(TextView(context).apply {
                text = "＋ افزودن قانون"
                setPadding(0, dp(10), 0, dp(10))
                setTextColor(context.getProperPrimaryColor())
                setOnClickListener { showRuleEditor(null, custom) { working.add(it); rebuild() } }
            })
            working.forEachIndexed { index, rule ->
                val folderName = custom.firstOrNull { it.id == rule.folderId }?.name ?: "پوشه حذف‌شده"
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                row.addView(CheckBox(context).apply {
                    text = "$folderName • ${rule.keywords.joinToString("، ")}"; isChecked = rule.enabled
                    layoutParams = LinearLayout.LayoutParams(0, dp(56), 1f)
                    setOnCheckedChangeListener { _, value -> rule.enabled = value }
                })
                row.addView(TextView(context).apply { text = "✎"; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { showRuleEditor(rule, custom) { working[index] = it; rebuild() } } })
                row.addView(TextView(context).apply { text = "×"; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { working.removeAt(index); rebuild() } })
                container.addView(row)
            }
        }
        rebuild()
        AlertDialog.Builder(context)
            .setTitle("قوانین مرتب‌سازی")
            .setMessage("هر قانون می‌تواند چند کلمه کلیدی داشته باشد و با یکی یا همه موارد تطبیق داده شود.")
            .setView(container)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                ConversationFolderRuleManager.saveRules(context, working)
                adapter?.refreshFolderRules()
                applySelectedFilter()
            }.show()
    }

    private fun showRuleEditor(
        existing: ConversationFolderRuleManager.Rule?,
        folders: List<ConversationFolderManager.Folder>,
        onSaved: (ConversationFolderRuleManager.Rule) -> Unit,
    ) {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, folders.map { it.name })
            setSelection(folders.indexOfFirst { it.id == existing?.folderId }.coerceAtLeast(0))
        }
        root.addView(TextView(context).apply { text = "پوشه مقصد" }); root.addView(spinner)
        val keywords = EditText(context).apply { hint = "کلمات کلیدی؛ با ویرگول یا خط جدید جدا کنید"; minLines = 3; gravity = Gravity.TOP; setText(existing?.keywords?.joinToString("\n") ?: "") }
        root.addView(TextView(context).apply { text = "کلمات کلیدی"; setPadding(0, dp(10), 0, 0) }); root.addView(keywords)
        val mode = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL }
        val one = RadioButton(context).apply { text = "حداقل یکی از کلمات" }
        val all = RadioButton(context).apply { text = "همه کلمات" }
        mode.addView(one); mode.addView(all); if (existing?.mode == ConversationFolderRuleManager.MatchMode.ALL) all.isChecked = true else one.isChecked = true
        root.addView(mode)
        val message = CheckBox(context).apply { text = "متن آخرین پیام"; isChecked = existing?.fields?.contains(ConversationFolderRuleManager.MatchField.MESSAGE) ?: true }
        val title = CheckBox(context).apply { text = "نام مخاطب / عنوان مکالمه"; isChecked = existing?.fields?.contains(ConversationFolderRuleManager.MatchField.TITLE) ?: false }
        val phone = CheckBox(context).apply { text = "شماره فرستنده"; isChecked = existing?.fields?.contains(ConversationFolderRuleManager.MatchField.PHONE) ?: false }
        root.addView(message); root.addView(title); root.addView(phone)
        AlertDialog.Builder(context)
            .setTitle(if (existing == null) "قانون جدید" else "ویرایش قانون")
            .setView(root)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val words = keywords.text.toString().split(Regex("[,،\\n]")).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                val fields = buildSet {
                    if (message.isChecked) add(ConversationFolderRuleManager.MatchField.MESSAGE)
                    if (title.isChecked) add(ConversationFolderRuleManager.MatchField.TITLE)
                    if (phone.isChecked) add(ConversationFolderRuleManager.MatchField.PHONE)
                }
                if (words.isEmpty() || fields.isEmpty()) return@setPositiveButton
                onSaved(ConversationFolderRuleManager.Rule(
                    id = existing?.id ?: "rule_${System.currentTimeMillis()}",
                    folderId = folders[spinner.selectedItemPosition].id,
                    keywords = words,
                    mode = if (all.isChecked) ConversationFolderRuleManager.MatchMode.ALL else ConversationFolderRuleManager.MatchMode.ONE,
                    fields = fields,
                    enabled = existing?.enabled ?: true,
                ))
            }.show()
    }

    fun showAssignFoldersDialog(threadIds: List<Long>, onSaved: (() -> Unit)? = null) {
        val custom = ConversationFolderManager.getFolders(context).filter { !it.system }
        if (custom.isEmpty()) {
            Toast.makeText(context, "ابتدا یک پوشه بسازید", Toast.LENGTH_SHORT).show()
            return
        }
        val checked = custom.map { folder -> threadIds.all { ConversationFolderManager.getFolderMembership(context, it).contains(folder.id) } }.toBooleanArray()
        AlertDialog.Builder(context)
            .setTitle("قرار دادن در پوشه‌ها")
            .setMultiChoiceItems(custom.map { it.name }.toTypedArray(), checked) { _, which, value -> checked[which] = value }
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val managedIds = custom.map { it.id }.toSet()
                val selected = checked.mapIndexedNotNull { index, value -> if (value) custom[index].id else null }.toSet()
                threadIds.forEach { ConversationFolderManager.setFolderMembership(context, it, selected, managedIds) }
                refreshForNewConversations()
                onSaved?.invoke()
            }.show()
    }

    private fun withAlpha(color: Int, alpha: Float): Int = Color.argb((Color.alpha(color) * alpha).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val ACTION_ID = "__action__"
        private const val SWIPE_LISTENER_TAG = 0x53495045
        private const val ADAPTER_BIND_RETRY_MS = 100L
        private const val MAX_BIND_ATTEMPTS = 50
    }
}
