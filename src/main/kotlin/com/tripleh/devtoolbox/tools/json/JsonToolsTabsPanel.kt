package com.tripleh.devtoolbox.tools.json

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Font
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Hosts several [JsonToolsPanel] sub-tabs so multiple JSON documents can be worked on at
 * once. The tab row is a custom chip strip: one chip per document (title + close button)
 * followed immediately by the "+" button that opens a fresh tab. Closing the last tab
 * reopens an empty one so the panel is never blank.
 */
class JsonToolsTabsPanel(private val project: Project, private val parentDisposable: Disposable) : JPanel(BorderLayout()) {

    private val tabRow = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply { isOpaque = false }
    private val content = JPanel(CardLayout())
    private val docs = ArrayList<Doc>()
    private var selected: Doc? = null
    private var counter = 0

    private inner class Doc(val title: String) {
        val key = "doc-$title"
        val panel = JsonToolsPanel(project, parentDisposable)
        val header = TabHeader(this)
    }

    init {
        tabRow.add(JButton(AllIcons.General.Add).apply {
            toolTipText = "New JSON tab"
            isFocusable = false
            preferredSize = Dimension(30, 24)
            addActionListener { addTab() }
        })

        val north = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(4)
            add(tabRow, BorderLayout.CENTER)
        }
        add(north, BorderLayout.NORTH)
        add(content, BorderLayout.CENTER)

        addTab()
    }

    private fun addTab() {
        counter++
        val doc = Doc("JSON $counter")
        docs.add(doc)
        // Keep the "+" last: insert the chip right before it.
        tabRow.add(doc.header, tabRow.componentCount - 1)
        content.add(doc.panel, doc.key)
        select(doc)
    }

    private fun closeDoc(doc: Doc) {
        val index = docs.indexOf(doc)
        if (index < 0) return
        docs.remove(doc)
        tabRow.remove(doc.header)
        content.remove(doc.panel)
        if (docs.isEmpty()) {
            addTab()
        } else {
            if (selected === doc) select(docs[index.coerceAtMost(docs.size - 1)])
            revalidate()
            repaint()
        }
    }

    private fun select(doc: Doc) {
        selected = doc
        docs.forEach { it.header.setSelected(it === doc) }
        (content.layout as CardLayout).show(content, doc.key)
    }

    private inner class TabHeader(val doc: Doc) : JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)) {

        private val label = JBLabel(doc.title)
        private var isSelectedTab = false

        init {
            isOpaque = false
            border = JBUI.Borders.empty(2, 8)
            add(label)
            add(JButton(AllIcons.Actions.Close).apply {
                toolTipText = "Close tab"
                isFocusable = false
                preferredSize = Dimension(18, 18)
                border = JBUI.Borders.empty()
                isContentAreaFilled = false
                addActionListener { closeDoc(this@TabHeader.doc) }
            })
            val selectOnClick = object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = select(this@TabHeader.doc)
            }
            addMouseListener(selectOnClick)
            label.addMouseListener(selectOnClick)
        }

        fun setSelected(value: Boolean) {
            if (isSelectedTab == value) return
            isSelectedTab = value
            label.font = label.font.deriveFont(if (value) Font.BOLD else Font.PLAIN)
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            if (isSelectedTab) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = UIUtil.getListSelectionBackground(true)
                g2.fillRoundRect(0, 0, width, height, 8, 8)
                g2.dispose()
            }
            super.paintComponent(g)
        }
    }
}
