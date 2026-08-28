package org.fossify.messages.helpers

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources

/** Safely displays bank vector resources directly on an ImageView. */
object IranianBankLogoImageHelper {
    private const val TAG = "IranianBankLogo"

    fun setBankLogo(imageView: ImageView, resourceId: Int): Boolean {
        return try {
            DebugLog.write(imageView.context, "BANK_LOGO_DRAWABLE_START resourceId=$resourceId imageViewId=${imageView.id} package=${imageView.context.packageName}")
            val drawable: Drawable? = AppCompatResources.getDrawable(imageView.context, resourceId)
            if (drawable == null) {
                Log.e(TAG, "BANK_LOGO_DRAWABLE_NULL resourceId=$resourceId imageViewId=${imageView.id}")
                DebugLog.write(imageView.context, "BANK_LOGO_DRAWABLE_NULL resourceId=$resourceId imageViewId=${imageView.id}")
                return false
            }
            imageView.setImageDrawable(null)
            imageView.setImageDrawable(drawable)
            DebugLog.write(
                imageView.context,
                "BANK_LOGO_DRAWABLE_SET resourceId=$resourceId imageViewId=${imageView.id} " +
                    "drawable=${drawable.javaClass.simpleName} intrinsic=${drawable.intrinsicWidth}x${drawable.intrinsicHeight}"
            )
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to load bank logo resource: $resourceId", t)
            DebugLog.write(
                imageView.context,
                "BANK_LOGO_DRAWABLE_EXCEPTION resourceId=$resourceId imageViewId=${imageView.id} " +
                    "type=${t.javaClass.simpleName} message=${t.message}"
            )
            false
        }
    }

    fun setBankLogoOrClear(imageView: ImageView, resourceId: Int?) {
        if (resourceId != null && setBankLogo(imageView, resourceId)) return
        imageView.setImageDrawable(null)
    }
}
