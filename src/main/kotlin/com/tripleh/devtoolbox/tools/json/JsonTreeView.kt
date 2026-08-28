package com.tripleh.devtoolbox.tools.json

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Collapsible tree rendering of a [JsonValue]: `key : {n}` for objects, `key : [n]` for
 * arrays, colored scalars below. Selecting a node reports the value's source range via
 * [selectionConsumer] so the editor can highlight it.
 */
class JsonTreeView : JPanel(BorderLayout()) {

    var selectionConsumer: ((Int, Int) -> Unit)? = null

    private val tree = Tree()
    private val model = DefaultTreeModel(DefaultMutableTreeNode())

    init {
        tree.model = model
        tree.isRootVisible = true
        tree.showsRootHandles = false
        tree.cellRenderer = Renderer
        tree.emptyText.text = "Tree view appears here once the JSON is valid"
        tree.border = JBUI.Borders.empty()
        tree.addTreeSelectionListener {
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val data = node.userObject as? Node ?: return@addTreeSelectionListener
            selectionConsumer?.invoke(data.value.start, data.value.end)
        }

        val header = JPanel(BorderLayout())
        header.border = JBUI.Borders.emptyBottom(2)
        header.add(JBLabel("Tree"), BorderLayout.WEST)
        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0))
        buttons.isOpaque = false
        val expandAll = JButton("Expand All").apply {
            isFocusable = false
            toolTipText = "Expand all tree nodes"
            addActionListener { expandAll() }
        }
        val collapseAll = JButton("Collapse All").apply {
            isFocusable = false
            toolTipText = "Collapse all tree nodes"
            addActionListener { collapseAll() }
        }
        buttons.add(expandAll)
        buttons.add(collapseAll)
        header.add(buttons, BorderLayout.EAST)

        add(header, BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(tree), BorderLayout.CENTER)
    }

    /** Replaces the displayed tree; null shows an empty-text hint ([blank] picks the wording). */
    fun show(value: JsonValue?, blank: Boolean) {
        tree.emptyText.text = when {
            value != null -> ""
            blank -> "Paste or type JSON to see the tree"
            else -> "Invalid JSON — tree unavailable"
        }
        val root = if (value == null) null else buildNode(null, value)
        model.setRoot(root)
        model.reload()
        if (root != null) tree.expandRow(0)
    }

    fun expandAll() {
        for (row in 0 until tree.rowCount) tree.expandRow(row)
    }

    fun collapseAll() {
        for (row in tree.rowCount - 1 downTo 0) tree.collapseRow(row)
    }

    private fun buildNode(label: String?, value: JsonValue): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(Node(label, value))
        when (value) {
            is JsonObjectValue -> value.members.forEach { node.add(buildNode(it.key, it.value)) }
            is JsonArrayValue -> value.items.forEachIndexed { index, item -> node.add(buildNode(index.toString(), item)) }
            else -> Unit
        }
        return node
    }

    private class Node(val label: String?, val value: JsonValue)

    private object Renderer : ColoredTreeCellRenderer() {

        private val keyAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(0x174AD4, 0x56A8F5))
        private val punctuationAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.foreground())
        private val stringAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(0x067D17, 0x6A8759))
        private val numberAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(0x1750EB, 0x6897BB))
        private val keywordAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(0x0033B3, 0xCC7832))

        override fun customizeCellRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ) {
            val node = value as? DefaultMutableTreeNode ?: return
            val data = node.userObject as? Node ?: return
            data.label?.let {
                append(it, keyAttrs)
                append(" : ", punctuationAttrs)
            }
            when (val v = data.value) {
                is JsonObjectValue -> append("{${v.members.size}}", punctuationAttrs)
                is JsonArrayValue -> append("[${v.items.size}]", punctuationAttrs)
                is JsonStringValue -> append(preview(v.value), stringAttrs)
                is JsonNumberValue -> append(v.raw, numberAttrs)
                is JsonBooleanValue -> append(v.value.toString(), keywordAttrs)
                is JsonNullValue -> append("null", keywordAttrs)
            }
        }

        private fun preview(text: String): String {
            val singleLine = text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
            return if (singleLine.length > MAX_PREVIEW) singleLine.take(MAX_PREVIEW) + "…" else singleLine
        }

        private const val MAX_PREVIEW = 120
    }
}
