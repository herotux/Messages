package org.fossify.messages.views

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import com.google.android.material.appbar.MaterialToolbar
import org.fossify.commons.activities.BaseSimpleActivity
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
import org.fossify.messages.R

/** Compact search header used by the dedicated Homa search surface. */
class HomaMainSearchMenu(context: Context, attrs: AttributeSet) : MyAppBarLayout(context, attrs) {
    var isSearchOpen = false
    var useArrowIcon = false
    var onSearchOpenListener: (() -> Unit)? = null
    var onSearchClosedListener: (() -> Unit)? = null
    var onSearchTextChangedListener: ((text: String) -> Unit)? = null
    var onNavigateBackClickListener: (() -> Unit)? = null

    private val searchBarContainer: View
    private val toolbarContainer: RelativeLayout
    private val searchIcon: ImageView
    private val searchField: EditText
    private val topToolbar: MaterialToolbar

    override val toolbar: MaterialToolbar?
        get() = topToolbar

    init {
        LayoutInflater.from(context).inflate(R.layout.menu_search, this, true)
        searchBarContainer = findViewById(R.id.search_bar_container)
        toolbarContainer = findViewById(R.id.toolbar_container)
        searchIcon = findViewById(R.id.top_toolbar_search_icon)
        searchField = findViewById(R.id.top_toolbar_search)
        topToolbar = findViewById(R.id.top_toolbar)

        searchBarContainer.layoutParams = searchBarContainer.layoutParams.apply {
            height = dp(56)
        }
        searchBarContainer.setPadding(0, 0, 0, 0)
        topToolbar.title = ""
        searchField.visibility = View.GONE
        setClosedLayout()
    }

    fun setupMenu() {
        searchIcon.setOnClickListener {
            if (isSearchOpen) {
                closeSearch()
            } else if (useArrowIcon && onNavigateBackClickListener != null) {
                onNavigateBackClickListener!!()
            } else {
                openSearch()
                searchField.requestFocus()
                (context as? Activity)?.showKeyboard(searchField)
            }
        }

        searchField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isSearchOpen) {
                openSearch()
            }
        }

        searchField.onTextChangeListener { text ->
            onSearchTextChangedListener?.invoke(text)
        }
    }

    fun focusView() {
        openSearch()
        searchField.requestFocus()
    }

    private fun openSearch() {
        if (isSearchOpen) return
        isSearchOpen = true
        searchField.visibility = View.VISIBLE
        setOpenLayout()
        onSearchOpenListener?.invoke()
        searchIcon.setImageResource(R.drawable.ic_arrow_left_vector)
        searchIcon.contentDescription = resources.getString(R.string.back)
    }

    fun closeSearch() {
        isSearchOpen = false
        onSearchClosedListener?.invoke()
        searchField.setText("")
        searchField.clearFocus()
        searchField.visibility = View.GONE
        setClosedLayout()
        if (!useArrowIcon) {
            searchIcon.setImageResource(R.drawable.ic_search_vector)
            searchIcon.contentDescription = resources.getString(R.string.search)
        }
        (context as? Activity)?.hideKeyboard()
    }

    fun getCurrentQuery() = searchField.text.toString()

    fun updateHintText(text: String) {
        searchField.hint = text
    }

    @Suppress("unused", "EmptyFunctionBlock")
    fun toggleHideOnScroll(@Suppress("UNUSED_PARAMETER") hideOnScroll: Boolean) = Unit

    fun toggleForceArrowBackIcon(useArrowBack: Boolean) {
        useArrowIcon = useArrowBack
        if (useArrowBack) {
            searchIcon.setImageResource(R.drawable.ic_arrow_left_vector)
            searchIcon.contentDescription = resources.getString(R.string.back)
        } else {
            searchIcon.setImageResource(R.drawable.ic_search_vector)
            searchIcon.contentDescription = resources.getString(R.string.search)
        }
    }

    fun updateColors() {
        val backgroundColor = context.getProperBackgroundColor()
        val contrastColor = backgroundColor.getContrastColor()

        setBackgroundColor(Color.TRANSPARENT)
        topToolbar.background = null
        searchIcon.applyColorFilter(contrastColor)
        toolbarContainer.background?.applyColorFilter(
            color = context.getProperPrimaryColor().adjustAlpha(LOWER_ALPHA)
        )
        searchField.setTextColor(contrastColor)
        searchField.setHintTextColor(contrastColor.adjustAlpha(MEDIUM_ALPHA))
        (context as? BaseSimpleActivity)?.updateTopBarColors(this, backgroundColor)
    }

    private fun setClosedLayout() {
        toolbarContainer.background = null
        searchIcon.layoutParams = RelativeLayout.LayoutParams(dp(44), dp(56)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
        }
        searchIcon.setPadding(dp(8), 0, dp(8), 0)
        topToolbar.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            marginEnd = dp(48)
        }
    }

    private fun setOpenLayout() {
        toolbarContainer.setBackgroundResource(R.drawable.search_menu_background)
        searchIcon.layoutParams = RelativeLayout.LayoutParams(dp(44), dp(56)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
        }
        searchIcon.setPadding(dp(8), 0, dp(8), 0)
        searchField.layoutParams = RelativeLayout.LayoutParams(
            0,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            addRule(RelativeLayout.RIGHT_OF, R.id.top_toolbar_search_icon)
            addRule(RelativeLayout.LEFT_OF, R.id.top_toolbar)
        }
        topToolbar.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            marginEnd = dp(2)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
