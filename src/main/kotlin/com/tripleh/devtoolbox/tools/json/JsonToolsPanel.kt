package com.tripleh.devtoolbox.tools.json

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.tripleh.devtoolbox.tools.ui.StatusPanel
import java.awt.BorderLayout
import java.awt.Font
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * JSON Tools: format, minify, escape and unescape JSON in a single editor.
 */
class JsonToolsPanel : JPanel(BorderLayout()) {

    private val editor = JBTextArea()
    private val status = StatusPanel()
    private val formatButton = JButton("Format")
    private val minifyButton = JButton("Minify")
    private val escapeButton = JButton("Escape")
    private val unescapeButton = JButton("Unescape")
    private val clearButton = JButton("Clear")
    private val indentField = JBTextField(INDENT_DEFAULT.toString(), 4)

    init {
        editor.lineWrap = false
        editor.tabSize = 2
        editor.font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(13f).toInt())
        editor.emptyText.text = "Paste or type JSON here…"
        editor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateStatus()
            override fun removeUpdate(e: DocumentEvent?) = updateStatus()
            override fun changedUpdate(e: DocumentEvent?) = updateStatus()
        })

        val editorPane = JPanel(BorderLayout())
        editorPane.add(JBLabel("Input"), BorderLayout.NORTH)
        editorPane.add(editor, BorderLayout.CENTER)
        add(editorPane, BorderLayout.CENTER)

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        toolbar.add(formatButton)
        toolbar.add(minifyButton)
        toolbar.add(escapeButton)
        toolbar.add(unescapeButton)
        toolbar.add(clearButton)
        toolbar.add(JBLabel("indent:"))
        toolbar.add(indentField)
        add(toolbar, BorderLayout.NORTH)
        add(status, BorderLayout.SOUTH)

        formatButton.addActionListener { transform { JsonFormat.minify(it)?.let { JsonFormat.indent(it, indent()) } } }
        minifyButton.addActionListener { transform { JsonFormat.minify(it) } }
        escapeButton.addActionListener { transform { JsonEscape.escape(it) } }
        unescapeButton.addActionListener { transform { JsonEscape.unescape(it) } }
        clearButton.addActionListener {
            editor.text = ""
            updateStatus()
        }
        indentField.addActionListener {
            val value = indent()
            indentField.text = value.toString()
            transform { JsonFormat.minify(it)?.let { JsonFormat.indent(it, value) } }
        }

        // Cmd/Ctrl+Enter formats, Cmd/Ctrl+Shift+Enter minifies.
        editor.inputMap.put(KeyStroke.getKeyStroke("meta ENTER"), "format")
        editor.inputMap.put(KeyStroke.getKeyStroke("control ENTER"), "format")
        editor.inputMap.put(KeyStroke.getKeyStroke("meta shift ENTER"), "minify")
        editor.inputMap.put(KeyStroke.getKeyStroke("control shift ENTER"), "minify")
        editor.actionMap.put("format", TransformAction { formatButton.doClick() })
        editor.actionMap.put("minify", TransformAction { minifyButton.doClick() })

        updateStatus()
    }

    private fun indent(): Int {
        val parsed = runCatching { indentField.text.trim().toInt() }.getOrNull()
        return if (parsed != null && parsed in INDENT_VALUES) parsed else INDENT_DEFAULT
    }

    /** Runs [transform] on the current text and replaces it on success; on failure shows the parse error. */
    private fun transform(transform: (String) -> String?) {
        val result = transform(editor.text)
        if (result == null) {
            updateStatus() // shows the "Invalid JSON" error
            return
        }
        val caret = editor.caretPosition
        editor.text = result
        editor.caretPosition = caret.coerceIn(0, result.length)
        updateStatus()
    }

    private fun updateStatus() {
        val text = editor.text
        val error = JsonFormat.validate(text)
        val lines = if (text.isBlank()) 0 else text.trim().count { it == '\n' } + 1
        status.update(text.length, lines, error)
    }

    /** Keyboard shortcuts only act when the editor holds something JSON-like. */
    private inner class TransformAction(private val action: () -> Unit) : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            val text = editor.text.trim()
            if (text.startsWith("{") || text.startsWith("[") || text.startsWith("\"")) action()
        }
    }

    private companion object {
        const val INDENT_DEFAULT = 2
        val INDENT_VALUES = setOf(2, 4)
    }
}
