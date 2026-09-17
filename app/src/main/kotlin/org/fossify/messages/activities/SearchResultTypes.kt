package org.fossify.messages.activities

import org.fossify.messages.models.Conversation
import org.fossify.messages.models.Message

/** Compatibility wrappers used by the existing mixed message/conversation search flow. */
data class MessageResult(val message: Message)
data class ConversationResult(val conversation: Conversation)
