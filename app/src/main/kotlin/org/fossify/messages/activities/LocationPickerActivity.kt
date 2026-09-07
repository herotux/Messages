package org.fossify.messages.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class LocationPickerActivity : SimpleActivity() {
    private lateinit var map: WebView
    private var selectedLat = 35.6892
    private var selectedLon = 51.3890
    private var locationReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            map = WebView(this@LocationPickerActivity).apply {
                layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                addJavascriptInterface(MapBridge(), "Android")
            }
            addView(map)
            addView(Button(this@LocationPickerActivity).apply {
                text = "📍 ارسال این موقعیت"
                isAllCaps = false
                setOnClickListener { finishWithLocation() }
            }, LinearLayout.LayoutParams(-1, -2))
        })
        requestLocationIfNeeded()
        loadMap()
    }

    private fun coordinate(value: Double) = String.format(Locale.US, "%.6f", value)

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadMap() {
        val lat = coordinate(selectedLat)
        val lon = coordinate(selectedLon)
        val html = """
            <!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>
            <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>
            <style>html,body,#map{height:100%;margin:0} .hint{position:absolute;z-index:9999;top:10px;left:50%;transform:translateX(-50%);background:white;padding:7px 12px;border-radius:18px;box-shadow:0 1px 5px #777;font-family:sans-serif}</style>
            </head><body><div id='map'></div><div class='hint'>نقشه را جابه‌جا کنید و نشانگر را روی مقصد بگذارید</div>
            <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
            <script>
              const map=L.map('map').setView([$lat,$lon],15);
              L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
              const marker=L.marker([$lat,$lon],{draggable:true}).addTo(map);
              function send(){const p=marker.getLatLng();Android.pick(p.lat,p.lng);}
              marker.on('dragend',send); map.on('click',e=>{marker.setLatLng(e.latlng);send();});
            </script></body></html>
        """.trimIndent()
        map.loadDataWithBaseURL("https://www.openstreetmap.org/", html, "text/html", "UTF-8", null)
    }

    private fun requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) useLastKnownLocation()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private fun useLastKnownLocation() {
        if (locationReady) return
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        val candidates = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val best = candidates.mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }.maxByOrNull { it.time }
        if (best != null) {
            selectedLat = best.latitude; selectedLon = best.longitude; locationReady = true
            map.post { val lat = coordinate(selectedLat); val lon = coordinate(selectedLon); map.evaluateJavascript("map.setView([$lat,$lon],16); marker.setLatLng([$lat,$lon]);", null) }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) useLastKnownLocation()
    }

    private fun finishWithLocation() {
        val lat = coordinate(selectedLat); val lon = coordinate(selectedLon)
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_LATITUDE, selectedLat)
            putExtra(EXTRA_LONGITUDE, selectedLon)
            putExtra(EXTRA_LOCATION_URL, "https://maps.google.com/?q=$lat,$lon")
        })
        finish()
    }

    inner class MapBridge { @JavascriptInterface fun pick(lat: Double, lon: Double) { selectedLat = lat; selectedLon = lon } }

    companion object {
        const val EXTRA_LATITUDE = "location_latitude"
        const val EXTRA_LONGITUDE = "location_longitude"
        const val EXTRA_LOCATION_URL = "location_url"
        const val REQUEST_LOCATION = 4201
    }
}