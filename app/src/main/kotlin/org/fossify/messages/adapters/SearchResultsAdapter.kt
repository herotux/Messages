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
import org.fossify.messages.helpers.ProviderSearchBridge
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

    init {
        requestProviderResults(highlightText)
    }

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
        searchResults = newItems.clone() as ArrayList<SearchResult>
        textToHighlight = highlightText
        notifyDataSetChanged()
        requestProviderResults(highlightText)
    }

    private fun requestProviderResults(query: String) {
        if (query.length < 2) return
        val requestedQuery = query
        ProviderSearchBridge.search(activity, requestedQuery) { providerResults ->
            if (requestedQuery != textToHighlight) return@search

            val merged = ArrayList<SearchResult>(searchResults.size + providerResults.size)
            val seenIds = HashSet<Long>()
            searchResults.forEach { result ->
                if (result.messageId >= 0) seenIds.add(result.messageId)
                merged.add(result)
            }
            providerResults.forEach { result ->
                if (result.messageId < 0 || seenIds.add(result.messageId)) {
                    merged.add(result)
                }
            }
            // Provider results are already newest-first. Keep the existing Room results
            // first and append unseen provider messages; this avoids comparing formatted dates.
            if (merged != searchResults) {
                searchResults = merged
                notifyDataSetChanged()
            }
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

            val confirmedBank = BankConversationVerificationStore.getConfirmedBank(activity, searchResult.threadId)
            val bankDetection = if (confirmedBank == null && searchResult.messageId >= 0) {
                BankSmsDetector.detect(searchResult.title, searchResult.snippet)
            } else null
            val bank = confirmedBank ?: bankDetection?.bank
            val bankLogoRes = bank?.let { IranianBankLogoResolver.resolve(activity, it) }
            val senderSource = if (searchResult.messageId < 0) searchResult.snippet else searchResult.title
            val senderLogoRes = if (bankLogoRes == null) IranianSenderIconResolver.resolve(activity, senderSource) else null
            val logoRes = bankLogoRes ?: senderLogoRes

            Glide.with(activity).clear(searchResultImage)
            if (logoRes != null && IranianBankLogoImageHelper.setBankLogo(searchResultImage, logoRes)) {
                return@apply
            }
            SimpleContactsHelper(activity).loadContactImage(searchResult.photoUri, searchResultImage, searchResult.title)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(ItemSearchResultBinding.bind(holder.itemView).searchResultImage)
        }
    }
}
