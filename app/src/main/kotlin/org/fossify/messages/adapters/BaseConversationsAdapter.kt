package org.fossify.messages.adapters

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Parcelable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.fossify.commons.adapters.MyRecyclerViewListAdapter
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.formatDateOrTime
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getTextSize
import org.fossify.commons.extensions.setupViewBackground
import org.fossify.commons.helpers.FontHelper
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.databinding.ItemConversationBinding
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.getAllDrafts
import org.fossify.messages.helpers.BankConversationVerificationStore
import org.fossify.messages.helpers.BankSmsDetector
import org.fossify.messages.helpers.IranianBankLogoImageHelper
import org.fossify.messages.helpers.IranianBankLogoResolver
import org.fossify.messages.helpers.IranianSenderIconResolver
import org.fossify.messages.helpers.PersianDateHelper
import org.fossify.messages.models.Conversation

@Suppress("LeakingThis")
abstract class BaseConversationsAdapter(
    activity: SimpleActivity,
    recyclerView: MyRecyclerView,
    onRefresh: () -> Unit,
    itemClick: (Any) -> Unit,
) : MyRecyclerViewListAdapter<Conversation>(activity, recyclerView, ConversationDiffCallback(), itemClick, onRefresh), RecyclerViewFastScroller.OnPopupTextUpdate {
    private var fontSize = activity.getTextSize()
    private var drafts = HashMap<Long, String>()
    private var recyclerViewState: Parcelable? = null

    init { setupDragListener(true); setHasStableIds(true); updateDrafts(); registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = restoreRecyclerViewState()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = restoreRecyclerViewState()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = restoreRecyclerViewState()
    }) }
    @SuppressLint("NotifyDataSetChanged") fun updateFontSize() { fontSize = activity.getTextSize(); notifyDataSetChanged() }
    fun updateConversations(newConversations: ArrayList<Conversation>, commitCallback: (() -> Unit)? = null) { saveRecyclerViewState(); submitList(newConversations.toList(), commitCallback) }
    @SuppressLint("NotifyDataSetChanged") fun updateDrafts() { ensureBackgroundThread { val newDrafts = HashMap<Long, String>(); fetchDrafts(newDrafts); activity.runOnUiThread { if (drafts.hashCode() != newDrafts.hashCode()) { drafts = newDrafts; notifyDataSetChanged() } } } }
    override fun getSelectableItemCount() = itemCount
    protected fun getSelectedItems() = currentList.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<Conversation>
    override fun getIsItemSelectable(position: Int) = true
    override fun getItemSelectionKey(position: Int) = currentList.getOrNull(position)?.hashCode()
    override fun getItemKeyPosition(key: Int) = currentList.indexOfFirst { it.hashCode() == key }
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = createViewHolder(ItemConversationBinding.inflate(layoutInflater, parent, false).root)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) { val conversation = getItem(position); holder.bindView(conversation, allowSingleClick = true, allowLongClick = true) { itemView, _ -> setupView(itemView, conversation) }; bindViewHolder(holder) }
    override fun getItemId(position: Int) = getItem(position).threadId
    override fun onViewRecycled(holder: ViewHolder) { super.onViewRecycled(holder); if (!activity.isDestroyed && !activity.isFinishing) Glide.with(activity).clear(ItemConversationBinding.bind(holder.itemView).conversationImage) }
    private fun fetchDrafts(drafts: HashMap<Long, String>) { drafts.clear(); for ((threadId, draft) in activity.getAllDrafts()) drafts[threadId] = draft }

    private fun setupView(view: View, conversation: Conversation) {
        ItemConversationBinding.bind(view).apply {
            root.setupViewBackground(activity)
            val smsDraft = drafts[conversation.threadId]
            draftIndicator.beVisibleIf(!smsDraft.isNullOrEmpty()); draftIndicator.setTextColor(properPrimaryColor)
            pinIndicator.beVisibleIf(activity.config.pinnedConversations.contains(conversation.threadId.toString())); pinIndicator.applyColorFilter(textColor)
            conversationFrame.isSelected = selectedKeys.contains(conversation.hashCode())
            conversationAddress.text = conversation.title; conversationAddress.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            conversationBodyShort.text = smsDraft ?: conversation.snippet; conversationBodyShort.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            conversationDate.text = if (activity.config.usePersianCalendar) PersianDateHelper.toPersianDigits(PersianDateHelper.formatMonthName(conversation.date * 1000L)) else (conversation.date * 1000L).formatDateOrTime(context = context, hideTimeOnOtherDays = true, showCurrentYear = false)
            conversationDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            val isUnread = !conversation.read
            conversationBodyShort.alpha = if (isUnread) 1f else 0.7f
            val style = if (isUnread) { if (conversation.isScheduled) Typeface.BOLD_ITALIC else Typeface.BOLD } else { if (conversation.isScheduled) Typeface.ITALIC else Typeface.NORMAL }
            val customTypeface = FontHelper.getTypeface(activity); conversationAddress.setTypeface(customTypeface, style); conversationBodyShort.setTypeface(customTypeface, style); conversationDate.setTypeface(customTypeface, style)
            arrayListOf(conversationAddress, conversationBodyShort, conversationDate).forEach { it.setTextColor(textColor) }
            setupBadgeCount(unreadCountBadge, isUnread, conversation.unreadCount)

            val placeholder = if (conversation.isGroupConversation) SimpleContactsHelper(activity).getColoredGroupIcon(conversation.title) else null
            val confirmedBank = if (!conversation.isGroupConversation) BankConversationVerificationStore.getConfirmedBank(activity, conversation.threadId) else null
            val bankDetection = if (!conversation.isGroupConversation && confirmedBank == null) BankSmsDetector.detect(conversation.phoneNumber, conversation.snippet) else null
            val bank = confirmedBank ?: bankDetection?.bank
            val bankLogoRes = bank?.let { IranianBankLogoResolver.resolve(activity, it) }
            val senderLogoRes = if (!conversation.isGroupConversation && bankLogoRes == null) IranianSenderIconResolver.resolve(activity, conversation.phoneNumber) else null
            val logoRes = bankLogoRes ?: senderLogoRes
            if (logoRes != null) {
                Glide.with(activity).clear(conversationImage)
                if (!IranianBankLogoImageHelper.setBankLogo(conversationImage, logoRes)) SimpleContactsHelper(activity).loadContactImage(conversation.photoUri, conversationImage, conversation.title, placeholderImage = placeholder)
            } else SimpleContactsHelper(activity).loadContactImage(conversation.photoUri, conversationImage, conversation.title, placeholderImage = placeholder)
        }
    }
    private fun setupBadgeCount(view: TextView, isUnread: Boolean, count: Int) { view.apply { beVisibleIf(isUnread); if (isUnread) { text = when { count > MAX_UNREAD_BADGE_COUNT -> "$MAX_UNREAD_BADGE_COUNT+"; count == 0 -> ""; else -> count.toString() }; setTextColor(properPrimaryColor.getContrastColor()); background?.applyColorFilter(properPrimaryColor) } } }
    override fun onChange(position: Int) = currentList.getOrNull(position)?.title ?: ""
    private fun saveRecyclerViewState() { recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState() }
    private fun restoreRecyclerViewState() { recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState) }
    private class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() { override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean = Conversation.areItemsTheSame(oldItem, newItem); override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean = Conversation.areContentsTheSame(oldItem, newItem) }
    companion object { private const val MAX_UNREAD_BADGE_COUNT = 99 }
}
