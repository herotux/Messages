package org.fossify.messages.views

/**
 * Compatibility helper for primitive BooleanArray values.
 * Kotlin's standard mapIndexedNotNull extensions target Iterable/Array,
 * while BooleanArray is a primitive array.
 */
private inline fun <R : Any> BooleanArray.mapIndexedNotNull(
    transform: (index: Int, value: Boolean) -> R?,
): List<R> {
    val result = ArrayList<R>()
    for (index in indices) {
        transform(index, this[index])?.let(result::add)
    }
    return result
}
