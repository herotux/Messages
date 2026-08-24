package org.fossify.messages.adapters

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.getTextSize
import org.fossify.commons.extensions.highlightTextPart
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.databinding.ItemSearchResultBinding
import org.fossify.messages.helpers.BankConversationVerificationStore
import org.fossify.messages.helpers.BankSmsDetector
import org.fossify.messages.helpers.IranianBankLogoImageHelper
import org.fossify.messages.helpers.IranianBankLogoResolver
import org.fossify.messages.helpers.IranianSenderIconResolver
import org.fossify.messages.models.SearchResult

class SearchResultsAdapter(
    activity: SimpleActivity,
    var searchResults: ArrayList<SearchResult>,
    recyclerView: MyRecyclerView,
    highlightText: String,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    private var fontSize = activity.getTextSize()
    private var textToHighlight = highlightText

    override fun getActionMenuId() = 0
    override fun prepareActionMode(menu: Menu) {}
    override fun actionItemPressed(id: Int) {}
    override fun getSelectableItemCount() = searchResults.size
    override fun getIsItemSelectable(position: Int) = false
    override fun getItemSelectionKey(position: Int) = searchResults.getOrNull(position)?.hashCode()
    override fun getItemKeyPosition(key: Int) = searchResults.indexOfFirst { it.hashCode() == key }
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        createViewHolder(ItemSearchResultBinding.inflate(layoutInflater, parent, false).root)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchResult = searchResults[position]
        holder.bindView(searchResult, allowSingleClick = true, allowLongClick = false) { itemView, _ ->
            setupView(itemView, searchResult)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = searchResults.size

    fun updateItems(newItems: ArrayList<SearchResult>, highlightText: String = "") {
        if (newItems.hashCode() != searchResults.hashCode()) {
            searchResults = newItems.clone() as ArrayList<SearchResult>
            textToHighlight = highlightText
            notifyDataSetChanged()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    private fun setupView(view: View, searchResult: SearchResult) {
        ItemSearchResultBinding.bind(view).apply {
            searchResultTitle.text = searchResult.title.highlightTextPart(textToHighlight, properPrimaryColor)
            searchResultTitle.setTextColor(textColor)
            searchResultTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            searchResultSnippet.text = searchResult.snippet.highlightTextPart(textToHighlight, properPrimaryColor)
            searchResultSnippet.setTextColor(textColor)
            searchResultSnippet.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            searchResultDate.text = searchResult.date
            searchResultDate.setTextColor(textColor)
            searchResultDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)

            // Search is presentation-only: bank detection must never filter or alter results.
            // Manual confirmation has the highest priority, then the actual matched SMS text.
            val confirmedBank = BankConversationVerificationStore.getConfirmedBank(activity, searchResult.threadId)
            val bankDetection = if (confirmedBank == null && searchResult.messageId >= 0) {
                BankSmsDetector.detect(searchResult.title, searchResult.snippet)
            } else {
                null
            }
            val bank = confirmedBank ?: bankDetection?.bank
            val bankLogoRes = bank?.let { IranianBankLogoResolver.resolve(activity, it) }

            val senderSource = if (searchResult.messageId < 0) searchResult.snippet else searchResult.title
            val senderLogoRes = if (bankLogoRes == null) {
                IranianSenderIconResolver.resolve(activity, senderSource)
            } else {
                null
            }
            val logoRes = bankLogoRes ?: senderLogoRes

            Glide.with(activity).clear(searchResultImage)
            if (logoRes != null && IranianBankLogoImageHelper.setBankLogo(searchResultImage, logoRes)) {
                return@apply
            }

            SimpleContactsHelper(activity).loadContactImage(
                searchResult.photoUri,
                searchResultImage,
                searchResult.title
            )
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(ItemSearchResultBinding.bind(holder.itemView).searchResultImage)
        }
    }
}
