package com.tripleh.devtoolbox.tools.json

import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerPosition
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.project.Project
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor

/** Element types produced by [JsonToolsLexer]. */
private object JsonTokenTypes {
    val KEY = IElementType("DEVTOOLBOX_JSON_KEY", null)
    val STRING = IElementType("DEVTOOLBOX_JSON_STRING", null)
    val NUMBER = IElementType("DEVTOOLBOX_JSON_NUMBER", null)
    val KEYWORD = IElementType("DEVTOOLBOX_JSON_KEYWORD", null)
    val PUNCTUATION = IElementType("DEVTOOLBOX_JSON_PUNCTUATION", null)
    val BAD = TokenType.BAD_CHARACTER
}

/**
 * Self-contained JSON highlighter for the JSON Tools editor. Deliberately does not depend
 * on the IDE's JSON plugin (which is optional and not a declared dependency); keys carry
 * JBColor defaults so both light and dark themes get sensible colors without scheme changes.
 */
class JsonToolsSyntaxHighlighter : SyntaxHighlighter {

    override fun getHighlightingLexer(): Lexer = JsonToolsLexer()

    override fun getTokenHighlights(type: IElementType?): Array<TextAttributesKey> = when (type) {
        JsonTokenTypes.KEY -> arrayOf(KEY_ATTR)
        JsonTokenTypes.STRING -> arrayOf(STRING_ATTR)
        JsonTokenTypes.NUMBER -> arrayOf(NUMBER_ATTR)
        JsonTokenTypes.KEYWORD -> arrayOf(KEYWORD_ATTR)
        JsonTokenTypes.BAD -> arrayOf(BAD_ATTR)
        else -> TextAttributesKey.EMPTY_ARRAY
    }

    companion object {
        private val KEY_ATTR = create("DEVTOOLBOX_JSON_KEY_DEFAULT", JBColor(0x174AD4, 0x56A8F5))
        private val STRING_ATTR = create("DEVTOOLBOX_JSON_STRING_DEFAULT", JBColor(0x067D17, 0x6A8759))
        private val NUMBER_ATTR = create("DEVTOOLBOX_JSON_NUMBER_DEFAULT", JBColor(0x1750EB, 0x6897BB))
        private val KEYWORD_ATTR = create("DEVTOOLBOX_JSON_KEYWORD_DEFAULT", JBColor(0x0033B3, 0xCC7832))
        private val BAD_ATTR = create("DEVTOOLBOX_JSON_BAD_DEFAULT", JBColor(0xFF0000, 0xF75464))

        /** Default attributes (JBColor adapts to light/dark) so the colors work without scheme registration. */
        private fun create(name: String, color: JBColor): TextAttributesKey =
            createTextAttributesKey(name, TextAttributes(color, null, null, null, 0))
    }
}

/**
 * Context-free JSON lexer: strings (with a ':' lookahead to tell keys from values), numbers,
 * true/false/null, punctuation and a catch-all bad token. Highlighting only — no validation.
 */
private class JsonToolsLexer : Lexer() {

    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var pos = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var type: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        pos = startOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        type = null
        advance()
    }

    override fun getState() = 0

    override fun getTokenType(): IElementType? = type

    override fun getTokenStart() = tokenStart

    override fun getTokenEnd() = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd() = endOffset

    override fun getCurrentPosition(): LexerPosition = object : LexerPosition {
        override fun getState() = 0
        override fun getOffset() = tokenStart
    }

    override fun restore(position: LexerPosition) {
        pos = position.offset
        tokenStart = pos
        tokenEnd = pos
        type = null
    }

    override fun advance() {
        tokenStart = pos
        type = null
        if (pos >= endOffset) {
            tokenEnd = pos
            return
        }
        when (val c = buffer[pos]) {
            '"' -> scanString()
            ' ', '\t', '\n', '\r' -> {
                while (pos < endOffset && buffer[pos].isWhitespace()) pos++
                tokenEnd = pos
                type = TokenType.WHITE_SPACE
            }
            '{', '}', '[', ']', ':', ',' -> {
                pos++
                tokenEnd = pos
                type = JsonTokenTypes.PUNCTUATION
            }
            '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                pos++
                while (pos < endOffset && buffer[pos] in NUMBER_CHARS) pos++
                tokenEnd = pos
                type = JsonTokenTypes.NUMBER
            }
            else -> scanKeywordOrBad(c)
        }
        tokenEnd = tokenEnd.coerceAtMost(endOffset)
    }

    private fun scanString() {
        pos++
        while (pos < endOffset) {
            when (val c = buffer[pos]) {
                '\\' -> pos += 2
                '"' -> { pos++; break }
                else -> pos++
            }
        }
        tokenEnd = pos
        var lookahead = pos
        while (lookahead < endOffset && buffer[lookahead].isWhitespace()) lookahead++
        type = if (lookahead < endOffset && buffer[lookahead] == ':') JsonTokenTypes.KEY else JsonTokenTypes.STRING
    }

    private fun scanKeywordOrBad(first: Char) {
        val rest = buffer.subSequence(pos, endOffset).toString()
        val keyword = when (first) {
            't' -> "true"
            'f' -> "false"
            'n' -> "null"
            else -> null
        }
        if (keyword != null && rest.startsWith(keyword)) {
            pos += keyword.length
            tokenEnd = pos
            type = JsonTokenTypes.KEYWORD
        } else {
            pos++
            tokenEnd = pos
            type = JsonTokenTypes.BAD
        }
    }

    private companion object {
        const val NUMBER_CHARS = "0123456789.eE+-"
    }
}

/**
 * Multi-line editor with line numbers and JSON syntax coloring, independent of the IDE's
 * optional JSON plugin (highlighter is installed directly, bypassing the file type lookup).
 */
internal class JsonEditorField(project: Project) : EditorTextField(
    EditorFactory.getInstance().createDocument(""), project, PlainTextFileType.INSTANCE, false, false
) {

    init {
        addSettingsProvider { editor: EditorEx ->
            editor.settings.isLineNumbersShown = true
            editor.settings.isLineMarkerAreaShown = false
            editor.settings.isFoldingOutlineShown = false
            editor.settings.isCaretRowShown = true
        }
    }

    override fun createEditor(): EditorEx {
        val ex = super.createEditor()
        ex.highlighter = createHighlighter()
        return ex
    }

    private fun createHighlighter(): EditorHighlighter =
        LexerEditorHighlighter(JsonToolsSyntaxHighlighter(), EditorColorsManager.getInstance().globalScheme)
}
