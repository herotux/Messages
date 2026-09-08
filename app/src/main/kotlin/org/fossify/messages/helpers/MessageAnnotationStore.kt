package org.fossify.messages.helpers

import android.content.Context
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.models.AnnotationLabel
import org.fossify.messages.models.ConversationLabel
import org.fossify.messages.models.ConversationNote
import org.fossify.messages.models.MessageLabel
import org.fossify.messages.models.MessageNote

object MessageAnnotationStore {
    private fun dao(context: Context) = context.messagesDB.AnnotationLabelsDao()

    fun getMessageLabels(context: Context, messageId: Long): List<AnnotationLabel> = dao(context).getMessageLabels(messageId)
    fun getMessageNote(context: Context, messageId: Long): MessageNote? = dao(context).getMessageNote(messageId)
    fun getConversationLabels(context: Context, threadId: Long): List<AnnotationLabel> = dao(context).getConversationLabels(threadId)
    fun getConversationNote(context: Context, threadId: Long): ConversationNote? = dao(context).getConversationNote(threadId)
    fun getLabels(context: Context): List<AnnotationLabel> = dao(context).getLabels()

    fun ensureLabel(context: Context, name: String, color: Int): AnnotationLabel {
        val normalized = name.trim().removePrefix("#")
        require(normalized.isNotEmpty())
        dao(context).getLabelByName(normalized)?.let { return it }
        val now = System.currentTimeMillis()
        val id = dao(context).insertLabel(AnnotationLabel(name = normalized, color = color, createdAt = now, updatedAt = now))
        return dao(context).getLabel(id) ?: dao(context).getLabelByName(normalized)!!
    }

    fun setMessageLabels(context: Context, messageId: Long, names: List<String>) {
        val db = dao(context)
        db.removeAllMessageLabels(messageId)
        names.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.forEach { name ->
            val label = ensureLabel(context, name, context.getProperPrimaryColor())
            db.addMessageLabel(MessageLabel(messageId, label.id))
        }
    }

    fun removeMessageLabel(context: Context, messageId: Long, labelId: Long) = dao(context).removeMessageLabel(messageId, labelId)

    fun setMessageNote(context: Context, messageId: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) dao(context).deleteMessageNote(messageId) else {
            val now = System.currentTimeMillis()
            val old = dao(context).getMessageNote(messageId)
            dao(context).upsertMessageNote(MessageNote(messageId, value, old?.createdAt ?: now, now))
        }
    }

    fun setConversationLabels(context: Context, threadId: Long, names: List<String>) {
        val db = dao(context)
        db.removeAllConversationLabels(threadId)
        names.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.forEach { name ->
            val label = ensureLabel(context, name, context.getProperPrimaryColor())
            db.addConversationLabel(ConversationLabel(threadId, label.id))
        }
    }

    fun setConversationNote(context: Context, threadId: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) dao(context).deleteConversationNote(threadId) else {
            val now = System.currentTimeMillis()
            val old = dao(context).getConversationNote(threadId)
            dao(context).upsertConversationNote(ConversationNote(threadId, value, old?.createdAt ?: now))
        }
    }
}
