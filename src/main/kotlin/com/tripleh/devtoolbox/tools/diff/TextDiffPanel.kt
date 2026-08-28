package com.tripleh.devtoolbox.tools.diff

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestPanel
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.tripleh.devtoolbox.tools.json.JsonFormat
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.Timer

/**
 * Side-by-side text comparison backed by the IDE's own diff viewer — line numbers,
 * changed-line / intra-line-word highlighting, gutter markers and change navigation
 * all come from the platform. Both panes are editable and re-diff live while typing.
 *
 * Editors start as plain text; once both sides parse as valid JSON they are rebuilt
 * with the JSON file type for syntax coloring. The upgrade is sticky (never flips
 * back), so a transient invalid edit while typing doesn't rebuild the panes.
 */
class TextDiffPanel(private val project: Project, parentDisposable: Disposable) : JPanel(BorderLayout()) {

    private val diffPanel: DiffRequestPanel =
        DiffManager.getInstance().createRequestPanel(project, parentDisposable, null)

    private var leftContent: DocumentContent? = null
    private var rightContent: DocumentContent? = null
    private var jsonMode = false
    private val jsonCheckTimer = Timer(JSON_CHECK_DELAY_MS) { upgradeToJsonTypeIfBothParse() }

    init {
        add(diffPanel.component, BorderLayout.CENTER)
        showContents("", "")
    }

    private fun showContents(left: String, right: String) {
        val factory = DiffContentFactory.getInstance()
        val newLeft = factory.createEditable(project, left, currentFileType())
        val newRight = factory.createEditable(project, right, currentFileType())

        val listener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = jsonCheckTimer.restart()
        }
        newLeft.document.addDocumentListener(listener)
        newRight.document.addDocumentListener(listener)
        leftContent = newLeft
        rightContent = newRight

        diffPanel.setRequest(SimpleDiffRequest(TITLE, newLeft, newRight, LEFT_TITLE, RIGHT_TITLE))
    }

    private fun upgradeToJsonTypeIfBothParse() {
        if (jsonMode) return
        val left = leftContent?.document?.text ?: return
        val right = rightContent?.document?.text ?: return
        if (left.isBlank() || right.isBlank()) return
        if (JsonFormat.parse(left) == null || JsonFormat.parse(right) == null) return
        jsonMode = true
        showContents(left, right)
    }

    private fun currentFileType(): FileType =
        if (jsonMode) FileTypeManager.getInstance().findFileTypeByName(JSON_FILE_TYPE_NAME) ?: PlainTextFileType.INSTANCE
        else PlainTextFileType.INSTANCE

    private companion object {
        const val TITLE = "Text Diff"
        const val LEFT_TITLE = "Left"
        const val RIGHT_TITLE = "Right"
        const val JSON_FILE_TYPE_NAME = "JSON"
        const val JSON_CHECK_DELAY_MS = 400
    }
}
