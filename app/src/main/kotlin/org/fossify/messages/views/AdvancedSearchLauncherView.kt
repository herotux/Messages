package org.fossify.messages.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import org.fossify.commons.views.MyTextView
import org.fossify.messages.activities.AdvancedSearchActivity

/** A real clickable launcher used inside the main search overlay. */
class AdvancedSearchLauncherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MyTextView(context, attrs, defStyleAttr) {
    init {
        isClickable = true
        isFocusable = true
        setOnClickListener {
            context.startActivity(Intent(context, AdvancedSearchActivity::class.java))
        }
    }
}
