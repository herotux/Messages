package org.fossify.messages.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import org.fossify.commons.adapters.MyRecyclerViewListAdapter
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.formatDateOrTime
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getTextSize
import org.fossify.commons.extensions.getTimeFormat
import org.fossify.commons.extensions.shareTextIntent
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.usableScreenSize
import org.fossify.commons.helpers.FontHelper
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.activities.NewConversationActivity
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.activities.ThreadActivity
import org.fossify.messages.activities.VCardViewerActivity
import org.fossify.messages.databinding.ItemAttachmentDocumentBinding
import org.fossify.messages.databinding.ItemAttachmentImageBinding
import org.fossify.messages.databinding.ItemAttachmentVcardBinding
import org.fossify.messages.databinding.ItemMessageBinding
import org.fossify.messages.databinding.ItemThreadDateTimeBinding
import org.fossify.messages.databinding.ItemThreadErrorBinding
import org.fossify.messages.databinding.ItemThreadSendingBinding
import org.fossify.messages.databinding.ItemThreadSuccessBinding
import org.fossify.messages.dialogs.DeleteConfirmationDialog
import org.fossify.messages.dialogs.MessageDetailsDialog
import org.fossify.messages.dialogs.SelectTextDialog
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.getContactFromAddress
import org.fossify.messages.extensions.isImageMimeType
import org.fossify.messages.extensions.isVCardMimeType
import org.fossify.messages.extensions.isVideoMimeType
import org.fossify.messages.extensions.launchViewIntent
import org.fossify.messages.extensions.startContactDetailsIntent
import org.fossify.messages.extensions.subscriptionManagerCompat
import org.fossify.messages.helpers.EXTRA_VCARD_URI
import org.fossify.messages.helpers.BankSmsDetector
import org.fossify.messages.helpers.IranianBankLogoImageHelper
import org.fossify.messages.helpers.IranianBankLogoResolver
import org.fossify.messages.helpers.PersianDateHelper
import org.fossify.messages.helpers.THREAD_DATE_TIME
import org.fossify.messages.helpers.THREAD_RECEIVED_MESSAGE
import org.fossify.messages.helpers.THREAD_SENT_MESSAGE
import org.fossify.messages.helpers.THREAD_SENT_MESSAGE_ERROR
import org.fossify.messages.helpers.THREAD_SENT_MESSAGE_SENDING
import org.fossify.messages.helpers.THREAD_SENT_MESSAGE_SENT
import org.fossify.messages.helpers.generateStableId
import org.fossify.messages.helpers.setupDocumentPreview
import org.fossify.messages.helpers.setupVCardPreview
import org.fossify.messages.models.Attachment
import org.fossify.messages.models.Message
import org.fossify.messages.models.ThreadItem
import org.fossify.messages.models.ThreadItem.ThreadDateTime
import org.fossify.messages.models.ThreadItem.ThreadError
import org.fossify.messages.models.ThreadItem.ThreadSending
import org.fossify.messages.models.ThreadItem.ThreadSent
import org.joda.time.DateTime

