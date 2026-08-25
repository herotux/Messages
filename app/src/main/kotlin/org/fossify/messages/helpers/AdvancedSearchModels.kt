package org.fossify.messages.helpers

data class AdvancedSearchFilter(
    val text: String = "",
    val sender: String = "",
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val direction: Direction = Direction.ANY,
    val unreadOnly: Boolean = false,
    val hasAttachment: Boolean? = null,
    val bankOnly: Boolean = false
) {
    enum class Direction { ANY, INCOMING, OUTGOING }
}

data class AdvancedSearchHit(
    val messageId: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Boolean,
    val subject: String? = null,
    val isBank: Boolean = false,
    val bankId: String? = null
)
