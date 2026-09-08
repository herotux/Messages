package org.fossify.messages.adapters

import android.view.View
import java.util.WeakHashMap

private val annotationViewTags = WeakHashMap<View, MutableMap<String, Any?>>()

fun View.setTag(key: String, value: Any?) {
    synchronized(annotationViewTags) {
        annotationViewTags.getOrPut(this) { HashMap() }[key] = value
    }
}

fun View.getTag(key: String): Any? = synchronized(annotationViewTags) {
    annotationViewTags[this]?.get(key)
}
