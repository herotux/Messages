package org.fossify.messages.plugins

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** OpenAI-compatible AI provider used by the built-in AI Assistant plugin. */
object AiAssistantPlugin {
    private const val PREFS = "plugin_ai_assistant"
    private const val KEY_ENDPOINT = "endpoint"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_SYSTEM = "system_prompt"

    const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions"
    const val DEFAULT_MODEL = "gpt-4o-mini"

    data class Config(
        val endpoint: String = DEFAULT_ENDPOINT,
        val apiKey: String = "",
        val model: String = DEFAULT_MODEL,
        val systemPrompt: String = "You are a helpful SMS writing assistant. Keep replies concise and natural."
    )

    fun isAvailable(context: Context) =
        PluginLicenseStore.isLicensed(context, PluginRegistry.AI_ASSISTANT)

    fun getConfig(context: Context): Config {
        val prefs = context.getSharedPreferences(PREFS, 0)
        return Config(
            endpoint = prefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT).orEmpty().ifBlank { DEFAULT_ENDPOINT },
            apiKey = prefs.getString(KEY_API_KEY, "").orEmpty(),
            model = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL },
            systemPrompt = prefs.getString(KEY_SYSTEM, Config().systemPrompt).orEmpty()
        )
    }

    fun saveConfig(context: Context, config: Config) {
        context.getSharedPreferences(PREFS, 0).edit()
            .putString(KEY_ENDPOINT, config.endpoint.trim())
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_MODEL, config.model.trim())
            .putString(KEY_SYSTEM, config.systemPrompt.trim())
            .apply()
    }

    /** Sends a chat-completions request to any OpenAI-compatible endpoint. */
    fun generate(context: Context, prompt: String, onResult: (Result<String>) -> Unit) {
        require(isAvailable(context)) { "AI Assistant is not licensed" }
        val config = getConfig(context)
        require(config.apiKey.isNotBlank()) { "API key is not configured" }
        require(prompt.isNotBlank()) { "Prompt is empty" }

        Thread {
            val result = runCatching {
                val connection = (URL(config.endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", config.systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", prompt))
                val payload = JSONObject()
                    .put("model", config.model)
                    .put("messages", messages)
                    .put("temperature", 0.7)
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (code !in 200..299) error("HTTP $code: ${parseError(response)}")
                parseResponse(response)
            }
            android.os.Handler(context.mainLooper).post { onResult(result) }
        }.start()
    }

    private fun parseResponse(raw: String): String {
        val json = JSONObject(raw)
        val choices = json.optJSONArray("choices") ?: error("AI response has no choices")
        val message = choices.optJSONObject(0)?.optJSONObject("message")
        val content = message?.optString("content").orEmpty().trim()
        if (content.isBlank()) error("AI returned an empty response")
        return content
    }

    private fun parseError(raw: String): String = runCatching {
        JSONObject(raw).optJSONObject("error")?.optString("message").orEmpty()
    }.getOrDefault("").ifBlank { raw.take(300) }
}
