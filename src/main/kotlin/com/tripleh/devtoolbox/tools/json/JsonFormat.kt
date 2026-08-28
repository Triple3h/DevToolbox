package com.tripleh.devtoolbox.tools.json

/**
 * Minimal recursive-descent JSON validator / formatter / minifier.
 *
 * No external JSON library is used on purpose: keeps the plugin dependency-free and
 * Qodana-warning-clean. The parser validates the whole document and builds a [JsonValue]
 * tree carrying each value's character range in the source; minify/indent are tree
 * serializers, so they are lossless and work on already-formatted input. Errors carry
 * 1-based line/column.
 */
object JsonFormat {

    /** Parses [input] into a value tree, or null when it is not valid JSON. */
    fun parse(input: String): JsonValue? = runCatching { Parser(input).parseDocument() }.getOrNull()

    fun validate(input: String): String? =
        runCatching { Parser(input).parseDocument() }.exceptionOrNull()?.message

    /**
     * Returns the minified JSON, or null when the input is not valid JSON.
     * Blank input is treated as "nothing to do" (null).
     */
    fun minify(input: String): String? {
        if (input.isBlank()) return null
        val value = parse(input) ?: return null
        return CompactSerializer.serialize(value)
    }

    /** Pretty-prints [input] with [indent] spaces per level; returns null if not valid JSON. */
    fun indent(input: String, indent: Int = 2): String? {
        if (input.isBlank()) return null
        val value = parse(input) ?: return null
        return PrettySerializer(indent.coerceIn(1, 8)).serialize(value)
    }

    private class Parser(private val input: String) {
        private var pos = 0
        private var line = 1
        private var col = 1

        fun parseDocument(): JsonValue {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
            if (pos < input.length) fail("Unexpected trailing content")
            return value
        }

        private fun parseValue(): JsonValue {
            if (pos >= input.length) fail("Unexpected end of input")
            val start = pos
            val value: JsonValue = when (input[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseStringValue()
                't' -> parseKeywordValue("true", JsonBooleanValue(true))
                'f' -> parseKeywordValue("false", JsonBooleanValue(false))
                'n' -> parseKeywordValue("null", JsonNullValue())
                else -> parseNumberValue()
            }
            value.start = start
            value.end = pos
            return value
        }

        private fun parseObject(): JsonObjectValue {
            val obj = JsonObjectValue()
            advance()
            skipWhitespace()
            if (peek() == '}') { advance(); return obj }
            while (true) {
                skipWhitespace()
                if (peek() != '"') fail("Expected a string key")
                val keyStart = pos
                parseString()
                val keyRaw = input.substring(keyStart, pos)
                skipWhitespace()
                if (peek() != ':') fail("Expected ':' after key")
                advance()
                skipWhitespace()
                val value = parseValue()
                obj.add(decodeString(keyRaw), keyRaw, value)
                skipWhitespace()
                when (peek()) {
                    ',' -> { advance(); skipWhitespace() }
                    '}' -> { advance(); return obj }
                    else -> fail("Expected ',' or '}'")
                }
            }
        }

        private fun parseArray(): JsonArrayValue {
            val array = JsonArrayValue()
            advance()
            skipWhitespace()
            if (peek() == ']') { advance(); return array }
            while (true) {
                skipWhitespace()
                array.items.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> { advance(); skipWhitespace() }
                    ']' -> { advance(); return array }
                    else -> fail("Expected ',' or ']'")
                }
            }
        }

        private fun parseStringValue(): JsonStringValue {
            val start = pos
            parseString()
            val raw = input.substring(start, pos)
            return JsonStringValue(raw, decodeString(raw))
        }

        private fun parseNumberValue(): JsonNumberValue {
            val start = pos
            if (peek() == '-') advance()
            if (peek() == '0') {
                advance()
                if (peek().isDigit()) fail("Leading zeros are not allowed")
            } else if (peek().isDigit()) {
                while (peek().isDigit()) advance()
            } else fail("Invalid number")
            if (peek() == '.') {
                advance()
                if (!peek().isDigit()) fail("Expected digit after decimal point")
                while (peek().isDigit()) advance()
            }
            if (peek() == 'e' || peek() == 'E') {
                advance()
                if (peek() == '+' || peek() == '-') advance()
                if (!peek().isDigit()) fail("Expected digit in exponent")
                while (peek().isDigit()) advance()
            }
            val number = input.substring(start, pos)
            if (number.toDoubleOrNull() == null) fail("Invalid number '$number'")
            return JsonNumberValue(number)
        }

