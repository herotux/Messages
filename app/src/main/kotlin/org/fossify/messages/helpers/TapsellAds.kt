package org.fossify.messages.helpers

import android.app.Activity
import android.util.Log
import org.fossify.messages.BuildConfig
import java.lang.reflect.Proxy

/**
 * Small, fail-safe Tapsell Plus integration.
 * Credentials are injected at build time through TAPSELL_APP_KEY and
 * TAPSELL_BANNER_ZONE environment variables. Empty values disable ads.
 */
object TapsellAds {
    private const val TAG = "TapsellAds"
    private var initialized = false

    fun initialize() {
        if (initialized || BuildConfig.TAPSELL_APP_KEY.isBlank()) return
        try {
            val clazz = Class.forName("ir.tapsell.plus.TapsellPlus")
            clazz.getMethod("initialize", String::class.java)
                .invoke(null, BuildConfig.TAPSELL_APP_KEY)
            initialized = true
        } catch (e: Exception) {
            Log.w(TAG, "Tapsell initialization failed", e)
        }
    }

    fun showBanner(activity: Activity) {
        if (BuildConfig.TAPSELL_APP_KEY.isBlank() || BuildConfig.TAPSELL_BANNER_ZONE.isBlank()) return
        initialize()

        try {
            val tapsell = Class.forName("ir.tapsell.plus.TapsellPlus")
            val bannerType = findEnumConstant("ir.tapsell.plus.enums.BannerType", "BANNER_320x50")
                ?: findEnumConstant("ir.tapsell.plus.BannerType", "BANNER_320x50")
                ?: return
            val method = tapsell.methods.firstOrNull {
                it.name == "showBannerAd" && it.parameterTypes.size == 6
            } ?: return

            val args = arrayOfNulls<Any>(6)
            args[0] = BuildConfig.TAPSELL_BANNER_ZONE
            args[1] = bannerType
            args[2] = android.view.Gravity.BOTTOM
            args[3] = android.view.Gravity.CENTER
            args[4] = createCallback(method.parameterTypes[4])
            args[5] = createCallback(method.parameterTypes[5])

            if (args[4] == null || args[5] == null) return
            method.invoke(null, *args)
        } catch (e: Exception) {
            Log.w(TAG, "Tapsell banner failed", e)
        }
    }

    fun hideBanner() {
        try {
            Class.forName("ir.tapsell.plus.TapsellPlus")
                .getMethod("hideBanner")
                .invoke(null)
        } catch (_: Exception) {
        }
    }

    private fun findEnumConstant(className: String, constant: String): Any? {
        return try {
            val clazz = Class.forName(className)
            if (!clazz.isEnum) return null
            clazz.enumConstants.firstOrNull { (it as Enum<*>).name == constant }
        } catch (_: Exception) {
            null
        }
    }

    private fun createCallback(type: Class<*>): Any? {
        if (!type.isInterface) return null
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, _, _ -> null }
    }
}
