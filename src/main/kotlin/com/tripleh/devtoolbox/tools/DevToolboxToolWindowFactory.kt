package com.tripleh.devtoolbox.tools

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.tripleh.devtoolbox.RestServicesPanel
import com.tripleh.devtoolbox.tools.diff.TextDiffPanel
import com.tripleh.devtoolbox.tools.json.JsonToolsTabsPanel

/**
 * "Dev Toolbox" tool window: a tabbed panel grouping all dev utilities — REST endpoint
 * list, JSON tools, text diff. The last-selected tab is remembered per-IDE session.
 */
class DevToolboxToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val manager = toolWindow.contentManager

        val restPanel = RestServicesPanel(project)
        manager.addContent(ContentFactory.getInstance().createContent(restPanel, TAB_REST, false))
        manager.addContent(
            ContentFactory.getInstance().createContent(JsonToolsTabsPanel(project, toolWindow.disposable), TAB_JSON, false)
        )
        manager.addContent(
            ContentFactory.getInstance().createContent(TextDiffPanel(project, toolWindow.disposable), TAB_DIFF, false)
        )

        val remembered = Memory.lastTab
        val index = when (remembered) {
            TAB_JSON -> 1
            TAB_DIFF -> 2
            else -> 0
        }
        manager.setSelectedContent(manager.getContent(index)!!)

        manager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                val name = event.content.displayName
                if (name == TAB_REST || name == TAB_JSON || name == TAB_DIFF) Memory.lastTab = name
            }
        })

        DumbService.getInstance(project).runWhenSmart { restPanel.refresh() }
    }

    private companion object {
        const val TAB_REST = "REST Services"
        const val TAB_JSON = "JSON Tools"
        const val TAB_DIFF = "Text Diff"
    }
}

/** Per-IDE (application-level) memory for the active Dev Toolbox tab. */
object Memory {
    var lastTab: String? = null
}
