package com.github.nayacco.restfulhelper

import com.intellij.ide.util.ModuleRendererFactory
import com.intellij.ide.util.NavigationItemListCellRenderer
import com.intellij.ide.ui.UISettings
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.navigation.NavigationItem
import com.intellij.navigation.NavigationItemFileStatus
import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.speedSearch.SpeedSearchUtil
import com.intellij.util.text.Matcher
import com.intellij.util.text.MatcherHolder
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.SwingConstants

/**
 * Renderer for the "Go to Request Mapping" popup list.
 *
 * Extends [NavigationItemListCellRenderer] so that everything except our own items is rendered
 * exactly like the platform does. For [RequestMappingItem] values the layout is replicated with a
 * single difference: the leading HTTP method token is rendered bold and colored per verb, so that
 * GET / POST / PUT / DELETE rows are distinguishable at a glance.
 */
class RequestMappingItemListCellRenderer : NavigationItemListCellRenderer() {

    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value !is RequestMappingItem) {
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        }
        return renderRequestMappingItem(list, value, index, isSelected)
    }

    private fun renderRequestMappingItem(
        list: JList<*>,
        item: RequestMappingItem,
        index: Int,
        isSelected: Boolean
    ): Component {
        removeAll()

        val hasRightRenderer = UISettings.getInstance().showIconInQuickNavigation
        val factory = ModuleRendererFactory.findInstance(item)

        val leftRenderer = LeftRenderer(
            renderLocation = !hasRightRenderer || !factory.rendersLocationString(),
            matcher = MatcherHolder.getAssociatedMatcher(list)
        )
        val leftComponent = leftRenderer.getListCellRendererComponent(list, item, index, isSelected, false)
        val listBg = leftComponent.background
        (leftComponent as JComponent).isOpaque = false
        add(leftComponent, BorderLayout.WEST)

        background = if (isSelected) UIUtil.getListSelectionBackground(true) else listBg

        if (hasRightRenderer) {
            val textWithIcon = factory.getModuleTextWithIcon(item)
            if (textWithIcon != null) {
                val rightComponent = JBLabel(textWithIcon.text, textWithIcon.icon, SwingConstants.LEADING)
                rightComponent.isOpaque = false
                rightComponent.background = listBg
                add(rightComponent, BorderLayout.EAST)

                val spacer = NonOpaquePanel()
                val size = rightComponent.size
                val spacerWidth = (size.width * 0.015 + (getComponent(0).size.width * 0.015)).toInt()
                spacer.size = Dimension(spacerWidth, size.height)
                spacer.background = if (isSelected) UIUtil.getListSelectionBackground(true) else listBg
                add(spacer, BorderLayout.CENTER)
            }
        }
        return this
    }

    private class LeftRenderer(
        private val renderLocation: Boolean,
        private val matcher: Matcher?
    ) : ColoredListCellRenderer<Any>() {

        override fun customizeCellRenderer(
            list: JList<*>,
            value: Any,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            val item = value as NavigationItem
            val presentation = item.presentation
            if (presentation == null) {
                append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                return
            }

            val name = presentation.presentableText ?: ""

            var nameAttributes = NodeRenderer.getSimpleTextAttributes(presentation)
            val status = NavigationItemFileStatus.get(item)
            if (status !== FileStatus.NOT_CHANGED) {
                status.color?.let { nameAttributes = SimpleTextAttributes(nameAttributes.style, it) }
            }

            val method = (value as RequestMappingItem).requestMethod.uppercase()
            val spaceIndex = name.indexOf(' ')
            if (method.isNotEmpty() && spaceIndex > 0) {
                append("${name.substring(0, spaceIndex)} ", METHOD_COLORS[method] ?: nameAttributes)
                SpeedSearchUtil.appendColoredFragmentForMatcher(
                    name.substring(spaceIndex + 1), this, nameAttributes, matcher, UIUtil.getListBackground(), selected
                )
            } else {
                SpeedSearchUtil.appendColoredFragmentForMatcher(name, this, nameAttributes, matcher, UIUtil.getListBackground(), selected)
            }

            setIcon(presentation.getIcon(false))

            if (renderLocation) {
                val location = presentation.locationString
                if (!location.isNullOrEmpty()) {
                    append(" $location", SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY))
                }
            }

            setPaintFocusBorder(false)
            background = if (selected) UIUtil.getListSelectionBackground(true) else UIUtil.getListBackground()
        }

        private companion object {
            val METHOD_COLORS: Map<String, SimpleTextAttributes> = mapOf(
                "GET" to methodAttr(JBColor(0x1F7A33, 0x5FB865)),
                "POST" to methodAttr(JBColor(0x2B6CB8, 0x548AF7)),
                "PUT" to methodAttr(JBColor(0xB35C00, 0xE8A33D)),
                "DELETE" to methodAttr(JBColor(0xC23434, 0xE86A6A)),
                "PATCH" to methodAttr(JBColor(0x7A3EA1, 0xB07FE0)),
            )

            private fun methodAttr(color: Color) = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, color)
        }
    }
}
