package org.fossify.messages.plugins

import android.content.Context
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** Local tools exposed to the AI Assistant. Destructive/external actions are proposal-only. */
object AiSmsToolEngine {
    private const val DEFAULT_LIMIT = 20
    private const val MAX_LIMIT = 50

    fun definitions(): JSONArray = JSONArray().apply {
        put(tool("search_sms", "Search SMS by text, sender and optional time range.", obj(
            "query" to prop("string"),
            "sender" to prop("string"),
            "date_from" to prop("integer"),
            "date_to" to prop("integer"),
            "limit" to prop("integer")
        )))
        put(tool("get_conversation", "Read recent SMS messages for one sender.", obj(
            "sender" to prop("string"),
            "limit" to prop("integer"),
            required = arrayOf("sender")
        )))
        put(tool("find_appointments", "Find likely appointment, meeting, reservation or reminder SMS.", obj(
            "date_from" to prop("integer"),
            "date_to" to prop("integer"),
            "limit" to prop("integer")
        )))
        put(tool("get_sms", "Read one exact SMS by database id.", obj(
            "message_id" to prop("integer"),
            required = arrayOf("message_id")
        )))
        put(tool("draft_reply", "Prepare a reply draft; never send it.", obj(
            "message_id" to prop("integer"),
            "tone" to prop("string"),
            "instruction" to prop("string"),
            required = arrayOf("message_id")
        )))
        put(tool("translate_sms", "Translate one exact SMS; never edit or send it.", obj(
            "message_id" to prop("integer"),
            "target_language" to prop("string"),
            "preserve_names_numbers" to prop("boolean"),
            required = arrayOf("message_id", "target_language")
        )))
        put(tool("find_location", "Find coordinates or location text in an SMS.", obj(
            "message_id" to prop("integer"),
            "location_text" to prop("string")
        )))
        put(tool("navigate_to_location", "Prepare a navigation URI; requires explicit user confirmation.", obj(
            "message_id" to prop("integer"),
            "location_text" to prop("string")
        )))
        put(tool("request_snapp_taxi", "Prepare a Snapp taxi proposal; never book or share location automatically.", obj(
            "message_id" to prop("integer"),
            "destination" to prop("string"),
            "latitude" to prop("number"),
            "longitude" to prop("number")
        )))
    }

    fun execute(context: Context, name: String, args: JSONObject): JSONObject = try {
        when (name) {
            "search_sms" -> searchSms(context, args)
            "get_conversation" -> getConversation(context, args)
            "find_appointments" -> findAppointments(context, args)
            "get_sms" -> getSms(context, args)
            "draft_reply" -> draftReply(context, args)
            "translate_sms" -> translateSms(context, args)
            "find_location" -> findLocation(context, args)
            "navigate_to_location" -> navigateToLocation(context, args)
            "request_snapp_taxi" -> requestSnappTaxi(context, args)
            else -> error("Unknown tool: $name")
        }
    } catch (e: SecurityException) {
        JSONObject().put("error", "SMS access is not available")
    } catch (e: Exception) {
        JSONObject().put("error", e.message ?: "Tool execution failed")
    }

    private fun searchSms(context: Context, args: JSONObject): JSONObject = querySms(
        context,
        args.optString("query").trim(),
        args.optString("sender").trim(),
        args.optLong("date_from", -1L),
        args.optLong("date_to", -1L),
        safeLimit(args.optInt("limit", DEFAULT_LIMIT))
    )

    private fun getConversation(context: Context, args: JSONObject): JSONObject {
        val sender = args.optString("sender").trim()
        require(sender.isNotBlank()) { "sender is required" }
        return querySms(context, "", sender, -1L, -1L, safeLimit(args.optInt("limit", DEFAULT_LIMIT)))
    }

