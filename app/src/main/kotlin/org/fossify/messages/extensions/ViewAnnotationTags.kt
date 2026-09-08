package org.fossify.messages.extensions

import android.view.View
import java.util.WeakHashMap

private val annotationViewTags = WeakHashMap<View, MutableMap<String, Any?>>()

fun View.setTag(key: String, value: Any?) {
    synchronized(annotationViewTags) {
        val tags = annotationViewTags.getOrPut(this) { HashMap() }
        tags[key] = value
    }
}

fun View.getTag(key: String): Any? = synchronized(annotationViewTags) {
    annotationViewTags[this]?.get(key)
}
