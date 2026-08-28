package com.tripleh.devtoolbox.tools.json

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.openapi.Disposable
import com.tripleh.devtoolbox.tools.ui.StatusPanel
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.Timer

/**
 * JSON Tools content of one sub-tab: a syntax-colored editor (left) and a live collapsible
 * tree view (right) fed by a debounced re-parse of the document. Format/Minify/Escape/
 * Unescape rewrite the document; every sub-tab instance owns an independent document.
 */
class JsonToolsPanel(private val project: Project, parentDisposable: Disposable) : JPanel(BorderLayout()) {

    private val editor = JsonEditorField(project)
    private val treeView = JsonTreeView()
    private val status = StatusPanel()
    private val formatButton = JButton("Format")
    private val minifyButton = JButton("Minify")
    private val escapeButton = JButton("Escape")
    private val unescapeButton = JButton("Unescape")
    private val clearButton = JButton("Clear")
    private val indentCombo = ComboBox(arrayOf("2", "4"))
    private val refreshTimer = Timer(REFRESH_DELAY_MS) { refreshTreeAndStatus() }.apply { isRepeats = false }

    init {
        editor.setPlaceholder("Paste or type JSON here…")
        editor.addSettingsProvider { ex ->
            ex.settings.isLineNumbersShown = true
            installShortcuts(ex.contentComponent)
        }
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = scheduleRefresh()
        }, parentDisposable)
        treeView.selectionConsumer = { start, end -> selectRangeInEditor(start, end) }

        val leftPane = JPanel(BorderLayout())
        leftPane.add(JBLabel("Input"), BorderLayout.NORTH)
        leftPane.add(editor, BorderLayout.CENTER)

        val split = OnePixelSplitter(false, SPLIT_PROPORTION).apply {
            firstComponent = leftPane
            secondComponent = treeView
        }

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        toolbar.add(formatButton)
        toolbar.add(minifyButton)
        toolbar.add(escapeButton)
        toolbar.add(unescapeButton)
        toolbar.add(clearButton)
        toolbar.add(JBLabel("indent:"))
        toolbar.add(indentCombo)

        add(toolbar, BorderLayout.NORTH)
        add(split, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
        border = JBUI.Borders.empty(4)

        formatButton.addActionListener { transform { JsonFormat.indent(it, indent()) } }
        minifyButton.addActionListener { transform { JsonFormat.minify(it) } }
        escapeButton.addActionListener { transform { JsonEscape.escape(it) } }
        unescapeButton.addActionListener { transform { JsonEscape.unescape(it) } }
        clearButton.addActionListener { transform { "" } }
        indentCombo.addActionListener {
            if (editor.text.isNotBlank()) formatButton.doClick()
        }

        refreshTreeAndStatus()
    }

    private fun indent(): Int = when (indentCombo.selectedItem) {
        "4" -> 4
        else -> 2
    }

    private fun scheduleRefresh() {
        refreshTimer.restart()
    }

    private fun refreshTreeAndStatus() {
        val text = editor.document.text
        val error = JsonFormat.validate(text)
        val value = if (error == null) JsonFormat.parse(text) else null
        val lines = if (text.isBlank()) 0 else text.trim().count { it == '\n' } + 1
        status.update(text.length, lines, error)
        treeView.show(value, text.isBlank())
    }

    /** Runs [transform] on the current text and replaces the document on success; on failure shows the parse error. */
    private fun transform(transform: (String) -> String?) {
        val result = transform(editor.document.text) ?: run {
            refreshTreeAndStatus() // shows the "Invalid JSON" error
            return
        }
        val caret = editor.editor?.caretModel?.offset ?: 0
        val document = editor.document
        WriteCommandAction.runWriteCommandAction(project) { document.setText(result) }
        editor.editor?.caretModel?.moveToOffset(caret.coerceAtMost(result.length))
        refreshTreeAndStatus()
    }

    private fun selectRangeInEditor(start: Int, end: Int) {
        val ex = editor.editor ?: return
        ex.selectionModel.setSelection(start, end)
        ex.caretModel.moveToOffset(end)
        ex.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
    }

    /** Cmd/Ctrl+Enter formats, Cmd/Ctrl+Shift+Enter minifies. */
    private fun installShortcuts(component: JComponent) {
        component.inputMap.put(KeyStroke.getKeyStroke("meta ENTER"), "format")
        component.inputMap.put(KeyStroke.getKeyStroke("control ENTER"), "format")
        component.inputMap.put(KeyStroke.getKeyStroke("meta shift ENTER"), "minify")
        component.inputMap.put(KeyStroke.getKeyStroke("control shift ENTER"), "minify")
        component.actionMap.put("format", TransformAction { formatButton.doClick() })
        component.actionMap.put("minify", TransformAction { minifyButton.doClick() })
    }

    /** Keyboard shortcuts only act when the editor holds something JSON-like. */
    private inner class TransformAction(private val action: () -> Unit) : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            val text = editor.document.text.trim()
            if (text.startsWith("{") || text.startsWith("[") || text.startsWith("\"")) action()
        }
    }

    private companion object {
        const val REFRESH_DELAY_MS = 300
        const val SPLIT_PROPORTION = 0.55f
    }
}
