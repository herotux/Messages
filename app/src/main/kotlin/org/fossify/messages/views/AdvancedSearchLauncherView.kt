package org.fossify.messages.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.Toast
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
            try {
                val intent = Intent(context, AdvancedSearchActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
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
