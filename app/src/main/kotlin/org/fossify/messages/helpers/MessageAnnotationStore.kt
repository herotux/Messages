package org.fossify.messages.helpers

import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.models.AnnotationLabel
import org.fossify.messages.models.ConversationLabel
import org.fossify.messages.models.ConversationNote
import org.fossify.messages.models.MessageLabel
import org.fossify.messages.models.MessageNote

object MessageAnnotationStore {
    private fun dao(activity: SimpleActivity) = activity.messagesDB.AnnotationLabelsDao()
    fun getMessageLabels(activity: SimpleActivity, messageId: Long): List<AnnotationLabel> = dao(activity).getMessageLabels(messageId)
    fun getMessageNote(activity: SimpleActivity, messageId: Long): MessageNote? = dao(activity).getMessageNote(messageId)
    fun getConversationLabels(activity: SimpleActivity, threadId: Long): List<AnnotationLabel> = dao(activity).getConversationLabels(threadId)
    fun getConversationNote(activity: SimpleActivity, threadId: Long): ConversationNote? = dao(activity).getConversationNote(threadId)
    fun getLabels(activity: SimpleActivity): List<AnnotationLabel> = dao(activity).getLabels()

    fun ensureLabel(activity: SimpleActivity, name: String, color: Int): AnnotationLabel {
        val normalized = name.trim().removePrefix("#")
        require(normalized.isNotEmpty())
        dao(activity).getLabelByName(normalized)?.let { return it }
        val now = System.currentTimeMillis()
        val id = dao(activity).insertLabel(AnnotationLabel(name = normalized, color = color, createdAt = now, updatedAt = now))
        return dao(activity).getLabel(id) ?: dao(activity).getLabelByName(normalized)!!
    }

    fun setMessageLabels(activity: SimpleActivity, messageId: Long, names: List<String>) {
        val db = dao(activity)
        db.removeAllMessageLabels(messageId)
        names.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.forEach { name ->
            val label = ensureLabel(activity, name, activity.getProperPrimaryColor())
            db.addMessageLabel(MessageLabel(messageId, label.id))
        }
    }

    fun removeMessageLabel(activity: SimpleActivity, messageId: Long, labelId: Long) = dao(activity).removeMessageLabel(messageId, labelId)

    fun setMessageNote(activity: SimpleActivity, messageId: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) dao(activity).deleteMessageNote(messageId) else {
            val now = System.currentTimeMillis()
            val old = dao(activity).getMessageNote(messageId)
            dao(activity).upsertMessageNote(MessageNote(messageId, value, old?.createdAt ?: now, now))
        }
    }

    fun setConversationLabels(activity: SimpleActivity, threadId: Long, names: List<String>) {
        val db = dao(activity)
        db.removeAllConversationLabels(threadId)
        names.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.forEach { name ->
            val label = ensureLabel(activity, name, activity.getProperPrimaryColor())
            db.addConversationLabel(ConversationLabel(threadId, label.id))
        }
    }

    fun setConversationNote(activity: SimpleActivity, threadId: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) dao(activity).deleteConversationNote(threadId) else {
            val now = System.currentTimeMillis()
            val old = dao(activity).getConversationNote(threadId)
            dao(activity).upsertConversationNote(ConversationNote(threadId, value, old?.createdAt ?: now, now))
        }
    }
}
