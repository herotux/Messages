package org.fossify.messages.plugins

import android.content.Context

/** Payment boundary. Plugins never depend directly on a store SDK. */
interface PluginPaymentProvider {
    fun purchase(context: Context, pluginId: String, onResult: (Boolean) -> Unit)
    fun restore(context: Context, pluginId: String, onResult: (Boolean) -> Unit)
}

/** Development provider: simulates a successful one-time purchase locally. */
class DemoPluginPaymentProvider : PluginPaymentProvider {
    override fun purchase(context: Context, pluginId: String, onResult: (Boolean) -> Unit) {
        PluginLicenseStore.markPurchased(context, pluginId)
        onResult(true)
    }

    override fun restore(context: Context, pluginId: String, onResult: (Boolean) -> Unit) {
        onResult(PluginLicenseStore.entitlement(context, pluginId) == PluginEntitlement.PURCHASED)
    }
}

/**
 * Production boundary for Cafe Bazaar billing.
 *
 * The Bazaar SDK adapter intentionally lives outside the plugin layer. It should
 * call PluginLicenseStore.markPurchased() only after Bazaar reports a verified
 * purchase. Keeping this adapter isolated lets debug builds use DemoProvider
 * without changing any plugin code.
 */
interface BazaarBillingGateway {
    fun purchase(context: Context, productId: String, onResult: (Boolean) -> Unit)
    fun restore(context: Context, productId: String, onResult: (Boolean) -> Unit)
}

class BazaarPluginPaymentProvider(
    private val gateway: BazaarBillingGateway
) : PluginPaymentProvider {
    override fun purchase(context: Context, pluginId: String, onResult: (Boolean) -> Unit) {
        gateway.purchase(context, PluginProducts.skuFor(pluginId)) { success ->
            if (success) PluginLicenseStore.markPurchased(context, pluginId)
            onResult(success)
        }
    }

    override fun restore(context: Context, pluginId: String, onResult: (Boolean) -> Unit) {
        gateway.restore(context, PluginProducts.skuFor(pluginId)) { owned ->
            if (owned) PluginLicenseStore.markPurchased(context, pluginId)
            onResult(owned)
        }
    }
}
