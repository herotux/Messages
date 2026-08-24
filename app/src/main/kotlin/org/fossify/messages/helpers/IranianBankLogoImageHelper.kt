package org.fossify.messages.helpers

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources

/**
 * Safely displays bank vector resources directly on an ImageView.
 * Falls back to the platform drawable loader because a few generated vectors
 * are handled differently by AppCompat on some Android versions.
 */
object IranianBankLogoImageHelper {
    private const val TAG = "IranianBankLogo"

    fun setBankLogo(imageView: ImageView, resourceId: Int): Boolean {
        try {
            val drawable: Drawable = AppCompatResources.getDrawable(imageView.context, resourceId)
                ?: return false
            imageView.setImageDrawable(drawable)
            imageView.visibility = ImageView.VISIBLE
            imageView.alpha = 1f
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "AppCompat could not load bank logo: $resourceId", t)
        }

        return try {
            imageView.setImageResource(resourceId)
            imageView.visibility = ImageView.VISIBLE
            imageView.alpha = 1f
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to load bank logo resource: $resourceId", t)
            false
        }
    }

    fun setBankLogoOrClear(imageView: ImageView, resourceId: Int?) {
        if (resourceId != null && setBankLogo(imageView, resourceId)) return
        imageView.setImageDrawable(null)
    }
}
