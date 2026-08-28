package com.tripleh.devtoolbox

import com.tripleh.devtoolbox.extensions.Extensions
import com.tripleh.devtoolbox.utils.findApiSummary
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.Adjustable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicLong
import javax.swing.AbstractAction
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.event.DocumentListener

/**
 * Content of the "REST Services" tool window: a filterable list of all request mappings
 * found in the project, styled after RestfulToolkit's sidebar. Rows carry a colored HTTP
 * method badge, the mapping path, an optional documentation summary and the declaring
 * controller method; double click (or Enter) navigates to the declaration.
 */
internal class RestServicesPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val generation = AtomicLong()
    private var full: List<EndpointRow> = emptyList()
    private var filteredCount = 0
    private var visibleLimit = INITIAL_LIMIT
    private var updatingFilters = false

    private val searchField = SearchTextField(false)
    private val methodFilter = ComboBox<String>()
    private val moduleFilter = ComboBox<String>()
    private val refreshButton = JButton(AllIcons.Actions.Refresh)
    private val listModel = CollectionListModel<EndpointRow>()
    private val endpointList = JBList(listModel)
    private val statusLabel = JBLabel("Scanning request mappings…")

    private val filterChangedListener = object : DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = refilter()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = refilter()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = refilter()
    }

    init {
        border = JBUI.Borders.empty(8)

        searchField.addDocumentListener(filterChangedListener)

        refreshButton.toolTipText = "Rescan request mappings"
        refreshButton.isFocusable = false
        refreshButton.addActionListener { refresh() }

        val searchRow = JPanel(BorderLayout())
        searchRow.add(searchField, BorderLayout.CENTER)
        searchRow.add(refreshButton, BorderLayout.EAST)

        methodFilter.preferredSize = Dimension(JBUI.scale(180), methodFilter.preferredSize.height)
        methodFilter.addActionListener { if (!updatingFilters) refilter() }
        moduleFilter.addActionListener { if (!updatingFilters) refilter() }

        val filterRow = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 0.0
        constraints.insets = JBUI.insetsRight(8)
        filterRow.add(methodFilter, constraints)
        constraints.weightx = 1.0
        constraints.insets = JBUI.insets(0, 0, 0, 0)
        filterRow.add(moduleFilter, constraints)

        val header = JPanel(BorderLayout(0, 8))
        header.add(searchRow, BorderLayout.NORTH)
        header.add(filterRow, BorderLayout.SOUTH)
        add(header, BorderLayout.NORTH)

        endpointList.cellRenderer = EndpointRenderer()
        endpointList.emptyText.text = "No request mappings found"
        endpointList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2) navigateToSelection()
            }
        })

        val scrollPane = ScrollPaneFactory.createScrollPane(endpointList, true)
        scrollPane.verticalScrollBar.addAdjustmentListener { maybeExpandLimit(it.adjustable) }
        add(scrollPane, BorderLayout.CENTER)

        statusLabel.foreground = JBColor.GRAY
        statusLabel.border = JBUI.Borders.emptyTop(6)
        add(statusLabel, BorderLayout.SOUTH)

        endpointList.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "navigate")
        endpointList.actionMap.put("navigate", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) = navigateToSelection()
        })
    }

    /** Rescans all request mappings on a background thread and refreshes the list. */
    fun refresh() {
        val gen = generation.incrementAndGet()
        refreshButton.isEnabled = false
        statusLabel.text = "Scanning request mappings…"
        ApplicationManager.getApplication().executeOnPooledThread {
            val rows: List<EndpointRow> = try {
                ReadAction.compute<List<EndpointRow>, Exception> { collectEndpoints() }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Exception) {
                emptyList() // e.g. indices not ready; user can hit refresh again
            }
            ApplicationManager.getApplication().invokeLater({
                if (project.isDisposed || gen != generation.get()) return@invokeLater
                applyScanResult(rows)
            }, ModalityState.any())
        }
    }

    private fun collectEndpoints(): List<EndpointRow> {
        val rows = mutableListOf<EndpointRow>()
        for (contributor in Extensions.getExtensions()) {
            for (name in contributor.getNames(project, false)) {
                for (item in contributor.getItemsByName(name, "", project, false)) {
                    if (item is RequestMappingItem) rows += buildRow(item)
                }
            }
        }
        rows.sortWith(compareBy({ it.path }, { it.method }, { it.location }))
        return rows
    }

    private fun buildRow(item: RequestMappingItem): EndpointRow {
        val name = item.name
        val method = item.requestMethod.ifBlank { name.substringBefore(' ', "") }
        val path = name.substringAfter(' ', name)

        var location = ""
        var summary = ""
        when (val psiElement = item.psiElement) {
            is PsiMethod -> {
                val className = psiElement.containingClass?.name
                location = buildString {
                    if (className != null) append("$className#")
                    append(psiElement.name)
                }
                summary = psiElement.findApiSummary()
            }
            is PsiClass -> location = psiElement.name ?: ""
        }

        var module = ""
        val file = item.psiElement.containingFile?.originalFile?.virtualFile
        if (file != null) {
            module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(file)?.name ?: ""
        }
        return EndpointRow(item, method, path, location, module, summary)
    }

    private fun applyScanResult(rows: List<EndpointRow>) {
        full = rows
        rebuildMethodFilter()
        rebuildModuleFilter()
        visibleLimit = INITIAL_LIMIT
        refilter()
        refreshButton.isEnabled = true
    }

    private fun rebuildMethodFilter() {
        updatingFilters = true
        val previous = filterKey(methodFilter.selectedItem)
        val items = mutableListOf("All Methods (${full.size})")
        full.groupingBy { it.method }.eachCount().toSortedMap().forEach { (method, count) ->
            items += "$method ($count)"
        }
        methodFilter.model = DefaultComboBoxModel(items.toTypedArray())
        methodFilter.selectedItem = items.firstOrNull { filterKey(it) == previous } ?: items.first()
        updatingFilters = false
    }

    private fun rebuildModuleFilter() {
        updatingFilters = true
        val previous = filterKey(moduleFilter.selectedItem)
        val items = mutableListOf("All Modules (${full.size})")
        full.filter { it.module.isNotEmpty() }
            .groupingBy { it.module }
            .eachCount()
            .toSortedMap()
            .forEach { (module, count) -> items += "$module ($count)" }
        moduleFilter.model = DefaultComboBoxModel(items.toTypedArray())
        moduleFilter.selectedItem = items.firstOrNull { filterKey(it) == previous } ?: items.first()
        updatingFilters = false
    }

    private fun refilter() {
        val query = searchField.text.trim().lowercase()
        val method = selectedFilterValue(methodFilter)
        val module = selectedFilterValue(moduleFilter)

        val filtered = full.filter { row ->
            (method == null || row.method == method) &&
                (module == null || row.module == module) &&
                (query.isEmpty() || row.matches(query))
        }
        filteredCount = filtered.size
        listModel.replaceAll(filtered.take(visibleLimit))
        statusLabel.text =
            if (filteredCount > visibleLimit) "Showing first $visibleLimit of $filteredCount endpoints"
            else "$filteredCount endpoint${if (filteredCount == 1) "" else "s"}"
    }

    private fun EndpointRow.matches(query: String): Boolean =
        path.lowercase().contains(query) ||
            method.lowercase().contains(query) ||
            location.lowercase().contains(query) ||
            summary.lowercase().contains(query)

    private fun selectedFilterValue(comboBox: ComboBox<String>): String? {
        val text = comboBox.selectedItem as? String ?: return null
        if (text.startsWith("All ")) return null
        return text.substringBeforeLast(" (")
    }

    private fun filterKey(selected: Any?): String =
        (selected as? String)?.substringBefore(" (") ?: ""

    private fun navigateToSelection() {
        val row = endpointList.selectedValue ?: return
        if (row.item.canNavigate()) row.item.navigate(true)
    }

    private fun maybeExpandLimit(adjustable: Adjustable) {
        if (filteredCount <= visibleLimit || listModel.size < visibleLimit) return
        if (adjustable.value + adjustable.visibleAmount >= adjustable.maximum - 20) {
            visibleLimit *= 2
            refilter()
        }
    }

    private class EndpointRenderer : ColoredListCellRenderer<EndpointRow>() {

        override fun customizeCellRenderer(list: JList<out EndpointRow>, value: EndpointRow, index: Int, selected: Boolean, hasFocus: Boolean) {
            icon = MethodBadgeIcon(value.method)
            append(value.path, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            if (value.summary.isNotEmpty()) append("  ${value.summary}", SimpleTextAttributes.GRAY_ATTRIBUTES)
            if (value.location.isNotEmpty()) append("  (${value.location})", SimpleTextAttributes.GRAY_ATTRIBUTES)
            toolTipText = buildString {
                append("${value.method} ${value.path}")
                if (value.module.isNotEmpty()) append("   [${value.module}]")
            }
        }
    }

    internal data class EndpointRow(
        val item: RequestMappingItem,
        val method: String,
        val path: String,
        val location: String,
        val module: String,
        val summary: String,
    )

    private companion object {
        const val INITIAL_LIMIT = 200
    }
}
