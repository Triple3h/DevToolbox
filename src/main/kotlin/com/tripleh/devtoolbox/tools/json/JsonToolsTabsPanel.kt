package com.tripleh.devtoolbox.tools.json

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Hosts several [JsonToolsPanel] sub-tabs so multiple JSON documents can be worked on at
 * once. The "+" button opens a fresh tab; every tab header carries a close button, and
 * closing the last tab reopens an empty one so the panel is never blank.
 */
class JsonToolsTabsPanel(private val project: Project, private val parentDisposable: Disposable) : JPanel(BorderLayout()) {

    private val tabs = JBTabbedPane()
    private var counter = 0

    init {
        val addTabButton = JButton(AllIcons.General.Add).apply {
            toolTipText = "New JSON tab"
            isFocusable = false
            preferredSize = Dimension(30, 24)
            addActionListener { addTab() }
        }
        val addRow = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
        addRow.isOpaque = false
        addRow.add(addTabButton)

        val header = JPanel(BorderLayout())
        header.border = JBUI.Borders.emptyBottom(4)
        header.add(addRow, BorderLayout.EAST)

        add(header, BorderLayout.NORTH)
        add(tabs, BorderLayout.CENTER)

        addTab()
    }

    private fun addTab() {
        counter++
        val panel = JsonToolsPanel(project, parentDisposable)
        val title = "JSON $counter"
        tabs.addTab(title, panel)
        tabs.setTabComponentAt(tabs.tabCount - 1, TabHeader(title) { closeTab(panel) })
        tabs.selectedIndex = tabs.tabCount - 1
    }

    private fun closeTab(panel: JComponent) {
        val index = tabs.indexOfComponent(panel)
        if (index >= 0) tabs.removeTabAt(index)
        if (tabs.tabCount == 0) addTab()
    }

    private inner class TabHeader(title: String, onClose: () -> Unit) : JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)) {

        init {
            isOpaque = false
            add(JBLabel(title))
            add(JButton(AllIcons.Actions.Close).apply {
                toolTipText = "Close tab"
                isFocusable = false
                preferredSize = Dimension(18, 18)
                border = JBUI.Borders.empty()
                isContentAreaFilled = false
                addActionListener { onClose() }
            })
        }
    }
}
