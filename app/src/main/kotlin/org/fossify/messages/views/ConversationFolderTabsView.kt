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
        folders.filter { it.enabled }.forEach { folder -> addTab(folder.id, folder.name) }
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
        tabsContainer.addView(view, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
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
        tabsContainer.addView(view, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
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
        if (nextIndex != currentIndex || selectedId != visible[nextIndex].id) selectFolder(visible[nextIndex].id)
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
                !it.isGroupConversation && !currentAdapter.isBankConversation(it)
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
                current.add(ConversationFolderManager.Folder("custom_${System.currentTimeMillis()}", name, true, false))
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
            .setMessage("پوشه‌ها را فعال یا غیرفعال کنید و ترتیب نمایش را تغییر دهید.")
            .setView(container)
            .setNegativeButton("لغو", null)
            .setNeutralButton("قوانین خودکار") { _, _ -> showRulesDialog() }
            .setPositiveButton("ذخیره") { _, _ ->
                ConversationFolderManager.saveFolders(context, working)
                folders = working
                normalizeSelection()
                rebuildTabs()
                applySelectedFilter()
            }
            .show()
    }

    private fun showRulesDialog() {
        val customFolders = ConversationFolderManager.getFolders(context).filter { !it.system }
        if (customFolders.isEmpty()) {
            Toast.makeText(context, "ابتدا حداقل یک پوشه بسازید", Toast.LENGTH_SHORT).show()
            return
        }

        val working = ConversationFolderRuleManager.getRules(context).map {
            ConversationFolderRuleManager.Rule(it.id, it.folderId, it.keywords.toList(), it.mode, it.fields.toSet(), it.enabled)
        }.toMutableList()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), 0)
        }

        fun folderName(folderId: String) = customFolders.firstOrNull { it.id == folderId }?.name ?: "پوشه حذف‌شده"

        fun summary(rule: ConversationFolderRuleManager.Rule): String {
            val modeText = if (rule.mode == ConversationFolderRuleManager.MatchMode.ONE) "یکی از موارد" else "همه موارد"
            val words = rule.keywords.joinToString("، ")
            return "${folderName(rule.folderId)} • $modeText • $words"
        }

        fun rebuildRows() {
            container.removeAllViews()
            val add = TextView(context).apply {
                text = "＋ افزودن قانون جدید"
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(10))
                setTextColor(context.getProperPrimaryColor())
                setOnClickListener {
                    showRuleEditorDialog(null, customFolders) { rule ->
                        working.add(rule)
                        rebuildRows()
                    }
                }
            }
            container.addView(add)

            if (working.isEmpty()) {
                container.addView(TextView(context).apply {
                    text = "هنوز قانون خودکاری تعریف نشده است."
                    setPadding(0, dp(12), 0, dp(12))
                })
            }

            working.forEachIndexed { index, rule ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val enabled = CheckBox(context).apply {
                    isChecked = rule.enabled
                    text = summary(rule)
                    isSingleLine = false
                    layoutParams = LinearLayout.LayoutParams(0, dp(56), 1f)
                    setOnCheckedChangeListener { _, checked -> rule.enabled = checked }
                }
                val edit = TextView(context).apply {
                    text = "✎"
                    gravity = Gravity.CENTER
                    setPadding(dp(8), 0, dp(8), 0)
                    setOnClickListener {
                        showRuleEditorDialog(rule, customFolders) { edited ->
                            working[index] = edited
                            rebuildRows()
                        }
                    }
                }
                val delete = TextView(context).apply {
                    text = "×"
                    gravity = Gravity.CENTER
                    setPadding(dp(8), 0, dp(8), 0)
                    setOnClickListener {
                        working.removeAt(index)
                        rebuildRows()
                    }
                }
                row.addView(enabled)
                row.addView(edit)
                row.addView(delete)
                container.addView(row)
            }
        }
        rebuildRows()

        AlertDialog.Builder(context)
            .setTitle("قوانین مرتب‌سازی")
            .setMessage("پیام‌های جدید با این قوانین بررسی می‌شوند و می‌توانند خودکار وارد چند پوشه شوند.")
            .setView(container)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                ConversationFolderRuleManager.saveRules(context, working)
                adapter?.refreshFolderRules()
                applySelectedFilter()
            }
            .show()
    }

    private fun showRuleEditorDialog(
        existing: ConversationFolderRuleManager.Rule?,
        customFolders: List<ConversationFolderManager.Folder>,
        onSaved: (ConversationFolderRuleManager.Rule) -> Unit,
    ) {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }

        val folderSpinner = Spinner(context)
        val folderNames = customFolders.map { it.name }
        folderSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, folderNames)
        val initialFolder = customFolders.indexOfFirst { it.id == existing?.folderId }.coerceAtLeast(0)
        folderSpinner.setSelection(initialFolder)
        root.addView(TextView(context).apply {
            text = "پوشه مقصد"
            setPadding(0, dp(6), 0, dp(2))
        })
        root.addView(folderSpinner)

        val keywords = EditText(context).apply {
            hint = "هر مورد را در یک خط یا با ویرگول جدا کنید"
            minLines = 3
            gravity = Gravity.TOP
            setText(existing?.keywords?.joinToString("\n") ?: "")
        }
        root.addView(TextView(context).apply {
            text = "موارد جستجو"
            setPadding(0, dp(12), 0, dp(2))
        })
        root.addView(keywords)

        val modeGroup = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL }
        val one = RadioButton(context).apply { text = "حداقل یکی از موارد وجود داشته باشد" }
        val all = RadioButton(context).apply { text = "همه موارد وجود داشته باشند" }
        modeGroup.addView(one)
        modeGroup.addView(all)
        if (existing?.mode == ConversationFolderRuleManager.MatchMode.ALL) all.isChecked = true else one.isChecked = true
        root.addView(TextView(context).apply {
            text = "نحوه تطبیق"
            setPadding(0, dp(12), 0, dp(2))
        })
        root.addView(modeGroup)

        val messageField = CheckBox(context).apply {
            text = "متن آخرین پیام"
            isChecked = existing?.fields?.contains(ConversationFolderRuleManager.MatchField.MESSAGE) ?: true
        }
        val titleField = CheckBox(context).apply {
            text = "نام مخاطب / عنوان مکالمه"
            isChecked = existing?.fields?.contains(ConversationFolderRuleManager.MatchField.TITLE) ?: false
        }
        val phoneField = CheckBox(context).apply {
            text = "شماره فرستنده"
            isChecked = existing?.fields?.contains(ConversationFolderRuleManager.MatchField.PHONE) ?: false
        }
        root.addView(TextView(context).apply {
            text = "در کجا جستجو شود"
            setPadding(0, dp(12), 0, dp(2))
        })
        root.addView(messageField)
        root.addView(titleField)
        root.addView(phoneField)

        AlertDialog.Builder(context)
            .setTitle(if (existing == null) "قانون جدید" else "ویرایش قانون")
            .setView(root)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val words = keywords.text.toString()
                    .split(Regex("[,،\\n]"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                val fields = buildSet {
                    if (messageField.isChecked) add(ConversationFolderRuleManager.MatchField.MESSAGE)
                    if (titleField.isChecked) add(ConversationFolderRuleRuleManager.MatchField.TITLE)
                    if (phoneField.isChecked) add(ConversationFolderRuleManager.MatchField.PHONE)
                }
                if (words.isEmpty()) {
                    Toast.makeText(context, "حداقل یک مورد جستجو وارد کنید", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (fields.isEmpty()) {
                    Toast.makeText(context, "حداقل یک محل جستجو را انتخاب کنید", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val folderId = customFolders[folderSpinner.selectedItemPosition].id
                onSaved(
                    ConversationFolderRuleManager.Rule(
                        id = existing?.id ?: "rule_${System.currentTimeMillis()}",
                        folderId = folderId,
                        keywords = words,
                        mode = if (all.isChecked) ConversationFolderRuleManager.MatchMode.ALL else ConversationFolderRuleManager.MatchMode.ONE,
                        fields = fields,
                        enabled = existing?.enabled ?: true,
                    )
                )
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
            threadIds.all { ConversationFolderManager.getFolderMembership(context, it).contains(folder.id) }
        }.toBooleanArray()
        AlertDialog.Builder(context)
            .setTitle("قرار دادن در پوشه‌ها")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                threadIds.forEach { threadId ->
                    val memberships = checked.mapIndexedNotNull { index, selected -> if (selected) customFolders[index].id else null }.toSet()
                    ConversationFolderManager.setFolderMembership(context, threadId, memberships)
                }
                refreshForNewConversations()
                onSaved?.invoke()
            }
            .show()
    }

    private fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
        (Color.alpha(color) * alpha).roundToInt(),
        Color.red(color), Color.green(color), Color.blue(color)
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val ACTION_ID = "__action__"
        private const val ADAPTER_BIND_RETRY_MS = 100L
        private const val MAX_BIND_ATTEMPTS = 50
    }
}
