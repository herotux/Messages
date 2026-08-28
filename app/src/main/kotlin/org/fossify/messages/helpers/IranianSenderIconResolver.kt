package org.fossify.messages.helpers

import android.content.Context
import androidx.annotation.DrawableRes

/** Resolves trusted non-bank/service sender icons without affecting bank detection. */
object IranianSenderIconResolver {
    @DrawableRes
    fun resolve(context: Context, sender: String?): Int? {
        val info = IranianSenderIconRegistry.find(sender) ?: return null
        val resourceName = info.logoResourceName ?: return null
        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        return resourceId.takeIf { it != 0 }
    }
}
