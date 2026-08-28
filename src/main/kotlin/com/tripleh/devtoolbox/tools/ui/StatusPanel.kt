package com.tripleh.devtoolbox.tools.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Bottom status bar shared by tool-window panels: shows char/line counts on the left
 * and an error message (in red) on the right.
 */
class StatusPanel : JPanel(BorderLayout()) {

    private val infoLabel = JBLabel("")
    private val errorLabel = JBLabel("")
    private var error: String? = null

    init {
        border = JBUI.Borders.emptyTop(6)
        infoLabel.foreground = JBColor.GRAY
        errorLabel.foreground = JBColor.RED
        add(infoLabel, BorderLayout.WEST)
        add(errorLabel, BorderLayout.EAST)
    }

    fun update(charCount: Int, lineCount: Int, error: String?) {
        infoLabel.text = if (charCount == 0 && lineCount == 0) "" else "$lineCount lines, $charCount chars"
        this.error = error
        errorLabel.text = error ?: ""
    }

    fun showError(message: String) {
        this.error = message
        errorLabel.text = message
    }

    fun setInfo(text: String) {
        infoLabel.text = text
    }
}
