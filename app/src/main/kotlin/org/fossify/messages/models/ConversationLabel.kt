package org.fossify.messages.models

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "conversation_labels",
    primaryKeys = ["thread_id", "label_id"],
    indices = [Index("label_id")]
)
data class ConversationLabel(
    val threadId: Long,
    val labelId: Long,
)
