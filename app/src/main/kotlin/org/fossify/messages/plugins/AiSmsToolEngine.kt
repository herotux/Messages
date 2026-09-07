package org.fossify.messages.plugins

import android.content.Context
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject

/** Local, read-only tools exposed to the AI Assistant. No tool can send/delete SMS. */
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
    }

    fun execute(context: Context, name: String, args: JSONObject): JSONObject = try {
        when (name) {
            "search_sms" -> searchSms(context, args)
            "get_conversation" -> getConversation(context, args)
            "find_appointments" -> findAppointments(context, args)
            "get_sms" -> getSms(context, args)
            "draft_reply" -> draftReply(context, args)
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
