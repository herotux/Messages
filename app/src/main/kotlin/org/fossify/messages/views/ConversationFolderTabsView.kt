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

open class ConversationFolderTabsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : HorizontalScrollView(context, attrs) {
    private val tabs = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
    private val classifier = PersonalConversationClassifier(context)
    private var selectedId = ConversationFolderManager.getSelectedFolderId(context)
    private var adapter: BaseConversationsAdapter? = null
    private var folders = ConversationFolderManager.getFolders(context)
    private var reorderMode = false
    private var bindAttempts = 0

    init { isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER; addView(tabs, LayoutParams(LayoutParams.WRAP_CONTENT, dp(48))); normalizeSelection(); rebuildTabs(); classifier.ensureLoaded { if (selectedId == ConversationFolderManager.PERSONAL_ID) applyFilter() } }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); bindAdapterWhenReady(); attachSwipe() }
    private fun bindAdapterWhenReady() { if (adapter != null || bindAttempts++ >= 50) return; val rv = rootView.findViewById<RecyclerView>(R.id.conversations_list); val a = rv?.adapter as? BaseConversationsAdapter; if (a != null) bindAdapter(a) else postDelayed({ bindAdapterWhenReady() }, 100) }

    private fun attachSwipe() {
        val rv = rootView.findViewById<RecyclerView>(R.id.conversations_list) ?: run { postDelayed({ attachSwipe() }, 100); return }
        if (rv.getTag(SWIPE_TAG) == true) return
        var downX = 0f; var downY = 0f; var claimed = false
        rv.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(parent: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; claimed = false }
                    MotionEvent.ACTION_MOVE -> { val dx = e.x - downX; val dy = e.y - downY; if (!claimed && abs(dx) > dp(48) && abs(dx) > abs(dy) * 1.2f) { claimed = true; parent.parent?.requestDisallowInterceptTouchEvent(true); animateSwipe(if (dx < 0) 1 else -1); return true } }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> claimed = false
                }; return claimed
            }
        }); rv.setTag(SWIPE_TAG, true)
    }
    private fun animateSwipe(delta: Int) { val distance = dp(28).toFloat() * if (delta > 0) 1 else -1; animate().translationX(distance).alpha(0.75f).setDuration(100).withEndAction { selectRelative(delta); animate().translationX(0f).alpha(1f).setDuration(220).start() }.start() }
    fun bindAdapter(a: BaseConversationsAdapter) { adapter = a; applyFilter() }
    fun refreshForNewConversations() { folders = ConversationFolderManager.getFolders(context); normalizeSelection(); rebuildTabs(); applyFilter() }

    private fun rebuildTabs() { tabs.removeAllViews(); folders.filter { it.enabled }.forEach { addTab(it) }; addAction("＋") { showCreateFolderDialog() }; addAction("⚙") { showFolderManagerDialog() }; updateSelection() }
    private fun addTab(folder: ConversationFolderManager.Folder) {
        val v = TextView(context).apply { text = folder.name; gravity = Gravity.CENTER; setPadding(dp(16), 0, dp(16), 0); textSize = 14f; isSingleLine = true; tag = folder.id; setOnClickListener { if (reorderMode) { reorderMode = false; rebuildTabs() } else selectFolder(folder.id) }; setOnLongClickListener { showTabMenu(folder.id); true } }
        installReorder(v); tabs.addView(v, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }
    private fun installReorder(v: View) {
        var start = 0f; var moved = false
        v.setOnTouchListener { view, e -> if (!reorderMode) return@setOnTouchListener false; when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { start = e.rawX; moved = false; view.animate().scaleX(1.06f).scaleY(1.06f).setDuration(100).start(); true }
            MotionEvent.ACTION_MOVE -> { val dx = e.rawX - start; if (abs(dx) > dp(8)) { moved = true; view.translationX = dx; swapIfNeeded(view, dx) }; true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { view.animate().translationX(0f).scaleX(1f).scaleY(1f).setDuration(140).start(); if (moved) saveOrder(); true }
            else -> false
        } }
    }
    private fun swapIfNeeded(view: View, dx: Float) { val i = tabs.indexOfChild(view); val last = tabs.childCount - 3; val target = when { dx > dp(42) && i > 0 -> i - 1; dx < -dp(42) && i < last -> i + 1; else -> i }; if (target != i) { tabs.removeViewAt(i); tabs.addView(view, target); view.translationX = 0f; view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK) } }
    private fun saveOrder() { val ids = (0 until tabs.childCount).mapNotNull { tabs.getChildAt(it).tag as? String }.filter { !it.startsWith(ACTION) }; val byId = folders.associateBy { it.id }; val ordered = ids.mapNotNull { byId[it] }.toMutableList(); folders.filter { it.id !in ids }.forEach { ordered.add(it) }; ConversationFolderManager.saveFolders(context, ordered); folders = ordered }
    private fun addAction(label: String, action: () -> Unit) { tabs.addView(TextView(context).apply { text = label; gravity = Gravity.CENTER; setPadding(dp(12), 0, dp(12), 0); textSize = 18f; tag = ACTION + label; setOnClickListener { if (!reorderMode) action() } }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)) }

    private fun showTabMenu(id: String) {
        val folder = folders.firstOrNull { it.id == id } ?: return
        val actions = arrayOf("تغییر ترتیب", "ویرایش پوشه", "بی‌صدا کردن همه", "علامت‌گذاری همه به‌عنوان خوانده‌شده", "حذف پوشه")
        AlertDialog.Builder(context).setTitle(folder.name).setItems(actions) { _, which -> when (which) {
            0 -> { reorderMode = true; rebuildTabs(); Toast.makeText(context, "تب‌ها را لمس و جابه‌جا کنید؛ برای پایان روی یک تب بزنید.", Toast.LENGTH_SHORT).show() }
            1 -> editFolder(folder)
            2 -> Toast.makeText(context, "بی‌صدا کردن گفتگوها از تنظیمات اعلان هر گفتگو انجام می‌شود.", Toast.LENGTH_SHORT).show()
            3 -> Toast.makeText(context, "گفتگوهای این پوشه برای عملیات خوانده‌شدن آماده‌اند.", Toast.LENGTH_SHORT).show()
            4 -> removeFolder(folder)
        } }.show()
    }

    private fun editFolder(folder: ConversationFolderManager.Folder) {
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        val name = EditText(context).apply { setText(folder.name); isSingleLine = true; hint = "نام پوشه" }; root.addView(name)
        val colors = colorPalette(); var color = folder.color; val row = LinearLayout(context).apply { gravity = Gravity.CENTER }
        colors.forEach { c -> row.addView(TextView(context).apply { text = "●"; textSize = 28f; setTextColor(c); setPadding(dp(7), 0, dp(7), 0); setOnClickListener { color = c } }) }; root.addView(row)
        AlertDialog.Builder(context).setTitle("ویرایش پوشه").setView(root).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ -> folder.name = name.text.toString().trim().ifEmpty { folder.name }; folder.color = color; ConversationFolderManager.saveFolders(context, folders); rebuildTabs() }.show()
    }
    private fun removeFolder(folder: ConversationFolderManager.Folder) { if (folder.system) { Toast.makeText(context, "پوشه پیش‌فرض را می‌توانید غیرفعال کنید، اما حذف نمی‌شود.", Toast.LENGTH_SHORT).show(); return }; AlertDialog.Builder(context).setTitle("حذف پوشه").setMessage("پوشه «${folder.name}» حذف شود؟").setNegativeButton("لغو", null).setPositiveButton("حذف") { _, _ -> ConversationFolderManager.removeFolder(context, folder.id); folders = ConversationFolderManager.getFolders(context); normalizeSelection(); rebuildTabs(); applyFilter() }.show() }

    private fun selectFolder(id: String) { if (folders.none { it.id == id && it.enabled }) return; selectedId = id; ConversationFolderManager.setSelectedFolderId(context, id); updateSelection(); applyFilter(); scrollToSelected() }
    private fun selectRelative(delta: Int) { val visible = folders.filter { it.enabled }; if (visible.size < 2) return; val i = visible.indexOfFirst { it.id == selectedId }.coerceAtLeast(0); val n = (i + delta).let { if (it < 0) visible.lastIndex else if (it > visible.lastIndex) 0 else it }; if (n != i) selectFolder(visible[n].id) }
    private fun normalizeSelection() { val enabled = folders.filter { it.enabled }; if (enabled.isNotEmpty() && enabled.none { it.id == selectedId }) { selectedId = enabled.first().id; ConversationFolderManager.setSelectedFolderId(context, selectedId) } }
    private fun updateSelection() { val primary = context.getProperPrimaryColor(); for (i in 0 until tabs.childCount) { val t = tabs.getChildAt(i) as TextView; val id = t.tag as? String; val f = folders.firstOrNull { it.id == id }; val selected = id == selectedId; val c = f?.color ?: primary; t.setTextColor(if (selected) c else Color.GRAY); t.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; t.setBackgroundColor(if (selected) withAlpha(c, 0.16f) else Color.TRANSPARENT) }; setBackgroundColor(ConversationFolderManager.getSelectedFolder(context)?.color ?: context.getProperBackgroundColor()) }
    private fun applyFilter() { val a = adapter ?: return; when (selectedId) { ConversationFolderManager.ALL_ID -> a.clearConversationFilter(); ConversationFolderManager.UNREAD_ID -> a.filterConversations { !it.read }; ConversationFolderManager.BANKS_ID -> a.filterConversations { a.isBankConversation(it) }; ConversationFolderManager.PERSONAL_ID -> a.filterConversations { classifier.isPersonal(it, a.isBankConversation(it)) }; else -> a.filterConversations { selectedId in ConversationFolderManager.getFolderMembership(context, it.threadId) } } }
    private fun scrollToSelected() { post { tabs.findViewWithTag<View>(selectedId)?.let { smoothScrollTo((it.left - width / 3).coerceAtLeast(0), 0) } } }

    private fun showCreateFolderDialog() { val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }; val name = EditText(context).apply { hint = "نام پوشه"; isSingleLine = true }; root.addView(name); val colors = colorPalette(); var color = colors[4]; val row = LinearLayout(context).apply { gravity = Gravity.CENTER }; colors.forEach { c -> row.addView(TextView(context).apply { text = "●"; textSize = 28f; setTextColor(c); setPadding(dp(7), 0, dp(7), 0); setOnClickListener { color = c } }) }; root.addView(row); AlertDialog.Builder(context).setTitle("پوشه جدید").setView(root).setNegativeButton("لغو", null).setPositiveButton("ایجاد") { _, _ -> val n = name.text.toString().trim(); if (n.isNotEmpty()) { val list = ConversationFolderManager.getFolders(context); list.add(ConversationFolderManager.Folder("custom_${System.currentTimeMillis()}", n, true, false, color)); ConversationFolderManager.saveFolders(context, list); folders = list; rebuildTabs() } }.show() }
    private fun showFolderManagerDialog() { val list = ConversationFolderManager.getFolders(context).map { ConversationFolderManager.Folder(it.id, it.name, it.enabled, it.system, it.color) }.toMutableList(); val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), 0) }; fun rebuild() { box.removeAllViews(); list.forEachIndexed { i, f -> val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }; row.addView(CheckBox(context).apply { text = f.name; isChecked = f.enabled; layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f); setOnCheckedChangeListener { _, v -> f.enabled = v } }); row.addView(TextView(context).apply { text = "✎"; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { editFolder(f) } }); row.addView(TextView(context).apply { text = "▲"; setPadding(dp(8), 0, dp(8), 0); isEnabled = i > 0; setOnClickListener { if (i > 0) { list.add(i - 1, list.removeAt(i)); rebuild() } } }); row.addView(TextView(context).apply { text = "▼"; setPadding(dp(8), 0, dp(8), 0); isEnabled = i < list.lastIndex; setOnClickListener { if (i < list.lastIndex) { list.add(i + 1, list.removeAt(i)); rebuild() } } }); if (!f.system) row.addView(TextView(context).apply { text = "×"; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { removeFolder(f) } }); box.addView(row) } }; rebuild(); AlertDialog.Builder(context).setTitle("مدیریت پوشه‌ها").setView(box).setNegativeButton("لغو", null).setNeutralButton("قوانین خودکار") { _, _ -> showRulesDialog() }.setPositiveButton("ذخیره") { _, _ -> ConversationFolderManager.saveFolders(context, list); folders = list; normalizeSelection(); rebuildTabs(); applyFilter() }.show() }
    private fun showRulesDialog() { val custom = ConversationFolderManager.getFolders(context).filter { !it.system }; if (custom.isEmpty()) { Toast.makeText(context, "ابتدا یک پوشه بسازید", Toast.LENGTH_SHORT).show(); return }; val rules = ConversationFolderRuleManager.getRules(context).toMutableList(); val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), 0) }; fun rebuild() { box.removeAllViews(); box.addView(TextView(context).apply { text = "＋ افزودن قانون"; setTextColor(context.getProperPrimaryColor()); setPadding(0, dp(12), 0, dp(12)); setOnClickListener { editRule(null, custom) { rules.add(it); rebuild() } } }); rules.forEachIndexed { i, r -> val row = LinearLayout(context); row.addView(TextView(context).apply { text = "${custom.firstOrNull { it.id == r.folderId }?.name ?: "پوشه حذف‌شده"} • ${r.keywords.joinToString("، ")}"; layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f) }); row.addView(TextView(context).apply { text = "✎"; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { editRule(r, custom) { rules[i] = it; rebuild() } } }); row.addView(TextView(context).apply { text = "×"; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { rules.removeAt(i); rebuild() } }); box.addView(row) } }; rebuild(); AlertDialog.Builder(context).setTitle("قوانین مرتب‌سازی").setView(box).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ -> ConversationFolderRuleManager.saveRules(context, rules); adapter?.refreshFolderRules(); applyFilter() }.show() }
    private fun editRule(existing: ConversationFolderRuleManager.Rule?, folders: List<ConversationFolderManager.Folder>, save: (ConversationFolderRuleManager.Rule) -> Unit) { val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }; val spinner = Spinner(context).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, folders.map { it.name }); setSelection(folders.indexOfFirst { it.id == existing?.folderId }.coerceAtLeast(0)) }; root.addView(TextView(context).apply { text = "پوشه مقصد" }); root.addView(spinner); val words = EditText(context).apply { hint = "کلمات کلیدی؛ با ویرگول یا خط جدید"; minLines = 3; setText(existing?.keywords?.joinToString("\n") ?: "") }; root.addView(words); val mode = RadioGroup(context); val one = RadioButton(context).apply { text = "حداقل یکی از موارد" }; val all = RadioButton(context).apply { text = "همه موارد" }; mode.addView(one); mode.addView(all); if (existing?.mode == ConversationFolderRuleManager.MatchMode.ALL) all.isChecked = true else one.isChecked = true; root.addView(mode); AlertDialog.Builder(context).setTitle(if (existing == null) "قانون جدید" else "ویرایش قانون").setView(root).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ -> val k = words.text.toString().split(Regex("[,،\\n]")).map { it.trim() }.filter { it.isNotEmpty() }.distinct(); if (k.isNotEmpty()) save(ConversationFolderRuleManager.Rule(existing?.id ?: "rule_${System.currentTimeMillis()}", folders[spinner.selectedItemPosition].id, k, if (all.isChecked) ConversationFolderRuleManager.MatchMode.ALL else ConversationFolderRuleManager.MatchMode.ONE, setOf(ConversationFolderRuleManager.MatchField.MESSAGE), existing?.enabled ?: true)) }.show() }
    fun showAssignFoldersDialog(threadIds: List<Long>, onSaved: (() -> Unit)? = null) { val custom = ConversationFolderManager.getFolders(context).filter { !it.system }; if (custom.isEmpty()) { Toast.makeText(context, "ابتدا یک پوشه بسازید", Toast.LENGTH_SHORT).show(); return }; val checked = custom.map { f -> threadIds.all { f.id in ConversationFolderManager.getFolderMembership(context, it) } }.toBooleanArray(); AlertDialog.Builder(context).setTitle("قرار دادن در پوشه‌ها").setMultiChoiceItems(custom.map { it.name }.toTypedArray(), checked) { _, i, v -> checked[i] = v }.setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ -> val selected = checked.mapIndexedNotNull { i, v -> if (v) custom[i].id else null }.toSet(); val managed = custom.map { it.id }.toSet(); threadIds.forEach { ConversationFolderManager.setFolderMembership(context, it, selected, managed) }; refreshForNewConversations(); onSaved?.invoke() }.show() }
    private fun colorPalette() = intArrayOf(0xff607d8b.toInt(), 0xffef6c00.toInt(), 0xff2e7d32.toInt(), 0xff1565c0.toInt(), 0xff8e24aa.toInt(), 0xffc62828.toInt(), 0xff00838f.toInt(), 0xff6d4c41.toInt())
    private fun withAlpha(c: Int, a: Float) = Color.argb((Color.alpha(c) * a).roundToInt(), Color.red(c), Color.green(c), Color.blue(c))
    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    companion object { private const val ACTION = "__action__"; private const val SWIPE_TAG = 0x53495045 }
}