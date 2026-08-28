package com.tripleh.devtoolbox.tools.json

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonEscapeTest {

    @Test
    fun escapeEscapesQuotesAndBackslashes() {
        val input = """{"type":"FormQuestionCard_v1","id":"x"}"""
        assertEquals(
            """{\"type\":\"FormQuestionCard_v1\",\"id\":\"x\"}""",
            JsonEscape.escape(input)
        )
    }

    @Test
    fun escapeEscapesControlChars() {
        val input = "a\nb\tc\"d"
        assertEquals("""a\nb\tc\"d""", JsonEscape.escape(input))
    }

    @Test
    fun unescapeDecodesSequences() {
        val input = """a\nb\tc\"d\\e\u4f60"""
        assertEquals("a\nb\tc\"d\\e你", JsonEscape.unescape(input))
    }

    @Test
    fun escapeEscapesLiteralBackslash() {
        // A literal backslash followed by 'n' in the source is two chars (\ and n);
        // escaping turns the backslash into \\, so it reads \\n (a literal backslash + n).
        val input = """line\nbreak"""
        assertEquals("""line\\nbreak""", JsonEscape.escape(input))
    }

    @Test
    fun escapeAndUnescapeRoundTrip() {
        val input = """{"a": "x", "n": "1\n2", "t": "a\tb"}"""
        assertEquals(input, JsonEscape.unescape(JsonEscape.escape(input)))
    }

    @Test
    fun blankInputStaysBlank() {
        assertEquals("", JsonEscape.escape(""))
        assertEquals("", JsonEscape.unescape(""))
        assertEquals("   ", JsonEscape.escape("   "))
        assertEquals("   ", JsonEscape.unescape("   "))
    }
}
