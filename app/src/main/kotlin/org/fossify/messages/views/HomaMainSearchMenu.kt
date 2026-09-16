package org.fossify.messages.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.google.android.material.appbar.MaterialToolbar
import org.fossify.commons.R as CommonsR
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.databinding.MenuSearchBinding
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.onTextChangeListener
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.helpers.LOWER_ALPHA
import org.fossify.commons.helpers.MEDIUM_ALPHA
import org.fossify.commons.views.MyAppBarLayout

/** Main-screen search header: compact icon when closed, real field when opened. */
class HomaMainSearchMenu(context: Context, attrs: AttributeSet) : MyAppBarLayout(context, attrs) {
    var isSearchOpen = false
    var useArrowIcon = false
    var onSearchOpenListener: (() -> Unit)? = null
    var onSearchClosedListener: (() -> Unit)? = null
    var onSearchTextChangedListener: ((text: String) -> Unit)? = null
    var onNavigateBackClickListener: (() -> Unit)? = null

    val binding = MenuSearchBinding.inflate(LayoutInflater.from(context), this)

    override val toolbar: MaterialToolbar?
        get() = binding.topToolbar

    init {
        binding.searchBarContainer.layoutParams = binding.searchBarContainer.layoutParams.apply {
            height = dp(64)
        }
        binding.searchBarContainer.setPadding(0, 0, 0, 0)
        binding.topToolbarSearch.visibility = View.GONE
        setClosedLayout()
    }

    fun setupMenu() {
        binding.topToolbarSearchIcon.setOnClickListener {
            if (isSearchOpen) {
                closeSearch()
            } else if (useArrowIcon && onNavigateBackClickListener != null) {
                onNavigateBackClickListener!!()
            } else {
                binding.topToolbarSearch.visibility = View.VISIBLE
                binding.topToolbarSearch.requestFocus()
                (context as? Activity)?.showKeyboard(binding.topToolbarSearch)
            }
        }

        binding.topToolbarSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isSearchOpen) {
                openSearch()
            }
        }

        binding.topToolbarSearch.onTextChangeListener { text ->
            onSearchTextChangedListener?.invoke(text)
        }
    }

    fun focusView() {
        binding.topToolbarSearch.visibility = View.VISIBLE
        binding.topToolbarSearch.requestFocus()
    }

    private fun openSearch() {
        isSearchOpen = true
        binding.topToolbarSearch.visibility = View.VISIBLE
        setOpenLayout()
        onSearchOpenListener?.invoke()
        binding.topToolbarSearchIcon.setImageResource(CommonsR.drawable.ic_arrow_left_vector)
        binding.topToolbarSearchIcon.contentDescription = resources.getString(CommonsR.string.back)
    }

    fun closeSearch() {
        isSearchOpen = false
        onSearchClosedListener?.invoke()
        binding.topToolbarSearch.setText("")
        binding.topToolbarSearch.clearFocus()
        binding.topToolbarSearch.visibility = View.GONE
        setClosedLayout()
        if (!useArrowIcon) {
            binding.topToolbarSearchIcon.setImageResource(CommonsR.drawable.ic_search_vector)
            binding.topToolbarSearchIcon.contentDescription = resources.getString(CommonsR.string.search)
        }
        (context as? Activity)?.hideKeyboard()
    }

    fun getCurrentQuery() = binding.topToolbarSearch.text.toString()

    fun updateHintText(text: String) {
        binding.topToolbarSearch.hint = text
    }

    @Suppress("unused", "EmptyFunctionBlock")
    fun toggleHideOnScroll(@Suppress("UNUSED_PARAMETER") hideOnScroll: Boolean) = Unit

    fun toggleForceArrowBackIcon(useArrowBack: Boolean) {
        useArrowIcon = useArrowBack
        if (useArrowBack) {
            binding.topToolbarSearchIcon.setImageResource(CommonsR.drawable.ic_arrow_left_vector)
            binding.topToolbarSearchIcon.contentDescription = resources.getString(CommonsR.string.back)
        } else {
            binding.topToolbarSearchIcon.setImageResource(CommonsR.drawable.ic_search_vector)
            binding.topToolbarSearchIcon.contentDescription = resources.getString(CommonsR.string.search)
        }
    }

    fun updateColors() {
        val backgroundColor = context.getProperBackgroundColor()
        val contrastColor = backgroundColor.getContrastColor()

        setBackgroundColor(backgroundColor)
        binding.topToolbarSearchIcon.applyColorFilter(contrastColor)
        binding.toolbarContainer.background?.applyColorFilter(
            color = context.getProperPrimaryColor().adjustAlpha(LOWER_ALPHA)
        )
        binding.topToolbarSearch.setTextColor(contrastColor)
        binding.topToolbarSearch.setHintTextColor(contrastColor.adjustAlpha(MEDIUM_ALPHA))
        (context as? BaseSimpleActivity)?.updateTopBarColors(this, backgroundColor)
    }

    private fun setClosedLayout() {
        binding.toolbarContainer.background = null
        binding.topToolbarSearchIcon.layoutParams = RelativeLayout.LayoutParams(dp(48), dp(64)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
        }
        binding.topToolbarSearchIcon.setPadding(dp(8), 0, dp(8), 0)
        binding.topToolbar.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            marginEnd = dp(52)
        }
    }

    private fun setOpenLayout() {
        binding.toolbarContainer.setBackgroundResource(CommonsR.drawable.search_menu_background)
        binding.topToolbarSearchIcon.layoutParams = RelativeLayout.LayoutParams(dp(48), dp(64)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
        }
        binding.topToolbarSearchIcon.setPadding(dp(8), 0, dp(8), 0)
        binding.topToolbarSearch.layoutParams = RelativeLayout.LayoutParams(
            0,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            addRule(RelativeLayout.RIGHT_OF, CommonsR.id.top_toolbar_search_icon)
            addRule(RelativeLayout.LEFT_OF, CommonsR.id.top_toolbar)
        }
        binding.topToolbar.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            marginEnd = dp(4)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
