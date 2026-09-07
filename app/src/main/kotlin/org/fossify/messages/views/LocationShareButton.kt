package org.fossify.messages.views

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.fossify.messages.R
import java.util.Locale

class LocationShareButton(context: Context) : LinearLayout(context) {
    private val activity = context as? Activity
    private var lat = 35.6892
    private var lon = 51.3890

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(8))
        setBackgroundResource(android.R.drawable.list_selector_background)
        isClickable = true
        addView(TextView(context).apply { text = "📍"; textSize = 28f })
        addView(TextView(context).apply { text = "موقعیت"; textSize = 12f })
        setOnClickListener { openPicker() }
    }

    private fun openPicker() {
        val activity = activity ?: return
        val root = LinearLayout(activity).apply { orientation = VERTICAL }
        val map = WebView(activity).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            layoutParams = LinearLayout.LayoutParams(-1, dp(420))
        }
        root.addView(map)
        root.addView(TextView(activity).apply {
            text = "نشانگر را جابه‌جا کنید یا روی نقشه ضربه بزنید، سپس «افزودن موقعیت» را بزنید."
            setPadding(dp(12), dp(8), dp(12), dp(8))
        })
        val dialog = AlertDialog.Builder(activity)
            .setTitle("📍 اشتراک موقعیت")
            .setView(root)
            .setNegativeButton("لغو", null)
            .create()
        val positive = Button(activity).apply {
            text = "افزودن موقعیت"
            isAllCaps = false
        }
        root.addView(positive)
        positive.setOnClickListener {
            val input = activity.findViewById<EditText>(R.id.thread_type_message) ?: return@setOnClickListener
            val locationText = "موقعیت مکانی: https://maps.google.com/?q=${lat.toString(Locale.US)},${lon.toString(Locale.US)}"
            val current = input.text?.toString().orEmpty().trimEnd()
            val combined = if (current.isBlank()) locationText else "$current\n\n$locationText"
            input.setText(combined)
            input.setSelection(input.length())
            dialog.dismiss()
        }
        dialog.show()
        loadMap(map)
    }

    private fun loadMap(map: WebView) {
        val html = """
            <html><head><meta name='viewport' content='width=device-width,initial-scale=1'>
            <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>
            <style>html,body,#map{height:100%;margin:0}.leaflet-control-attribution{font-size:10px}</style></head>
            <body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
            <script>
              let map=L.map('map').setView([${lat.toString(Locale.US)},${lon.toString(Locale.US)}],15);
              L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
              let marker=L.marker([${lat.toString(Locale.US)},${lon.toString(Locale.US)}],{draggable:true}).addTo(map);
              function pick(p){window.location.href='app://pick?lat='+p.lat+'&lon='+p.lng;}
              marker.on('dragend',e=>pick(marker.getLatLng()));
              map.on('click',e=>{marker.setLatLng(e.latlng);pick(e.latlng);});
            </script></body></html>
        """.trimIndent()
        map.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("app://pick")) {
                    Regex("lat=([-0-9.]+)&lon=([-0-9.]+)").find(url)?.let { m ->
                        lat = m.groupValues[1].toDouble()
                        lon = m.groupValues[2].toDouble()
                    }
                    return true
                }
                return false
            }
        }
        map.loadDataWithBaseURL("https://tile.openstreetmap.org/", html, "text/html", "UTF-8", null)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
