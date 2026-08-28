package com.tripleh.devtoolbox.tools.json

/**
 * Plain-text escaping / unescaping without external dependencies.
 *
 * The whole editor content is treated as a single string: Escape converts every special
 * character (quotes, backslashes, newlines, tabs, control chars) into its JSON escape
 * sequence, Unescape decodes them back. No JSON validation is required — a document that
 * is already valid JSON is simply escaped as a string literal, which is the intended use
 * (turn `{"a": 1}` into `{\"a\": 1}`). Blank input is left as-is.
 */
object JsonEscape {

    /** Returns the input with all special characters JSON-escaped; blank input stays blank. */
    fun escape(input: String): String = if (input.isEmpty()) input else escapeContent(input)

    /** Returns the input with JSON escape sequences decoded; blank input stays blank. */
    fun unescape(input: String): String = if (input.isEmpty()) input else unescapeContent(input)

    private fun escapeContent(input: String): String {
        val sb = StringBuilder(input.length + 16)
        for (ch in input) {
            sb.append(
                when (ch) {
                    '"' -> "\\\""
                    '\\' -> "\\\\"
                    '\b' -> "\\b"
                    '\u000c' -> "\\f"
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\t' -> "\\t"
                    else -> if (ch.code < 0x20) "\\u%04x".format(ch.code) else ch.toString()
                }
            )
        }
        return sb.toString()
    }

    private fun unescapeContent(input: String): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c != '\\') { sb.append(c); i++; continue }
            i++
            if (i >= input.length) { sb.append('\\'); break }
            when (val esc = input[i]) {
                'u' -> {
                    val hex = input.substring(i + 1, (i + 5).coerceAtMost(input.length))
                    val code = hex.toIntOrNull(16)
                    if (code != null) sb.append(code.toChar()) else sb.append("\\u").append(hex)
                    i += if (code != null) 5 else 1
                }
                'b' -> { sb.append('\b'); i++ }
                'f' -> { sb.append('\u000c'); i++ }
                'n' -> { sb.append('\n'); i++ }
                'r' -> { sb.append('\r'); i++ }
                't' -> { sb.append('\t'); i++ }
                '"' -> { sb.append('"'); i++ }
                '\\' -> { sb.append('\\'); i++ }
                else -> { sb.append('\\').append(esc); i++ }
            }
        }
        return sb.toString()
    }
}
