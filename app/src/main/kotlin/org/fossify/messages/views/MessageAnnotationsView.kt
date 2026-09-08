package org.fossify.messages.views

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.views.MyTextView
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.models.AnnotationLabel
import org.fossify.messages.models.MessageNote
import kotlin.math.roundToInt

class MessageAnnotationsView(
    private val activity: SimpleActivity,
    private val onLabelClick: ((AnnotationLabel) -> Unit)? = null,
    private val onNoteClick: ((MessageNote) -> Unit)? = null,
) : LinearLayout(activity) {
    private val labelsRow = LinearLayout(activity)
    private val noteView = MyTextView(activity)

    init {
        orientation = VERTICAL
        gravity = Gravity.START
        visibility = GONE
        setPadding(dp(2), dp(2), dp(2), dp(2))
        labelsRow.orientation = HORIZONTAL
        labelsRow.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        labelsRow.clipToPadding = false
        addView(labelsRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        noteView.apply {
            textSize = 11f
            setTextColor(activity.getProperTextColor())
            alpha = 0.78f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        addView(noteView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun render(labels: List<AnnotationLabel>, note: MessageNote?) {
        labelsRow.removeAllViews()
        labels.forEach { label ->
            labelsRow.addView(TextView(activity).apply {
                text = "#${label.name}"
                textSize = 10f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setTextColor(label.color)
                setPadding(dp(7), dp(2), dp(7), dp(2))
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(adjustAlpha(label.color, 0.12f))
                    setStroke(dp(1), adjustAlpha(label.color, 0.28f))
                }
                contentDescription = "Label ${label.name}"
                setOnClickListener { onLabelClick?.invoke(label) }
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(22)).apply { marginEnd = dp(5) }
            })
        }
        val noteText = note?.text?.trim().orEmpty()
        noteView.text = if (noteText.isEmpty()) "" else "Note  ·  $noteText"
        noteView.visibility = if (noteText.isEmpty()) GONE else VISIBLE
        visibility = if (labels.isEmpty() && noteText.isEmpty()) GONE else VISIBLE
    }

    private fun adjustAlpha(color: Int, alpha: Float): Int {
        val a = (android.graphics.Color.alpha(color) * alpha).roundToInt().coerceIn(18, 255)
        return android.graphics.Color.argb(a, android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
