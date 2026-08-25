package org.fossify.messages.activities

import org.fossify.messages.models.Conversation

/** Lightweight compatibility state used by the existing search UI callback. */
object SearchBuildCompatState {
    @Volatile
    var currentConversations: List<Conversation> = emptyList()
}

val currentList: List<Conversation>
    get() = SearchBuildCompatState.currentConversations
