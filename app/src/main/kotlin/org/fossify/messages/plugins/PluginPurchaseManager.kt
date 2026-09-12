package org.fossify.messages.plugins

import android.content.Context

/** Single purchase entry point used by the Plugin Store. */
object PluginPurchaseManager {
    /** Debug builds use the local provider; production can install the Bazaar adapter. */
    var provider: PluginPaymentProvider = DemoPluginPaymentProvider()

    fun purchase(context: Context, pluginId: String, onResult: (Boolean) -> Unit) =
        provider.purchase(context, pluginId, onResult)

    fun restore(context: Context, pluginId: String, onResult: (Boolean) -> Unit) =
        provider.restore(context, pluginId, onResult)
}
