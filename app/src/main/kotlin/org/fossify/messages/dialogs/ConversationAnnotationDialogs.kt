package org.fossify.messages.dialogs

import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.activities.BaseSimpleActivity
import org.fossify.messages.helpers.MessageAnnotationStore
import org.fossify.messages.models.Conversation

object ConversationAnnotationDialogs {
    fun editLabels(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        val input = EditText(activity).apply {
            hint = activity.getString(R.string.annotation_labels_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(false)
        }
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, 0, p, 0)
            addView(input, LinearLayout.LayoutParams(-1, -2))
            addView(TextView(activity).apply {
                text = activity.getString(R.string.annotation_labels_help)
                textSize = 12f
                alpha = 0.7f
                setPadding(0, p / 2, 0, 0)
            })
        }
        ensureBackgroundThread {
            val labels = MessageAnnotationStore.getConversationLabels(activity, conversation.threadId)
            activity.runOnUiThread {
                input.setText(labels.joinToString(", ") { it.name })
                AlertDialog.Builder(activity)
                    .setTitle(R.string.annotation_add_label)
                    .setView(container)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val names = input.text.toString().split(',', '\n', '،')
                        ensureBackgroundThread {
                            MessageAnnotationStore.setConversationLabels(activity, conversation.threadId, names)
                            activity.runOnUiThread(onSaved)
                        }
                    }
                    .show()
            }
        }
    }

    fun editNote(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        val input = EditText(activity).apply {
            hint = activity.getString(R.string.annotation_note_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 4
            maxLines = 8
        }
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, 0, p, 0)
            addView(input, LinearLayout.LayoutParams(-1, -2))
        }
        ensureBackgroundThread {
            val existing = MessageAnnotationStore.getConversationNote(activity, conversation.threadId)?.text.orEmpty()
            activity.runOnUiThread {
                input.setText(existing)
                input.setSelection(input.text.length)
                AlertDialog.Builder(activity)
                    .setTitle(R.string.annotation_add_note)
                    .setView(container)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.save, null)
                    .create().apply {
                        setOnShowListener {
                            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                ensureBackgroundThread {
                                    MessageAnnotationStore.setConversationNote(activity, conversation.threadId, input.text.toString())
                                    activity.runOnUiThread {
                                        dismiss()
                                        onSaved()
                                    }
                                }
                            }
                        }
                    }.show()
            }
        }
    }
}
