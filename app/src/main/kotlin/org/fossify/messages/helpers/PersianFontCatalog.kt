package org.fossify.messages.helpers

import android.content.Context
import android.graphics.Typeface
import org.fossify.commons.helpers.FontHelper
import org.fossify.messages.R
import java.net.HttpURLConnection
import java.net.URL

/**
 * Persian/Arabic fonts that can be selected from the app.
 * Vazirmatn is bundled with the app. The other open-source families are
 * fetched once from their upstream GitHub repositories and then cached
 * locally, so they remain available offline after the first successful use.
 */
data class PersianFontSpec(
    val title: String,
    val fileName: String,
    val upstreamUrl: String? = null,
    val bundledResource: Int? = null,
)

object PersianFontCatalog {
    val fonts = listOf(
        PersianFontSpec(
            title = "وزیرمتن (Vazirmatn)",
            fileName = "vazirmatn_regular.ttf",
            bundledResource = R.font.vazirmatn_regular,
        ),
        PersianFontSpec(
            title = "شبنم (Shabnam)",
            fileName = "shabnam.ttf",
            upstreamUrl = "https://raw.githubusercontent.com/rastikerdar/shabnam-font/master/dist/Shabnam.ttf",
        ),
        PersianFontSpec(
            title = "ساحل (Sahel)",
            fileName = "sahel.ttf",
            upstreamUrl = "https://raw.githubusercontent.com/rastikerdar/sahel-font/master/dist/Sahel.ttf",
        ),
        PersianFontSpec(
            title = "صمیم (Samim)",
            fileName = "samim.ttf",
            upstreamUrl = "https://raw.githubusercontent.com/rastikerdar/samim-font/master/dist/Samim.ttf",
        ),
        PersianFontSpec(
            title = "پرستو (Parastoo)",
            fileName = "parastoo.ttf",
            upstreamUrl = "https://raw.githubusercontent.com/rastikerdar/parastoo-font/master/dist/Parastoo.ttf",
        ),
        PersianFontSpec(
            title = "گندم (Gandom)",
            fileName = "gandom.ttf",
            upstreamUrl = "https://raw.githubusercontent.com/rastikerdar/gandom-font/master/dist/Gandom.ttf",
        ),
        PersianFontSpec(
            title = "تنها (Tanha)",
            fileName = "tanha.ttf",
            upstreamUrl = "https://raw.githubusercontent.com/rastikerdar/tanha-font/master/dist/Tanha.ttf",
        ),
    )

    fun install(context: Context, spec: PersianFontSpec): Result<Unit> = runCatching {
        val existing = FontHelper.getFontsDir(context).resolve(spec.fileName)
        if (existing.exists() && existing.length() > 0L) return@runCatching Unit

        val bytes = if (spec.bundledResource != null) {
            context.resources.openRawResource(spec.bundledResource).use { it.readBytes() }
        } else {
            require(!spec.upstreamUrl.isNullOrBlank())
            val connection = (URL(spec.upstreamUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Homa-Messages")
            }
            try {
                require(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
        }

        require(bytes.size > 1024) { "Invalid font file" }
        require(FontHelper.saveFontData(context, bytes, spec.fileName)) { "Unable to cache font" }
    }

    fun typeface(context: Context, spec: PersianFontSpec): Typeface {
        if (!FontHelper.getFontsDir(context).resolve(spec.fileName).exists()) {
            install(context, spec)
        }
        return Typeface.createFromFile(FontHelper.getFontsDir(context).resolve(spec.fileName))
    }
}
