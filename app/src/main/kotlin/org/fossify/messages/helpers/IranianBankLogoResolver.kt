package org.fossify.messages.helpers

import android.content.Context
import androidx.annotation.DrawableRes

/** Resolves a registry drawable name without allowing missing assets to crash the UI. */
object IranianBankLogoResolver {
    @DrawableRes
    fun resolve(context: Context, bank: IranianBankRegistry.BankInfo): Int? {
        val resourceName = bank.logoResourceName
        if (bank.id == IranianBankRegistry.BankId.SEPAH) {
            DebugLog.write(context, "SEPAH_UI_RESOLVE_START bankId=${bank.id} resourceName=$resourceName package=${context.packageName}")
        }
        if (resourceName == null) {
            if (bank.id == IranianBankRegistry.BankId.SEPAH) {
                DebugLog.write(context, "SEPAH_UI_RESOLVE_NO_RESOURCE_NAME")
            }
            return null
        }

        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        if (bank.id == IranianBankRegistry.BankId.SEPAH) {
            DebugLog.write(context, "SEPAH_UI_RESOLVE_RESULT resourceName=$resourceName resourceId=$resourceId found=${resourceId != 0}")
        }
        return resourceId.takeIf { it != 0 }
    }
}