class ThreadAdapter(activity: SimpleActivity, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit, val isRecycleBin: Boolean, val deleteMessages: (messages: List<Message>, toRecycleBin: Boolean, fromRecycleBin: Boolean) -> Unit) : MyRecyclerViewListAdapter<ThreadItem>(activity, recyclerView, ThreadItemDiffCallback(), itemClick) {
    private var fontSize = activity.getTextSize()
    @SuppressLint("MissingPermission") private val hasMultipleSIMCards = (activity.subscriptionManagerCompat().activeSubscriptionInfoList?.size ?: 0) > 1
    private val maxChatBubbleWidth = (activity.usableScreenSize.x * 0.8f).toInt()
    companion object { private const val MAX_MEDIA_HEIGHT_RATIO = 3; private const val SIM_BITS = 21; private const val SIM_MASK = (1L shl SIM_BITS) - 1 }
    init { setupDragListener(true); setHasStableIds(true); (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false }
    override fun getActionMenuId() = R.menu.cab_thread
    override fun prepareActionMode(menu: Menu) { val one = isOneItemSelected(); val selected = getSelectedItems().filterIsInstance<Message>(); val hasText = selected.any { it.body.isNotEmpty() }; val showSave = getSelectedItems().all { it is Message && (it.attachment?.attachments?.size ?: 0) > 0 } && getSelectedAttachments().isNotEmpty(); menu.apply { findItem(R.id.cab_copy_to_clipboard).isVisible = hasText; findItem(R.id.cab_save_as).isVisible = showSave; findItem(R.id.cab_share).isVisible = one && hasText; findItem(R.id.cab_forward_message).isVisible = one; findItem(R.id.cab_select_text).isVisible = one && hasText; findItem(R.id.cab_properties).isVisible = one; findItem(R.id.cab_restore).isVisible = isRecycleBin } }
    override fun actionItemPressed(id: Int) { if (selectedKeys.isEmpty()) return; when (id) { R.id.cab_copy_to_clipboard -> copyToClipboard(); R.id.cab_save_as -> saveAs(); R.id.cab_share -> shareText(); R.id.cab_forward_message -> forwardMessage(); R.id.cab_select_text -> selectText(); R.id.cab_delete -> askConfirmDelete(); R.id.cab_restore -> askConfirmRestore(); R.id.cab_select_all -> selectAll(); R.id.cab_properties -> showMessageDetails() } }
    override fun getSelectableItemCount() = currentList.filterIsInstance<Message>().size
    override fun getIsItemSelectable(position: Int) = !isThreadDateTime(position)
    override fun getItemSelectionKey(position: Int): Int? = (currentList.getOrNull(position) as? Message)?.getSelectionKey()
    override fun getItemKeyPosition(key: Int): Int = currentList.indexOfFirst { (it as? Message)?.getSelectionKey() == key }
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder { val binding = when (viewType) { THREAD_DATE_TIME -> ItemThreadDateTimeBinding.inflate(layoutInflater, parent, false); THREAD_SENT_MESSAGE_ERROR -> ItemThreadErrorBinding.inflate(layoutInflater, parent, false); THREAD_SENT_MESSAGE_SENT -> ItemThreadSuccessBinding.inflate(layoutInflater, parent, false); THREAD_SENT_MESSAGE_SENDING -> ItemThreadSendingBinding.inflate(layoutInflater, parent, false); else -> ItemMessageBinding.inflate(layoutInflater, parent, false) }; return ThreadViewHolder(binding) }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) { val item = getItem(position); holder.bindView(item, item is ThreadError || item is Message, item is Message) { itemView, _ -> when (item) { is ThreadDateTime -> setupDateTime(itemView, item); is ThreadError -> setupThreadError(itemView); is ThreadSent -> setupThreadSuccess(itemView, item.delivered); is ThreadSending -> setupThreadSending(itemView); is Message -> setupView(holder, itemView, item) } }; bindViewHolder(holder) }
    override fun getItemId(position: Int): Long = when (val item = getItem(position)) { is Message -> item.getStableId(); is ThreadDateTime -> { val sim = item.simID.hashCode().toLong() and SIM_MASK; generateStableId(THREAD_DATE_TIME, (item.date.toLong() shl SIM_BITS) or sim) }; is ThreadError -> generateStableId(THREAD_SENT_MESSAGE_ERROR, item.messageId); is ThreadSending -> generateStableId(THREAD_SENT_MESSAGE_SENDING, item.messageId); is ThreadSent -> generateStableId(THREAD_SENT_MESSAGE_SENT, item.messageId) }
    override fun getItemViewType(position: Int): Int = when (val item = getItem(position)) { is ThreadDateTime -> THREAD_DATE_TIME; is ThreadError -> THREAD_SENT_MESSAGE_ERROR; is ThreadSent -> THREAD_SENT_MESSAGE_SENT; is ThreadSending -> THREAD_SENT_MESSAGE_SENDING; is Message -> if (item.isReceivedMessage()) THREAD_RECEIVED_MESSAGE else THREAD_SENT_MESSAGE }
    private fun copyToClipboard() { val selected = getSelectedItems().filterIsInstance<Message>(); if (selected.isEmpty()) return; val text = if (selected.size == 1) selected.first().body else selected.filter { it.body.isNotEmpty() }.joinToString("\n\n") { message -> val date = if (activity.config.usePersianCalendar) PersianDateHelper.format(message.millis()) else DateTime(message.millis()).toString("${activity.config.dateFormat}, ${activity.getTimeFormat()}"); val sender = if (message.isReceivedMessage()) message.senderName else activity.getString(R.string.me); "[$date] $sender: ${message.body}" }; if (text.isNotEmpty()) activity.copyToClipboard(text) }
    private fun getSelectedAttachments(): List<Attachment> = getSelectedItems().filterIsInstance<Message>().flatMap { it.attachment?.attachments.orEmpty() }
    private fun saveAs() { val attachments = getSelectedAttachments(); if (attachments.isNotEmpty()) (activity as ThreadActivity).saveMMS(attachments) }
    private fun shareText() { (getSelectedItems().firstOrNull() as? Message)?.let { activity.shareTextIntent(it.body) } }
    private fun selectText() { (getSelectedItems().firstOrNull() as? Message)?.let { if (it.body.trim().isNotEmpty()) SelectTextDialog(activity, it.body) } }
    private fun showMessageDetails() { (getSelectedItems().firstOrNull() as? Message)?.let { MessageDetailsDialog(activity, it) } }
    private fun askConfirmDelete() { val count = selectedKeys.size; val items = try { resources.getQuantityString(R.plurals.delete_messages, count, count) } catch (e: Exception) { activity.showErrorToast(e); return }; val base = if (activity.config.useRecycleBin && !isRecycleBin) org.fossify.commons.R.string.move_to_recycle_bin_confirmation else org.fossify.commons.R.string.deletion_confirmation; val question = String.format(resources.getString(base), items); DeleteConfirmationDialog(activity, question, activity.config.useRecycleBin && !isRecycleBin) { skip -> ensureBackgroundThread { val messages = getSelectedItems(); if (messages.isNotEmpty()) deleteMessages(messages.filterIsInstance<Message>(), !skip && activity.config.useRecycleBin && !isRecycleBin, false) } } }
    private fun askConfirmRestore() { val count = selectedKeys.size; val items = try { resources.getQuantityString(R.plurals.delete_messages, count, count) } catch (e: Exception) { activity.showErrorToast(e); return }; ConfirmationDialog(activity, String.format(resources.getString(R.string.restore_confirmation), items)) { ensureBackgroundThread { val messages = getSelectedItems(); if (messages.isNotEmpty()) deleteMessages(messages.filterIsInstance<Message>(), false, true) } } }
    private fun forwardMessage() { val message = getSelectedItems().firstOrNull() as? Message ?: return; val attachment = message.attachment?.attachments?.firstOrNull(); Intent(activity, NewConversationActivity::class.java).apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, message.body); if (attachment != null) putExtra(Intent.EXTRA_STREAM, attachment.getUri()); activity.startActivity(this) } }
    private fun getSelectedItems(): ArrayList<ThreadItem> = currentList.filter { selectedKeys.contains((it as? Message)?.getSelectionKey() ?: 0) } as ArrayList<ThreadItem>
    private fun isThreadDateTime(position: Int) = currentList.getOrNull(position) is ThreadDateTime
    fun updateMessages(newMessages: List<ThreadItem>, scrollPosition: Int = -1, smoothScroll: Boolean = false) { submitList(newMessages.toMutableList()) { if (scrollPosition != -1) { if (smoothScroll) recyclerView.smoothScrollToPosition(scrollPosition) else recyclerView.scrollToPosition(scrollPosition) } } }
    private fun setupView(holder: ViewHolder, view: View, message: Message) { ItemMessageBinding.bind(view).apply { threadMessageHolder.isSelected = selectedKeys.contains(message.getSelectionKey()); threadMessageBody.apply { text = message.body; setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize); beVisibleIf(message.body.isNotEmpty()); setOnLongClickListener { holder.viewLongClicked(); true }; setOnClickListener { holder.viewClicked(message) } }; if (message.isReceivedMessage()) setupReceivedMessageView(this, message) else setupSentMessageView(this, message); if (message.attachment?.attachments?.isNotEmpty() == true) { threadMessageAttachmentsHolder.beVisible(); threadMessageAttachmentsHolder.removeAllViews(); for (attachment in message.attachment.attachments) { when { attachment.mimetype.isImageMimeType() || attachment.mimetype.isVideoMimeType() -> setupImageView(holder, this, message, attachment); attachment.mimetype.isVCardMimeType() -> setupVCardView(holder, threadMessageAttachmentsHolder, message, attachment); else -> setupFileView(holder, threadMessageAttachmentsHolder, message, attachment) }; threadMessagePlayOutline.beVisibleIf(attachment.mimetype.startsWith("video/")) } } else { threadMessageAttachmentsHolder.beGone(); threadMessagePlayOutline.beGone() } } }
    private fun setupReceivedMessageView(binding: ItemMessageBinding, message: Message) { binding.apply { with(ConstraintSet()) { clone(threadMessageHolder); clear(threadMessageWrapper.id, ConstraintSet.END); connect(threadMessageWrapper.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START); applyTo(threadMessageHolder) }; threadMessageSenderPhoto.beVisible(); threadMessageSenderPhoto.setOnClickListener { val contact = message.getSender() ?: return@setOnClickListener; val number = contact.phoneNumbers.firstOrNull()?.normalizedNumber ?: return@setOnClickListener; activity.getContactFromAddress(number) { if (it != null) activity.startContactDetailsIntent(it) } }; threadMessageBody.apply { background = AppCompatResources.getDrawable(activity, R.drawable.item_received_background); setTextColor(textColor); setLinkTextColor(activity.getProperPrimaryColor()) }; if (!activity.isFinishing && !activity.isDestroyed) { val detection = BankSmsDetector.detect(message.senderPhoneNumber, message.body); val bankLogoRes = detection?.let { IranianBankLogoResolver.resolve(activity, it.bank) }; val letter = SimpleContactsHelper(activity).getContactLetterIcon(message.senderName); val placeholder = letter.toDrawable(activity.resources); if (bankLogoRes != null) { Glide.with(activity).clear(threadMessageSenderPhoto); if (!IranianBankLogoImageHelper.setBankLogo(threadMessageSenderPhoto, bankLogoRes)) threadMessageSenderPhoto.setImageDrawable(placeholder) } else { val options = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE).error(placeholder).centerCrop(); Glide.with(activity).load(message.senderPhotoUri).placeholder(placeholder).apply(options).apply(RequestOptions.circleCropTransform()).into(threadMessageSenderPhoto) } } } }
    private fun setupSentMessageView(binding: ItemMessageBinding, message: Message) { binding.apply { with(ConstraintSet()) { clone(threadMessageHolder); clear(threadMessageWrapper.id, ConstraintSet.START); connect(threadMessageWrapper.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END); applyTo(threadMessageHolder) }; val primary = activity.getProperPrimaryColor(); val contrast = primary.getContrastColor(); threadMessageBody.apply { updateLayoutParams<RelativeLayout.LayoutParams> { removeRule(RelativeLayout.END_OF); addRule(RelativeLayout.ALIGN_PARENT_END) }; background = AppCompatResources.getDrawable(activity, R.drawable.item_sent_background); background.applyColorFilter(primary); setTextColor(contrast); setLinkTextColor(contrast); if (message.isScheduled) { typeface = Typeface.create(FontHelper.getTypeface(activity), Typeface.ITALIC); val drawable = AppCompatResources.getDrawable(activity, org.fossify.commons.R.drawable.ic_clock_vector)?.apply { applyColorFilter(contrast); val size = lineHeight; setBounds(0, 0, size, size) }; setCompoundDrawables(null, null, drawable, null) } else { typeface = FontHelper.getTypeface(activity); setCompoundDrawables(null, null, null, null) } } } }
    private fun setupImageView(holder: ViewHolder, binding: ItemMessageBinding, message: Message, attachment: Attachment) = binding.apply { val mime = attachment.mimetype; val uri = attachment.getUri(); val image = ItemAttachmentImageBinding.inflate(layoutInflater); threadMessageAttachmentsHolder.addView(image.root); val placeholder = Color.TRANSPARENT.toDrawable(); val options = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE).placeholder(placeholder).transform(FitCenter()); Glide.with(root.context).load(uri).apply(options).dontAnimate().override(maxChatBubbleWidth, maxChatBubbleWidth * MAX_MEDIA_HEIGHT_RATIO).downsample(DownsampleStrategy.AT_MOST).listener(object : RequestListener<Drawable> { override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean { threadMessagePlayOutline.beGone(); threadMessageAttachmentsHolder.removeView(image.root); return false }; override fun onResourceReady(dr: Drawable, a: Any, t: Target<Drawable>, d: DataSource, i: Boolean) = false }).into(image.attachmentImage); image.attachmentImage.updateLayoutParams<ViewGroup.LayoutParams> { width = maxChatBubbleWidth; height = ViewGroup.LayoutParams.WRAP_CONTENT }; image.attachmentImage.setOnClickListener { if (actModeCallback.isSelectable) holder.viewClicked(message) else activity.launchViewIntent(uri, mime, attachment.filename) }; image.root.setOnLongClickListener { holder.viewLongClicked(); true } }
    private fun setupVCardView(holder: ViewHolder, parent: LinearLayout, message: Message, attachment: Attachment) { val uri = attachment.getUri(); val view = ItemAttachmentVcardBinding.inflate(layoutInflater).apply { setupVCardPreview(activity = activity, uri = uri, onClick = { if (actModeCallback.isSelectable) holder.viewClicked(message) else activity.startActivity(Intent(activity, VCardViewerActivity::class.java).also { it.putExtra(EXTRA_VCARD_URI, uri) }) }, onLongClick = { holder.viewLongClicked() }) }.root; parent.addView(view) }
    private fun setupFileView(holder: ViewHolder, parent: LinearLayout, message: Message, attachment: Attachment) { val mime = attachment.mimetype; val uri = attachment.getUri(); val view = ItemAttachmentDocumentBinding.inflate(layoutInflater).apply { setupDocumentPreview(uri = uri, title = attachment.filename, mimeType = attachment.mimetype, onClick = { if (actModeCallback.isSelectable) holder.viewClicked(message) else activity.launchViewIntent(uri, mime, attachment.filename) }, onLongClick = { holder.viewLongClicked() }) }.root; parent.addView(view) }
    private fun setupDateTime(view: View, dateTime: ThreadDateTime) { ItemThreadDateTimeBinding.bind(view).apply { threadDateTime.apply { text = if (activity.config.usePersianCalendar) PersianDateHelper.toPersianDigits(PersianDateHelper.formatMonthName(dateTime.date * 1000L)) else (dateTime.date * 1000L).formatDateOrTime(context = context, hideTimeOnOtherDays = false, showCurrentYear = false); setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize) }; threadDateTime.setTextColor(textColor); threadSimIcon.beVisibleIf(hasMultipleSIMCards); threadSimNumber.beVisibleIf(hasMultipleSIMCards); if (hasMultipleSIMCards) { threadSimNumber.text = dateTime.simID; threadSimNumber.setTextColor(textColor.getContrastColor()); threadSimIcon.applyColorFilter(textColor) } } }
    private fun setupThreadSuccess(view: View, delivered: Boolean) { ItemThreadSuccessBinding.bind(view).apply { threadSuccess.setImageResource(if (delivered) R.drawable.ic_check_double_vector else org.fossify.commons.R.drawable.ic_check_vector); threadSuccess.applyColorFilter(textColor) } }
    private fun setupThreadError(view: View) { ItemThreadErrorBinding.bind(view).threadError.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize - 4) }
    private fun setupThreadSending(view: View) { ItemThreadSendingBinding.bind(view).threadSending.apply { setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize); setTextColor(textColor) } }
    override fun onViewRecycled(holder: ViewHolder) { super.onViewRecycled(holder); if (!activity.isDestroyed && !activity.isFinishing) { val binding = (holder as ThreadViewHolder).binding; if (binding is ItemMessageBinding) Glide.with(activity).clear(binding.threadMessageSenderPhoto) } }
    inner class ThreadViewHolder(val binding: ViewBinding) : ViewHolder(binding.root)
}

