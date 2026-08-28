package com.tripleh.devtoolbox.tools.json

/**
 * JSON string escaping / unescaping without external dependencies.
 *
 * escape() operates on a whole JSON document, escaping only the character content
 * inside string literals (so structural braces / brackets stay intact). Non-JSON
 * input is validated first and returned as null.
 */
object JsonEscape {

    /** Returns the input with all string contents JSON-escaped, or null if not valid JSON. */
    fun escape(input: String): String? {
        if (input.isBlank()) return null
        val minified = JsonFormat.minify(input) ?: return null
        val sb = StringBuilder(minified.length + 16)
        var inString = false
        var i = 0
        val n = minified.length
        while (i < n) {
            val c = minified[i]
            if (c == '"' && !inString) {
                inString = true
                sb.append(c)
                i++
                continue
            }
            if (c == '"' && inString) {
                inString = false
                sb.append(c)
                i++
                continue
            }
            if (c == '\\' && inString) {
                // Copy existing escape sequences verbatim (they are already escaped).
                sb.append(c)
                i++
                if (i < n) {
                    sb.append(minified[i])
                    if (minified[i] == 'u' && i + 4 < n) {
                        sb.append(minified, i + 1, i + 5)
                        i += 4
                    }
                    i++
                }
                continue
            }
            if (inString) {
                sb.append(escapeChar(c))
                i++
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    /** Returns the input with escaped sequences inside string literals decoded, or null if invalid. */
    fun unescape(input: String): String? {
        if (input.isBlank()) return null
        val minified = JsonFormat.minify(input) ?: return null
        val sb = StringBuilder(minified.length)
        var inString = false
        var i = 0
        val n = minified.length
        while (i < n) {
            val c = minified[i]
            if (c == '"' && !inString) { inString = true; sb.append(c); i++; continue }
            if (c == '"' && inString) { inString = false; sb.append(c); i++; continue }
            if (c == '\\' && inString) {
                i++
                if (i >= n) break
                val esc = minified[i]
                when (esc) {
                    'u' -> {
                        if (i + 4 < n) {
                            val hex = minified.substring(i + 1, i + 5)
                            val code = hex.toIntOrNull(16)
                            if (code != null) sb.append(code.toChar()) else sb.append("\\u").append(hex)
                            i += 5
                        } else {
                            sb.append("\\u")
                            i++
                        }
                    }
                    'b' -> { sb.append('\b'); i++ }
                    'f' -> { sb.append('\u000c'); i++ }
                    'n' -> { sb.append('\n'); i++ }
                    'r' -> { sb.append('\r'); i++ }
                    't' -> { sb.append('\t'); i++ }
                    else -> { sb.append(esc); i++ }
                }
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun escapeChar(c: Char): String = when (c) {
        '"' -> "\\\""
        '\\' -> "\\\\"
        '\b' -> "\\b"
        '\u000c' -> "\\f"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        else -> if (c.code < 0x20) "\\u%04x".format(c.code) else c.toString()
    }
}