        private fun parseKeywordValue(word: String, value: JsonValue): JsonValue {
            if (pos + word.length > input.length || input.substring(pos, pos + word.length) != word) {
                fail("Invalid literal")
            }
            repeat(word.length) { advance() }
            return value
        }

        private fun parseString() {
            advance() // opening quote
            while (true) {
                if (pos >= input.length) fail("Unterminated string")
                val c = input[pos]
                when (c) {
                    '"' -> { advance(); return }
                    '\\' -> {
                        advance()
                        if (pos >= input.length) fail("Unterminated escape sequence")
                        val esc = input[pos]
                        if (esc !in "\\\"/bfnrtu") fail("Invalid escape sequence '\\$esc'")
                        if (esc == 'u') {
                            if (pos + 4 >= input.length) fail("Incomplete Unicode escape")
                            val hex = input.substring(pos + 1, pos + 5)
                            if (!hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                                fail("Invalid Unicode escape '\\u$hex'")
                            }
                            pos += 4
                        }
                        advance()
                    }
                    else -> advance()
                }
            }
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) advance()
        }

        private fun advance() {
            if (pos < input.length) {
                if (input[pos] == '\n') { line++; col = 1 } else col++
                pos++
            }
        }

        private fun peek(): Char = if (pos < input.length) input[pos] else '\u0000'

        private fun fail(message: String): Nothing {
            throw IllegalArgumentException("Invalid JSON at line $line, column $col: $message")
        }
    }

    /** Decodes a raw JSON string slice (including the quotes) to its text value. */
    private fun decodeString(raw: String): String {
        val inner = raw.substring(1, raw.length - 1)
        if ('\\' !in inner) return inner
        val sb = StringBuilder(inner.length)
        var i = 0
        while (i < inner.length) {
            val c = inner[i]
            if (c != '\\') { sb.append(c); i++; continue }
            i++
            if (i >= inner.length) break
            when (val esc = inner[i]) {
                '"' -> sb.append('"')
                '\\' -> sb.append('\\')
                '/' -> sb.append('/')
                'b' -> sb.append('\b')
                'f' -> sb.append('\u000C')
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                't' -> sb.append('\t')
                'u' -> { sb.append(inner.substring(i + 1, i + 5).toInt(16).toChar()); i += 4 }
                else -> sb.append(esc) // unreachable: the parser already validated escapes
            }
            i++
        }
        return sb.toString()
    }

    private object CompactSerializer {
        fun serialize(value: JsonValue): String {
            val sb = StringBuilder()
            write(value, sb)
            return sb.toString()
        }

        internal fun write(value: JsonValue, sb: StringBuilder) {
            when (value) {
                is JsonObjectValue -> {
                    sb.append('{')
                    value.members.forEachIndexed { i, m ->
                        if (i > 0) sb.append(',')
                        sb.append(m.keyRaw).append(':')
                        write(m.value, sb)
                    }
                    sb.append('}')
                }
                is JsonArrayValue -> {
                    sb.append('[')
                    value.items.forEachIndexed { i, item ->
                        if (i > 0) sb.append(',')
                        write(item, sb)
                    }
                    sb.append(']')
                }
                is JsonStringValue -> sb.append(value.raw)
                is JsonNumberValue -> sb.append(value.raw)
                is JsonBooleanValue -> sb.append(value.value)
                is JsonNullValue -> sb.append("null")
            }
        }
    }

    private class PrettySerializer(private val width: Int) {
        fun serialize(value: JsonValue): String {
            val sb = StringBuilder()
            write(value, 0, sb)
            return sb.toString()
        }

        private fun write(value: JsonValue, depth: Int, sb: StringBuilder) {
            when (value) {
                is JsonObjectValue -> writeContainer(sb, depth, value.members.size, '{', '}') { i ->
                    sb.append(pad(depth + 1)).append(value.members[i].keyRaw).append(": ")
                    write(value.members[i].value, depth + 1, sb)
                }
                is JsonArrayValue -> writeContainer(sb, depth, value.items.size, '[', ']') { i ->
                    sb.append(pad(depth + 1))
                    write(value.items[i], depth + 1, sb)
                }
                else -> CompactSerializer.write(value, sb)
            }
        }

        private inline fun writeContainer(sb: StringBuilder, depth: Int, size: Int, open: Char, close: Char, writeItem: (Int) -> Unit) {
            if (size == 0) {
                sb.append(open).append(close)
                return
            }
            sb.append(open).append('\n')
            for (i in 0 until size) {
                if (i > 0) sb.append(',').append('\n')
                writeItem(i)
            }
            sb.append('\n').append(pad(depth)).append(close)
        }

        private fun pad(depth: Int): String = " ".repeat(depth * width)
    }
}
