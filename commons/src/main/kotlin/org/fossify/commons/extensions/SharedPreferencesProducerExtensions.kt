package org.fossify.commons.extensions

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T> SharedPreferences.sharedPreferencesCallback(
    sendOnCollect: Boolean = false,
    value: SharedPreferences.() -> T?,
): Flow<T?> = callbackFlow {
    val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(value(this@sharedPreferencesCallback))
        }

    if (sendOnCollect) {
        trySend(value(this@sharedPreferencesCallback))
    }

    registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
    awaitClose {
        unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }
}
