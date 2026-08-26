package org.fossify.messages.helpers

import android.content.Context
import androidx.annotation.DrawableRes

/** Resolves a registry drawable name without allowing missing assets to crash the UI. */
object IranianBankLogoResolver {
    private val tracedBanks = setOf(
        IranianBankRegistry.BankId.SEPAH,
        IranianBankRegistry.BankId.MELLI,
        IranianBankRegistry.BankId.TEJARAT,
        IranianBankRegistry.BankId.MELLAT,
        IranianBankRegistry.BankId.TOSEE_TAAVON,
    )

    @DrawableRes
    fun resolve(context: Context, bank: IranianBankRegistry.BankInfo): Int? {
        val trace = bank.id in tracedBanks
        val resourceName = bank.logoResourceName
        if (trace) {
            DebugLog.write(context, "BANK_LOGO_RESOLVE_START bankId=${bank.id} english=${bank.englishName} resourceName=$resourceName package=${context.packageName}")
        }
        if (resourceName == null) {
            if (trace) DebugLog.write(context, "BANK_LOGO_RESOLVE_NO_RESOURCE_NAME bankId=${bank.id}")
            return null
        }

        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        if (trace) {
            DebugLog.write(context, "BANK_LOGO_RESOLVE_RESULT bankId=${bank.id} resourceName=$resourceName resourceId=$resourceId found=${resourceId != 0}")
        }
        return resourceId.takeIf { it != 0 }
    }
}
