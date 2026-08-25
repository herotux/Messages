package org.fossify.messages.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.Toast
import org.fossify.commons.views.MyTextView
import org.fossify.messages.activities.AdvancedSearchActivity

class AdvancedSearchLauncherView : MyTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener {
            try {
                context.startActivity(Intent(context, AdvancedSearchActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Unable to open Advanced Search", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
