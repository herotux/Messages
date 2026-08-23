package org.fossify.messages.helpers

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import org.fossify.messages.R

/**
 * Displays bank vector resources directly on an ImageView.
 * This intentionally avoids Glide bitmap transformations for VectorDrawable logos.
 */
object IranianBankLogoImageHelper {
    fun setBankLogo(imageView: ImageView, resourceId: Int): Boolean {
        val drawable: Drawable = AppCompatResources.getDrawable(imageView.context, resourceId) ?: return false
        imageView.setImageDrawable(drawable)
        return true
    }

    fun setBankLogoOrClear(imageView: ImageView, resourceId: Int?) {
        if (resourceId != null && setBankLogo(imageView, resourceId)) return
        imageView.setImageDrawable(null)
    }
}