    private fun findAppointments(context: Context, args: JSONObject): JSONObject {
        val from = args.optLong("date_from", -1L)
        val to = args.optLong("date_to", -1L)
        val limit = safeLimit(args.optInt("limit", DEFAULT_LIMIT))
        val keywords = listOf(
            "appointment", "meeting", "reservation", "booking", "schedule", "reminder",
            "نوبت", "قرار", "جلسه", "رزرو", "ملاقات", "یادآوری", "وقت", "ویزیت"
        )
        val merged = JSONArray()
        val seen = HashSet<Long>()
        for (keyword in keywords) {
            val items = querySms(context, keyword, "", from, to, limit)
                .optJSONArray("messages") ?: continue
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optLong("id", -1L)
                if (id > 0L && seen.add(id)) merged.put(item)
            }
            if (merged.length() >= limit) break
        }
        return JSONObject().put("count", merged.length()).put("messages", merged)
    }

    private fun getSms(context: Context, args: JSONObject): JSONObject {
        val id = args.optLong("message_id", -1L)
        require(id > 0L) { "message_id is required" }
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection(),
            "${Telephony.Sms._ID} = ?",
            arrayOf(id.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return JSONObject().put("message", readMessage(cursor))
            }
        }
        return JSONObject()
            .put("message", JSONObject.NULL)
            .put("error", "SMS not found")
    }

    private fun draftReply(context: Context, args: JSONObject): JSONObject {
        val id = args.optLong("message_id", -1L)
        val source = getSms(context, JSONObject().put("message_id", id)).optJSONObject("message")
            ?: return JSONObject().put("error", "SMS not found")
        return JSONObject()
            .put("action", "draft_only")
            .put("message_id", id)
            .put("recipient", source.optString("address"))
            .put("source_message", source.optString("body"))
            .put("tone", args.optString("tone", "neutral"))
            .put("instruction", args.optString("instruction"))
            .put("note", "Create the reply in the assistant response. Do not send it automatically.")
    }

    private fun translateSms(context: Context, args: JSONObject): JSONObject {
        val id = args.optLong("message_id", -1L)
        val target = args.optString("target_language").trim()
        require(id > 0L) { "message_id is required" }
        require(target.isNotBlank()) { "target_language is required" }
        val source = getSms(context, JSONObject().put("message_id", id)).optJSONObject("message")
            ?: return JSONObject().put("error", "SMS not found")
        return JSONObject()
            .put("action", "translate_only")
            .put("message_id", id)
            .put("source_language", "auto")
            .put("target_language", target)
            .put("source_message", source.optString("body"))
            .put("preserve_names_numbers", args.optBoolean("preserve_names_numbers", true))
    }

    private fun findLocation(context: Context, args: JSONObject): JSONObject {
        val explicit = args.optString("location_text").trim()
        val messageId = args.optLong("message_id", -1L)
        val text = if (explicit.isNotBlank()) {
            explicit
        } else {
            require(messageId > 0L) { "message_id or location_text is required" }
            getSms(context, JSONObject().put("message_id", messageId))
                .optJSONObject("message")
                ?.optString("body")
                .orEmpty()
        }
        require(text.isNotBlank()) { "No location text found" }
        return JSONObject()
            .put("action", "map_search")
            .put("source_message_id", if (messageId > 0L) messageId else JSONObject.NULL)
            .put("location_text", text.take(2000))
            .put("coordinates", extractCoordinates(text) ?: JSONObject.NULL)
            .put("note", "Open the map only after explicit user action.")
    }

    private fun navigateToLocation(context: Context, args: JSONObject): JSONObject {
        val found = findLocation(context, args)
        val coordinates = found.optJSONObject("coordinates")
        val query = found.optString("location_text").take(500)
        val uri = if (coordinates != null) {
            val lat = coordinates.optDouble("latitude")
            val lon = coordinates.optDouble("longitude")
            "geo:$lat,$lon?q=$lat,$lon"
        } else {
            "geo:0,0?q=${URLEncoder.encode(query, Charsets.UTF_8.name())}"
        }
        return found
            .put("action", "open_navigation")
            .put("maps_uri", uri)
            .put("requires_user_confirmation", true)
            .put("note", "Navigation is proposal-only; do not open automatically.")
    }

    private fun requestSnappTaxi(context: Context, args: JSONObject): JSONObject {
        val destination = args.optString("destination").trim()
        val messageId = args.optLong("message_id", -1L)
        var resolved = destination
        var coordinates: JSONObject? = null

        if (resolved.isBlank() && messageId > 0L) {
            val source = getSms(context, JSONObject().put("message_id", messageId))
                .optJSONObject("message")
            resolved = source?.optString("body").orEmpty().take(2000)
            coordinates = extractCoordinates(resolved)
        }

        if (coordinates == null && args.has("latitude") && args.has("longitude")) {
            val lat = args.optDouble("latitude", Double.NaN)
            val lon = args.optDouble("longitude", Double.NaN)
            if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                coordinates = JSONObject()
                    .put("latitude", lat)
                    .put("longitude", lon)
            }
        }

        require(resolved.isNotBlank() || coordinates != null) {
            "destination or message_id is required"
        }

        return JSONObject()
            .put("action", "request_snapp_taxi_proposal")
            .put("provider", "Snapp")
            .put("pickup", "user_current_location")
            .put("destination", resolved)
            .put("coordinates", coordinates ?: JSONObject.NULL)
            .put("requires_user_confirmation", true)
            .put("requires_snapp_app", true)
            .put("note", "Never request, book, charge, or share location automatically.")
    }

    private fun extractCoordinates(text: String): JSONObject? {
        val regex = Regex("(-?\\d{1,3}\\.\\d{4,})\\s*[,; ]\\s*(-?\\d{1,3}\\.\\d{4,})")
        val match = regex.find(text) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return JSONObject()
            .put("latitude", lat)
            .put("longitude", lon)
    }

    private fun querySms(
        context: Context,
        query: String,
        sender: String,
        from: Long,
        to: Long,
        limit: Int
    ): JSONObject {
        val selection = ArrayList<String>()
        val values = ArrayList<String>()

        if (query.isNotBlank()) {
            selection += "(${Telephony.Sms.BODY} LIKE ? OR ${Telephony.Sms.ADDRESS} LIKE ?)"
            values += "%$query%"
            values += "%$query%"
        }
        if (sender.isNotBlank()) {
            selection += "${Telephony.Sms.ADDRESS} LIKE ?"
            values += "%$sender%"
        }
        if (from > 0L) {
            selection += "${Telephony.Sms.DATE} >= ?"
            values += from.toString()
        }
        if (to > 0L) {
            selection += "${Telephony.Sms.DATE} <= ?"
            values += to.toString()
        }

        val result = JSONArray()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection(),
            selection.takeIf { it.isNotEmpty() }?.joinToString(" AND "),
            values.toTypedArray().takeIf { it.isNotEmpty() },
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                result.put(readMessage(cursor))
            }
        }
        return JSONObject().put("count", result.length()).put("messages", result)
    }

    private fun projection() = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.TYPE,
        Telephony.Sms.READ,
        Telephony.Sms.THREAD_ID
    )

    private fun readMessage(cursor: android.database.Cursor): JSONObject {
        fun index(name: String): Int = cursor.getColumnIndex(name)

        val bodyIndex = index(Telephony.Sms.BODY)
        val body = if (bodyIndex >= 0) {
            cursor.getString(bodyIndex).orEmpty()
        } else {
            ""
        }

        val idIndex = index(Telephony.Sms._ID)
        val addressIndex = index(Telephony.Sms.ADDRESS)
        val dateIndex = index(Telephony.Sms.DATE)
        val typeIndex = index(Telephony.Sms.TYPE)
        val readIndex = index(Telephony.Sms.READ)
        val threadIndex = index(Telephony.Sms.THREAD_ID)

        return JSONObject()
            .put("id", if (idIndex >= 0) cursor.getLong(idIndex) else -1L)
            .put("address", if (addressIndex >= 0) cursor.getString(addressIndex).orEmpty() else "")
            .put("body", body.take(4000))
            .put("date", if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L)
            .put("type", if (typeIndex >= 0) cursor.getInt(typeIndex) else 0)
            .put("read", if (readIndex >= 0) cursor.getInt(readIndex) else 0)
            .put("thread_id", if (threadIndex >= 0) cursor.getLong(threadIndex) else 0L)
    }

    private fun prop(type: String, description: String = ""): JSONObject = JSONObject()
        .put("type", type)
        .put("description", description)

    private fun obj(
        vararg entries: Pair<String, JSONObject>,
        required: Array<String> = emptyArray()
    ): JSONObject = JSONObject().apply {
        put("type", "object")
        val properties = JSONObject()
        entries.forEach { (key, value) -> properties.put(key, value) }
        put("properties", properties)
        put("required", JSONArray(required))
    }

    private fun tool(name: String, description: String, parameters: JSONObject): JSONObject = JSONObject()
        .put("type", "function")
        .put(
            "function",
            JSONObject()
                .put("name", name)
                .put("description", description)
                .put("parameters", parameters)
        )

    private fun safeLimit(value: Int): Int = value.coerceIn(1, MAX_LIMIT)
}
