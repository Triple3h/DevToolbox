package com.tripleh.devtoolbox.tools.json

/**
 * Minimal recursive-descent JSON validator / formatter / minifier.
 *
 * No external JSON library is used on purpose: keeps the plugin dependency-free and
 * Qodana-warning-clean. The parser validates the whole document and produces
 * human-readable errors with 1-based line/column; it does not build a value tree.
 */
object JsonFormat {

    fun validate(input: String): String? = runCatching {
        Parser(input).run()
    }.exceptionOrNull()?.message

    /**
     * Returns the minified JSON, or null when the input is not valid JSON.
     * Blank input is treated as "nothing to do" (null).
     */
    fun minify(input: String): String? {
        if (input.isBlank()) return null
        return runCatching { Parser(input).run() }.getOrNull()
    }

    /** Indents minified (or compact) JSON with [indent] spaces; returns null if not valid JSON. */
    fun indent(input: String, indent: Int = 2): String? {
        val minified = minify(input) ?: return null
        val sb = StringBuilder()
        var depth = 0
        var inString = false
        var i = 0
        val n = minified.length
        while (i < n) {
            val c = minified[i]
            when {
                c == '"' && !inString -> { inString = true; sb.append(c); i++ }
                c == '\\' && inString -> { sb.append(c).append(minified.getOrNull(i + 1) ?: '\\'); i += 2 }
                c == '"' && inString -> { inString = false; sb.append(c); i++ }
                !inString && (c == '{' || c == '[') -> {
                    sb.append(c)
                    if (i + 1 < n && minified[i + 1] != '}' && minified[i + 1] != ']') {
                        depth++
                        sb.append('\n').append(" ".repeat(depth * indent))
                    }
                    i++
                }
                !inString && (c == '}' || c == ']') -> {
                    if (i > 0 && minified[i - 1] != '{' && minified[i - 1] != '[' &&
                        minified[i - 1] != ',' && minified[i - 1] != ':'
                    ) {
                        sb.append('\n')
                    }
                    depth = (depth - 1).coerceAtLeast(0)
                    sb.append(" ".repeat(depth * indent)).append(c)
                    i++
                }
                !inString && c == ',' -> { sb.append(c).append('\n').append(" ".repeat(depth * indent)); i++ }
                !inString && c == ':' -> { sb.append(c).append(' '); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }

    private class Parser(private val input: String) {
        private var pos = 0
        private var line = 1
        private var col = 1

        fun run(): String {
            skipWhitespace()
            parseValue()
            skipWhitespace()
            if (pos < input.length) fail("Unexpected trailing content")
            return input
        }

        private fun parseValue() {
            if (pos >= input.length) fail("Unexpected end of input")
            when (input[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> parseKeyword("true")
                'f' -> parseKeyword("false")
                'n' -> parseKeyword("null")
                else -> parseNumber()
            }
        }

        private fun parseObject() {
            advance()
            skipWhitespace()
            if (peek() == '}') { advance(); return }
            while (true) {
                skipWhitespace()
                if (peek() != '"') fail("Expected a string key")
                parseString()
                skipWhitespace()
                if (peek() != ':') fail("Expected ':' after key")
                advance()
                skipWhitespace()
                parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> { advance(); skipWhitespace() }
                    '}' -> { advance(); return }
                    else -> fail("Expected ',' or '}'")
                }
            }
        }

        private fun parseArray() {
            advance()
            skipWhitespace()
            if (peek() == ']') { advance(); return }
            while (true) {
                skipWhitespace()
                parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> { advance(); skipWhitespace() }
                    ']' -> { advance(); return }
                    else -> fail("Expected ',' or ']'")
                }
            }
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
                        if (esc !in "\\\"bfnrtu") fail("Invalid escape sequence '\\$esc'")
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

        private fun parseNumber() {
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
        }

        private fun parseKeyword(word: String) {
            if (pos + word.length > input.length || input.substring(pos, pos + word.length) != word) {
                fail("Invalid literal")
            }
            repeat(word.length) { advance() }
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
}
