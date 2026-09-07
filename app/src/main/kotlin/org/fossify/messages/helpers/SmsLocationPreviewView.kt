package org.fossify.messages.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.fossify.messages.R
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class SmsLocationPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(8), dp(8), dp(8), dp(8))
        visibility = View.GONE
    }
    private val title = TextView(context).apply {
        text = "📍 موقعیت مکانی"
        textSize = 15f
        setTextColor(resolveTextColor())
    }
    private val map = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        webViewClient = WebViewClient()
        webChromeClient = WebChromeClient()
        layoutParams = LinearLayout.LayoutParams(-1, dp(180))
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

    init {
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        card.addView(title)
        card.addView(map)
        card.addView(details)
        card.addView(actions)
        visibility = View.GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { attachToMessageBody() }
    }

    private fun attachToMessageBody() {
        val body = (parent as? View)?.findViewById<TextView>(R.id.thread_message_body) ?: return
        update(body.text?.toString().orEmpty())
        body.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = update(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun update(text: String) {
        val location = parseLocation(text)
        if (location == null) {
            visibility = View.GONE
            card.visibility = View.GONE
            actions.removeAllViews()
            return
        }
        visibility = View.VISIBLE
        card.visibility = View.VISIBLE
        details.text = "${formatCoordinate(location.latitude)}, ${formatCoordinate(location.longitude)}"
        loadMap(location)
        actions.removeAllViews()
        actions.addView(actionButton("🗺️ مسیریابی") { openNavigation(location) })
        if (isSnappInstalled()) actions.addView(actionButton("🚕 باز کردن اسنپ") { openSnapp() })
    }

    private fun loadMap(location: Location) {
        val lat = location.latitude.toString(Locale.US)
        val lon = location.longitude.toString(Locale.US)
        val html = """
            <html><head><meta name='viewport' content='width=device-width,initial-scale=1'>
            <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>
            <style>html,body,#map{height:100%;margin:0}.leaflet-control-attribution{font-size:9px}</style></head>
            <body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
            <script>let m=L.map('map',{zoomControl:false}).setView([$lat,$lon],16);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(m);L.marker([$lat,$lon]).addTo(m);</script>
            </body></html>
        """.trimIndent()
        map.loadDataWithBaseURL("https://tile.openstreetmap.org/", html, "text/html", "UTF-8", null)
    }

    private fun actionButton(text: String, onClick: () -> Unit) = Button(context).apply {
        this.text = text
        isAllCaps = false
        minHeight = dp(40)
        setOnClickListener { onClick() }
    }

    private fun openNavigation(location: Location) {
        val geo = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}")
        try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, geo), "انتخاب مسیریاب")) }
        catch (_: ActivityNotFoundException) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}"))) }
    }

    private fun openSnapp() {
        context.packageManager.getLaunchIntentForPackage(SNAPP_PACKAGE)?.let(context::startActivity)
    }

    private fun isSnappInstalled() = runCatching { context.packageManager.getPackageInfo(SNAPP_PACKAGE, 0); true }.getOrDefault(false)

    private fun parseLocation(text: String): Location? {
        val normalized = normalizeDigits(text)
        val coordinate = COORDINATE_REGEX.find(normalized)
        if (coordinate != null) {
            val a = coordinate.groupValues[1].toDoubleOrNull()
            val b = coordinate.groupValues[2].toDoubleOrNull()
            if (a != null && b != null) {
                when {
                    a in -90.0..90.0 && b in -180.0..180.0 -> return Location(a, b)
                    b in -90.0..90.0 && a in -180.0..180.0 -> return Location(b, a)
                }
            }
        }
        val query = Regex("[?&](?:q|query|destination)=([^&#]+)", RegexOption.IGNORE_CASE).find(normalized)?.groupValues?.getOrNull(1)
        if (query != null) {
            val decoded = runCatching { URLDecoder.decode(query, StandardCharsets.UTF_8.name()) }.getOrDefault(query)
            val nested = COORDINATE_REGEX.find(decoded)
            val a = nested?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            val b = nested?.groupValues?.getOrNull(2)?.toDoubleOrNull()
            if (a != null && b != null && a in -90.0..90.0 && b in -180.0..180.0) return Location(a, b)
        }
        return null
    }

    private fun normalizeDigits(value: String) = buildString(value.length) {
        value.forEach { c -> append(when (c) {
            in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
            in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
            else -> c
        }) }
    }

    private fun resolveTextColor(): Int {
        val typed = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.textColorPrimary, typed, true)) {
            if (typed.resourceId != 0) context.getColor(typed.resourceId) else typed.data
        } else Color.DKGRAY
    }

    private fun formatCoordinate(value: Double) = String.format(Locale.US, "%.6f", value)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private data class Location(val latitude: Double, val longitude: Double)

    companion object {
        private const val SNAPP_PACKAGE = "cab.snapp.passenger"
        private val COORDINATE_REGEX = Regex("(-?\\d{1,3}(?:\\.\\d{4,})?)\\s*[,; ]\\s*(-?\\d{1,3}(?:\\.\\d{4,})?)")
    }
}
