package com.tripleh.devtoolbox.tools.diff

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.tripleh.devtoolbox.tools.ui.StatusPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/**
 * Side-by-side text comparison. Lines that were added (only in the right pane) or
 * removed (only in the left pane) are highlighted; clicking a diff line scrolls the
 * opposite pane to the matching line; Alt+Up / Alt+Down jump between diff blocks.
 */
class TextDiffPanel : JPanel(BorderLayout()) {

    private val leftEditor = JTextPane()
    private val rightEditor = JTextPane()
    private val leftScroll = JBScrollPane(leftEditor)
    private val rightScroll = JBScrollPane(rightEditor)
    private val swapButton = JButton("Swap ⇄")
    private val status = StatusPanel()

    private var blocks: List<DiffBlock> = emptyList()

    init {
        val mono = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scaleFontSize(13f).toInt())
        leftEditor.font = mono
        rightEditor.font = mono

        val leftPanel = editorPanel(JBLabel("Left"), leftEditor, leftScroll)
        val rightPanel = editorPanel(JBLabel("Right"), rightEditor, rightScroll)

        swapButton.addActionListener { swapTexts() }

        val centerRow = JPanel(BorderLayout())
        centerRow.add(leftPanel, BorderLayout.CENTER)
        centerRow.add(rightPanel, BorderLayout.CENTER)
        add(centerRow, BorderLayout.CENTER)

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        toolbar.add(swapButton)
        add(toolbar, BorderLayout.NORTH)
        add(status, BorderLayout.SOUTH)

        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = recompute()
            override fun removeUpdate(e: DocumentEvent?) = recompute()
            override fun changedUpdate(e: DocumentEvent?) = recompute()
        }
        leftEditor.document.addDocumentListener(listener)
        rightEditor.document.addDocumentListener(listener)

        // Diff navigation: Alt+Down / Alt+Up to jump between change blocks.
        listOf(leftEditor, rightEditor).forEach {
            it.inputMap.put(KeyStroke.getKeyStroke("alt DOWN"), "next-diff")
            it.inputMap.put(KeyStroke.getKeyStroke("alt UP"), "prev-diff")
            it.actionMap.put("next-diff", DiffNavAction(1))
            it.actionMap.put("prev-diff", DiffNavAction(-1))
        }

        // Clicking a line scrolls the opposite pane to the same line.
        val sync = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                val src = e.source as? JTextPane ?: return
                val other = if (src === leftEditor) rightEditor else leftEditor
                val otherScroll = if (src === leftEditor) rightScroll else leftScroll
                scrollToLine(other, otherScroll, lineOf(src))
            }
        }
        leftEditor.addMouseListener(sync)
        rightEditor.addMouseListener(sync)

        recompute()
    }

    private fun editorPanel(label: JBLabel, editor: JTextPane, scroll: JBScrollPane): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(label, BorderLayout.NORTH)
        panel.add(scroll, BorderLayout.CENTER)
        return panel
    }

    private fun swapTexts() {
        val tmp = leftEditor.text
        leftEditor.text = rightEditor.text
        rightEditor.text = tmp
    }

    private fun recompute() {
        blocks = TextDiff.diff(leftEditor.text, rightEditor.text)
        applyHighlights()
        updateStatus()
    }

    private fun applyHighlights() {
        val clear = SimpleAttributeSet()
        leftEditor.styledDocument.setCharacterAttributes(0, leftEditor.document.length, clear, true)
        rightEditor.styledDocument.setCharacterAttributes(0, rightEditor.document.length, clear, true)
        highlightRuns(leftEditor, blocks.filter { it.isDeletion }, DELETION_COLOR)
        highlightRuns(rightEditor, blocks.filter { it.isAddition }, ADDITION_COLOR)
    }

    private fun highlightRuns(editor: JTextPane, runs: List<DiffBlock>, color: Color) {
        if (runs.isEmpty()) return
        val attr = SimpleAttributeSet()
        StyleConstants.setBackground(attr, color)
        val root = editor.document.defaultRootElement
        for (b in runs) {
            val start = if (b.isDeletion) b.leftStart else b.rightStart
            val count = if (b.isDeletion) b.leftCount else b.rightCount
            val first = root.getElement(start.coerceIn(0, root.elementCount - 1))
            val last = root.getElement((start + count - 1).coerceIn(0, root.elementCount - 1))
            val from = first.startOffset
            val to = last.endOffset
            if (to > from) editor.styledDocument.setCharacterAttributes(from, to - from, attr, true)
        }
    }

    private fun lineOf(editor: JTextPane): Int =
        try { editor.document.defaultRootElement.getElementIndex(editor.caretPosition) } catch (e: Exception) { 0 }

    private fun scrollToLine(target: JTextPane, scroll: JBScrollPane, line: Int) {
        val root = target.document.defaultRootElement
        val count = root.elementCount
        if (count == 0) return
        val l = line.coerceIn(0, count - 1)
        val el = root.getElement(l)
        SwingUtilities.invokeLater {
            try {
                val loc = target.modelToView2D(el.startOffset)
                scroll.viewport.viewPosition = java.awt.Point(0, loc.y.toInt())
                target.caretPosition = el.startOffset
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun updateStatus() {
        val leftLines = lineCount(leftEditor)
        val rightLines = lineCount(rightEditor)
        val added = blocks.count { it.isAddition }
        val removed = blocks.count { it.isDeletion }
        status.update(leftEditor.text.length, leftLines, null)
        status.setInfo("$leftLines / $rightLines lines  ·  +$added / −$removed")
    }

    private fun lineCount(editor: JTextPane): Int =
        try { editor.document.defaultRootElement.elementCount } catch (e: Exception) { 1 }

    private inner class DiffNavAction(private val dir: Int) : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            val editor = e?.source as? JTextPane ?: return
            val other = if (editor === leftEditor) rightEditor else leftEditor
            val otherScroll = if (editor === leftEditor) rightScroll else leftScroll
            val currentLine = lineOf(editor)
            val anchors = blocks.filter { it.isDeletion }.map { it.leftStart } +
                blocks.filter { it.isAddition }.map { it.rightStart }
            if (anchors.isEmpty()) return
            val target = if (dir > 0) anchors.firstOrNull { it > currentLine } ?: anchors.first()
            else anchors.lastOrNull { it < currentLine } ?: anchors.last()
            scrollToLine(other, otherScroll, target)
        }
    }

    private companion object {
        val DELETION_COLOR = Color(0xFFFFE1E1.toInt())
        val ADDITION_COLOR = Color(0xFFE2FFE2.toInt())
    }
}
