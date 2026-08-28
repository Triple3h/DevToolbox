package com.tripleh.devtoolbox.tools.json

/**
 * In-memory JSON value tree produced by [JsonFormat.parse].
 *
 * [start]/[end] record the value's character range in the parsed source ([end] exclusive),
 * so UI code can map tree nodes back to editor selections. Strings keep both the raw
 * source slice (with quotes and escapes, for lossless re-serialization) and the decoded
 * value (for display).
 */
sealed class JsonValue {
    var start: Int = 0
        internal set
    var end: Int = 0
        internal set
}

class JsonObjectValue : JsonValue() {

    val members = mutableListOf<Member>()

    fun add(key: String, keyRaw: String, value: JsonValue) = members.add(Member(key, keyRaw, value))

    class Member(val key: String, val keyRaw: String, val value: JsonValue)
}

class JsonArrayValue : JsonValue() {
    val items = mutableListOf<JsonValue>()
}

class JsonStringValue(val raw: String, val value: String) : JsonValue()

class JsonNumberValue(val raw: String) : JsonValue()

class JsonBooleanValue(val value: Boolean) : JsonValue()

class JsonNullValue : JsonValue()
