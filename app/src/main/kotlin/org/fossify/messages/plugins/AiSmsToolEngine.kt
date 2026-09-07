package org.fossify.messages.plugins

import android.content.Context
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Local tools exposed to the AI Assistant. Destructive/external actions are proposal-only. */
object AiSmsToolEngine {
    private const val DEFAULT_LIMIT = 20
    private const val MAX_LIMIT = 50

    fun definitions(): JSONArray = JSONArray().apply {
        put(tool("search_sms", "Search the device SMS database by text, sender, and optional time range. Returns only matching message metadata and excerpts.", JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("query", property("string", "Text to search in sender or message body."))
                .put("sender", property("string", "Optional sender/address filter."))
                .put("date_from", property("integer", "Optional Unix timestamp in milliseconds; inclusive."))
                .put("date_to", property("integer", "Optional Unix timestamp in milliseconds; inclusive."))
                .put("limit", property("integer", "Maximum results, 1-50.")))
            .put("required", JSONArray())))
        put(tool("get_conversation", "Read recent SMS messages for one sender/address. Use this when the user asks about a conversation.", JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("sender", property("string", "Phone number, short code, or sender address."))
                .put("limit", property("integer", "Maximum messages, 1-50.")))
            .put("required", JSONArray().put("sender"))))
        put(tool("find_appointments", "Find likely appointment, meeting, reservation, delivery, or reminder SMS messages in a time range. The AI must extract the final date/time from the returned messages and should never invent missing details.", JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("date_from", property("integer", "Optional Unix timestamp in milliseconds; inclusive."))
                .put("date_to", property("integer", "Optional Unix timestamp in milliseconds; inclusive."))
                .put("limit", property("integer", "Maximum results, 1-50.")))
            .put("required", JSONArray())))
        put(tool("get_sms", "Read one exact SMS by its database id. Use only when an exact source message is needed.", JSONObject()
            .put("type", "object")
            .put("properties", JSONObject().put("message_id", property("integer", "SMS database _id.")))
            .put("required", JSONArray().put("message_id"))))
        put(tool("draft_reply", "Prepare a reply draft from an SMS. This tool never sends a message.", JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("message_id", property("integer", "SMS database _id to reply to."))
                .put("tone", property("string", "Tone such as friendly, formal, concise, or neutral."))
                .put("instruction", property("string", "Optional user instruction for the draft.")))
            .put("required", JSONArray().put("message_id"))))
        put(tool("translate_sms", "Translate one exact SMS. Returns the source text and translation instructions; the AI produces the final translation. This tool never sends or edits SMS.", JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("message_id", property("integer", "SMS database _id."))
                .put("target_language", property("string", "Target language, for example Persian, English, Arabic, Turkish."))
                .put("preserve_names_numbers", property("boolean", "Keep names, phone numbers, codes and URLs unchanged when true.")))
            .put("required", JSONArray().put("message_id").put("target_language"))))
        put(tool("find_location", "Find a map location in an SMS. Prefer explicit latitude/longitude coordinates when present; otherwise return the location text for map search. This tool does not silently share device location.", JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("message_id", property("integer", "SMS database _id containing the location."))
                .put("location_text", property("string", "Optional explicit place/address to search instead of reading an SMS.")))
            .put("required", JSONArray())))
        put(tool("navigate_to_location", "Prepare a navigation action for a location found in an SMS. Returns a geo/maps URI that the app can open after explicit user confirmation. It never starts navigation automatically.", JSONObject()
            .put("type", "object")
            .put("message_id", property("integer", "SMS database _id containing the destination."))
            .put("location_text", property("string", "Optional destination text.")))
            .put("required", JSONArray())))
        put(tool("request_snapp_taxi", "Prepare a Snapp taxi request to a destination from the user's current location. This is proposal-only: the tool never requests a taxi, charges money, or shares location automatically. The user must confirm and the app must open Snapp explicitly.", JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("message_id", property("integer", "Optional SMS database _id containing the destination."))
                .put("destination", property("string", "Destination address or place name."))
                .put("latitude", property("number", "Optional destination latitude."))
                .put("longitude", property("number", "Optional destination longitude."))))
            .put("required", JSONArray())))
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
        JSONObject().put("error", "SMS access is not available. The app must have the required SMS access.")
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
        val keywords = listOf("appointment", "meeting", "reservation", "booking", "schedule", "reminder", "نوبت", "قرار", "جلسه", "رزرو", "ملاقات", "یادآوری", "وقت", "ویزیت")
        val merged = JSONArray()
        val seen = HashSet<Long>()
        for (keyword in keywords) {
            val items = querySms(context, keyword, "", from, to, limit).optJSONArray("messages") ?: continue
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optLong("id", -1L)
                if (id > 0 && seen.add(id)) merged.put(item)
            }
            if (merged.length() >= limit) break
        }
        return JSONObject().put("count", merged.length()).put("messages", merged)
    }

    private fun getSms(context: Context, args: JSONObject): JSONObject {
        val id = args.optLong("message_id", -1L)
        require(id > 0) { "message_id is required" }
        val projection = projection()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms._ID} = ?",
            arrayOf(id.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return JSONObject().put("message", readMessage(cursor))
        }
        return JSONObject().put("message", JSONObject.NULL).put("error", "SMS not found")
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
        require(id > 0) { "message_id is required" }
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
        val text = if (explicit.isNotBlank()) explicit else {
            require(messageId > 0) { "message_id or location_text is required" }
            getSms(context, JSONObject().put("message_id", messageId)).optJSONObject("message")?.optString("body").orEmpty()
        }
        require(text.isNotBlank()) { "No location text found" }
        val coordinates = extractCoordinates(text)
        return JSONObject()
            .put("action", "map_search")
            .put("source_message_id", if (messageId > 0) messageId else JSONObject.NULL)
            .put("location_text", text.take(2000))
            .put("coordinates", coordinates ?: JSONObject.NULL)
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
            "geo:0,0?q=${java.net.URLEncoder.encode(query, Charsets.UTF_8.name())}"
        }
        return found
            .put("action", "open_navigation")
            .put("maps_uri", uri)
            .put("requires_user_confirmation", true)
            .put("note", "This is a navigation proposal. Do not open or navigate automatically.")
    }

    private fun requestSnappTaxi(context: Context, args: JSONObject): JSONObject {
        val destination = args.optString("destination").trim()
        val messageId = args.optLong("message_id", -1L)
        var resolvedDestination = destination
        var coordinates: JSONObject? = null
        if (resolvedDestination.isBlank() && messageId > 0) {
            val source = getSms(context, JSONObject().put("message_id", messageId)).optJSONObject("message")
            resolvedDestination = source?.optString("body").orEmpty().take(2000)
            coordinates = extractCoordinates(resolvedDestination)
        }
        if (coordinates == null && args.has("latitude") && args.has("longitude")) {
            val lat = args.optDouble("latitude", Double.NaN)
            val lon = args.optDouble("longitude", Double.NaN)
            if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                coordinates = JSONObject().put("latitude", lat).put("longitude", lon)
            }
        }
        require(resolvedDestination.isNotBlank() || coordinates != null) { "destination or message_id is required" }
        return JSONObject()
            .put("action", "request_snapp_taxi_proposal")
            .put("provider", "Snapp")
            .put("pickup", "user_current_location")
            .put("destination", resolvedDestination)
            .put("coordinates", coordinates ?: JSONObject.NULL)
            .put("requires_user_confirmation", true)
            .put("requires_snapp_app", true)
            .put("note", "Never request, book, charge, or share the user's location automatically. Ask the user to confirm and open Snapp explicitly.")
    }

    private fun extractCoordinates(text: String): JSONObject? {
        val regex = Regex("(-?\\d{1,3}\\.\\d{4,})\\s*[,; ]\\s*(-?\\d{1,3}\\.\\d{4,})")
        val match = regex.find(text) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return JSONObject().put("latitude", lat).put("longitude", lon)
    }

    private fun querySms(context: Context, query: String, sender: String, from: Long, to: Long, limit: Int): JSONObject {
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
        if (from > 0) {
            selection += "${Telephony.Sms.DATE} >= ?"
            values += from.toString()
        }
        if (to > 0) {
            selection += "${Telephony.Sms.DATE} <= ?"
            values += to.toString()
        }
        val projection = projection()
        val result = JSONArray()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection.takeIf { it.isNotEmpty() }?.joinToString(" AND "),
            values.toTypedArray().takeIf { it.isNotEmpty() },
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        )?.use { cursor -> while (cursor.moveToNext()) result.put(readMessage(cursor)) }
        return JSONObject().put("count", result.length()).put("messages", result)
    }

    private fun projection() = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ, Telephony.Sms.THREAD_ID)

    private fun readMessage(cursor: android.database.Cursor): JSONObject {
        fun index(name: String) = cursor.getColumnIndex(name)
        val bodyIndex = index(Telephony.Sms.BODY)
        val body = if (bodyIndex >= 0) cursor.getString(bodyIndex).orEmpty() else ""
        return JSONObject()
            .put("id", if (index(Telephony.Sms._ID) >= 0) cursor.getLong(index(Telephony.Sms._ID)) else -1L)
            .put("address", if (index(Telephony.Sms.ADDRESS) >= 0) cursor.getString(index(Telephony.Sms.ADDRESS)).orEmpty() else "")
            .put("body", body.take(4000))
            .put("date", if (index(Telephony.Sms.DATE) >= 0) cursor.getLong(index(Telephony.Sms.DATE)) else 0L)
            .put("type", if (index(Telephony.Sms.TYPE) >= 0) cursor.getInt(index(Telephony.Sms.TYPE)) else 0)
            .put("read", if (index(Telephony.Sms.READ) >= 0) cursor.getInt(index(Telephony.Sms.READ)) else 0)
            .put("thread_id", if (index(Telephony.Sms.THREAD_ID) >= 0) cursor.getLong(index(Telephony.Sms.THREAD_ID)) else 0L)
    }

    private fun property(type: String, description: String): JSONObject = JSONObject().put("type", type).put("description", description)

    private fun tool(name: String, description: String, parameters: JSONObject): JSONObject = JSONObject().put("type", "function").put("function", JSONObject().put("name", name).put("description", description).put("parameters", parameters))

    private fun safeLimit(value: Int) = value.coerceIn(1, MAX_LIMIT)
}