private class ThreadItemDiffCallback : DiffUtil.ItemCallback<ThreadItem>() {
    override fun areItemsTheSame(oldItem: ThreadItem, newItem: ThreadItem): Boolean { if (oldItem::class.java != newItem::class.java) return false; return when (oldItem) { is ThreadError -> oldItem.messageId == (newItem as ThreadError).messageId; is ThreadSent -> oldItem.messageId == (newItem as ThreadSent).messageId; is ThreadSending -> oldItem.messageId == (newItem as ThreadSending).messageId; is Message -> Message.areItemsTheSame(oldItem, newItem as Message); is ThreadDateTime -> { val new = newItem as ThreadDateTime; oldItem.date == new.date && oldItem.simID == new.simID } } }
    override fun areContentsTheSame(oldItem: ThreadItem, newItem: ThreadItem): Boolean { if (oldItem::class.java != newItem::class.java) return false; return when (oldItem) { is ThreadSending -> true; is ThreadDateTime -> oldItem.simID == (newItem as ThreadDateTime).simID; is ThreadError -> oldItem.messageText == (newItem as ThreadError).messageText; is ThreadSent -> oldItem.delivered == (newItem as ThreadSent).delivered; is Message -> Message.areContentsTheSame(oldItem, newItem as Message) } }
}
