package org.fossify.commons.models

import kotlinx.serialization.Serializable

@Serializable
data class PhoneNumber(
    var value: String,
    var type: Int,
    var label: String,
    var normalizedNumber: String,
    var isPrimary: Boolean = false
) {
    override fun hashCode(): Int {
        var result = value?.hashCode() ?: 0
        result = 31 * result + type
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (normalizedNumber?.hashCode() ?: 0)
        result = 31 * result + isPrimary.hashCode()
        return result
    }
}
