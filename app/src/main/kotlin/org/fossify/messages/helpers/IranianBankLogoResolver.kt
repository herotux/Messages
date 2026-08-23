package org.fossify.messages.helpers

import android.content.Context
import androidx.annotation.DrawableRes
import org.fossify.messages.R

/** Resolves a registry drawable name without allowing missing assets to crash the UI. */
object IranianBankLogoResolver {
    @DrawableRes
    fun resolve(context: Context, bank: IranianBankRegistry.BankInfo): Int? {
        val resourceName = bank.logoResourceName ?: return null
        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        return resourceId.takeIf { it != 0 }
    }
}
