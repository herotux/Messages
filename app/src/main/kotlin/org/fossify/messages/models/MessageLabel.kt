package org.fossify.messages.models

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "message_labels",
    primaryKeys = ["message_id", "label_id"],
    indices = [Index("label_id")]
)
data class MessageLabel(
    val messageId: Long,
    val labelId: Long,
)
