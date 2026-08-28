package com.tripleh.devtoolbox.tools.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonFormatTest {

    @Test
    fun parseBuildsTreeWithSourceOffsets() {
        val input = """{"a": 1, "b": [true, null]}"""
        val obj = JsonFormat.parse(input) as? JsonObjectValue
        assertNotNull(obj)
        obj!!
        assertEquals(listOf("a", "b"), obj.members.map { it.key })
        val number = obj.members[0].value as JsonNumberValue
        assertEquals("1", number.raw)
        assertEquals(6, number.start)
        assertEquals(7, number.end)
        val array = obj.members[1].value as JsonArrayValue
        assertEquals(2, array.items.size)
        assertEquals(true, (array.items[0] as JsonBooleanValue).value)
        assertTrue(array.items[1] is JsonNullValue)
        assertEquals(input, input.substring(obj.start, obj.end))
    }

    @Test
    fun parseDecodesEscapedKeysAndStrings() {
        val input = """{"ké\u0059": "line\nbreak"}"""
        val obj = JsonFormat.parse(input) as JsonObjectValue
        assertEquals("kéY", obj.members[0].key)
        assertEquals("line\nbreak", (obj.members[0].value as JsonStringValue).value)
    }

    @Test
    fun minifyStripsInsignificantWhitespace() {
        val pretty = "{\n  \"a\" : 1 ,\n  \"b\" : [ 1 , 2 ]\n}"
        assertEquals("""{"a":1,"b":[1,2]}""", JsonFormat.minify(pretty))
    }

    @Test
    fun minifyPreservesStringContentAndEscapes() {
        val input = """{ "s": "a b  { x , 1 } \" q \n" }"""
        assertEquals("""{"s":"a b  { x , 1 } \" q \n"}""", JsonFormat.minify(input))
    }

    @Test
    fun indentPrettyPrintsAndIsIdempotent() {
        val input = """{"a":1,"b":{"c":[1,2],"d":{}},"e":[]}"""
        val pretty = JsonFormat.indent(input, 2)!!
        assertEquals(
            """
            {
              "a": 1,
              "b": {
                "c": [
                  1,
                  2
                ],
                "d": {}
              },
              "e": []
            }
            """.trimIndent(),
            pretty
        )
        // Re-formatting already-formatted JSON must not double-indent (regression guard).
        assertEquals(pretty, JsonFormat.indent(pretty, 2))
    }

    @Test
    fun minifyAndIndentRoundTrip() {
        val input = """{"x":[1,{"y":"z"},null],"w":false}"""
        val minified = JsonFormat.minify(input)!!
        assertEquals(input, minified)
        assertEquals(minified, JsonFormat.minify(JsonFormat.indent(input, 4)!!))
    }

    @Test
    fun invalidInputYieldsNullAndMessage() {
        assertNull(JsonFormat.parse("""{"a": }"""))
        assertNull(JsonFormat.minify("[1, 2"))
        assertNull(JsonFormat.indent("{unquoted}"))
        val message = JsonFormat.validate("""{"a": 1,}""")
        assertNotNull(message)
        assertTrue(message!!.contains("line"))
    }

    @Test
    fun blankInputIsNothingToDo() {
        assertNull(JsonFormat.minify("   "))
        assertNull(JsonFormat.indent(" "))
    }
}
