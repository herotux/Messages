package org.fossify.messages.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.fossify.messages.R
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Detects shared coordinates/map links in an SMS and renders a lightweight,
 * offline-safe location card. No AI and no device location permission are used.
 *
 * The actual navigation is delegated to apps installed by the user through
 * ACTION_VIEW. Snapp is opened only after an explicit tap; the app never books
 * or charges a ride automatically.
 */
class SmsLocationPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setBackgroundColor(Color.TRANSPARENT)
        visibility = View.GONE
    }
    private val title = TextView(context).apply {
        text = "📍 موقعیت مکانی"
        textSize = 15f
        setTextColor(resolveTextColor())
    }
    private val details = TextView(context).apply {
        textSize = 12f
        setPadding(0, dp(4), 0, dp(6))
        setTextColor(resolveTextColor())
    }
    private val actions = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.START
    }
    private var location: Location? = null

    init {
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        card.addView(title)
        card.addView(details)
        card.addView(actions)
        visibility = View.GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { attachToMessageBody() }
    }

    private fun attachToMessageBody() {
        val parent = parent ?: return
        val body = parent.findViewById<TextView>(R.id.thread_message_body) ?: return
        update(body.text?.toString().orEmpty())
        body.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = update(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun update(text: String) {
        val parsed = parseLocation(text)
        location = parsed
        if (parsed == null) {
            visibility = View.GONE
            card.visibility = View.GONE
            actions.removeAllViews()
            return
        }

        visibility = View.VISIBLE
        card.visibility = View.VISIBLE
        details.text = if (parsed.label.isNotBlank()) {
            "${parsed.label}\n${formatCoordinate(parsed.latitude)}, ${formatCoordinate(parsed.longitude)}"
        } else {
            "${formatCoordinate(parsed.latitude)}, ${formatCoordinate(parsed.longitude)}"
        }

        actions.removeAllViews()
        actions.addView(actionButton("🗺️ مسیریابی") {
            openNavigation(parsed)
        })
        if (isSnappInstalled()) {
            actions.addView(actionButton("🚕 باز کردن اسنپ") {
                openSnapp(parsed)
            })
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button = Button(context).apply {
        this.text = text
        isAllCaps = false
        minHeight = dp(40)
        setOnClickListener { onClick() }
    }

    private fun openNavigation(location: Location) {
        val geo = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}")
        val intent = Intent(Intent.ACTION_VIEW, geo)
        try {
            context.startActivity(Intent.createChooser(intent, "انتخاب مسیریاب"))
        } catch (_: ActivityNotFoundException) {
            val web = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    }

    private fun openSnapp(location: Location) {
        // Snapp does not expose a stable, public passenger deep-link contract
        // for pre-filling a ride destination. We therefore only launch the
        // installed passenger app; the user confirms the trip there.
        val launch = context.packageManager.getLaunchIntentForPackage(SNAPP_PACKAGE) ?: return
        launch.putExtra("destination_latitude", location.latitude)
        launch.putExtra("destination_longitude", location.longitude)
        context.startActivity(launch)
    }

    private fun isSnappInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SNAPP_PACKAGE, 0)
        true
    } catch (_: Exception) {
        false
    }

    private fun parseLocation(text: String): Location? {
        val normalized = normalizeDigits(text)
        val coordinate = COORDINATE_REGEX.find(normalized)
        if (coordinate != null) {
            val first = coordinate.groupValues[1].toDoubleOrNull()
            val second = coordinate.groupValues[2].toDoubleOrNull()
            if (first != null && second != null) {
                val latLon = if (first in -90.0..90.0 && second in -180.0..180.0) first to second else null
                val lonLat = if (second in -90.0..90.0 && first in -180.0..180.0) second to first else null
                val pair = latLon ?: lonLat
                if (pair != null) return Location(pair.first, pair.second, "")
            }
        }

        val query = Regex("[?&](?:q|query|destination)=([^&#]+)", RegexOption.IGNORE_CASE).find(normalized)
            ?.groupValues?.getOrNull(1)
        if (query != null) {
            val decoded = runCatching { URLDecoder.decode(query, StandardCharsets.UTF_8.name()) }.getOrDefault(query)
            val nested = COORDINATE_REGEX.find(decoded)
            if (nested != null) {
                val lat = nested.groupValues[1].toDoubleOrNull()
                val lon = nested.groupValues[2].toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    return Location(lat, lon, decoded)
                }
            }
        }
        return null
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { c ->
            append(
                when (c) {
                    in '۰'..'۹' -> ('0'.code + (c.code - '۰'.code)).toChar()
                    in '٠'..'٩' -> ('0'.code + (c.code - '٠'.code)).toChar()
                    else -> c
                }
            )
        }
    }

    private fun resolveTextColor(): Int {
        val typed = android.util.TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.textColorPrimary, typed, true)) {
            android.content.res.Resources.getSystem().getColor(typed.resourceId)
        } else Color.DKGRAY
    }

    private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class Location(val latitude: Double, val longitude: Double, val label: String)

    companion object {
        private const val SNAPP_PACKAGE = "cab.snapp.passenger"
        private val COORDINATE_REGEX = Regex("(-?\\d{1,3}(?:\\.\\d{4,})?)\\s*[,; ]\\s*(-?\\d{1,3}(?:\\.\\d{4,})?)")
    }
}
