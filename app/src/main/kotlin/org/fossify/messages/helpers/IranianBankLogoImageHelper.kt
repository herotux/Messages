package org.fossify.messages.helpers

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources

/**
 * Safely displays bank vector resources directly on an ImageView.
 * This intentionally avoids Glide bitmap transformations for VectorDrawable logos.
 */
object IranianBankLogoImageHelper {
    private const val TAG = "IranianBankLogo"

    fun setBankLogo(imageView: ImageView, resourceId: Int): Boolean {
        return try {
            val drawable: Drawable = AppCompatResources.getDrawable(imageView.context, resourceId) ?: return false
            imageView.setImageDrawable(drawable)
            true
        } catch (t: Throwable) {
            // A malformed/generated vector must never crash the message list/thread.
            Log.e(TAG, "Unable to load bank logo resource: $resourceId", t)
            false
        }
    }

    fun setBankLogoOrClear(imageView: ImageView, resourceId: Int?) {
        if (resourceId != null && setBankLogo(imageView, resourceId)) return
        imageView.setImageDrawable(null)
    }
}
