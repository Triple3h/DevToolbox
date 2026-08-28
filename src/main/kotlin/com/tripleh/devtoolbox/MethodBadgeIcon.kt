package com.tripleh.devtoolbox

import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon

/**
 * Colored round badge with the first letter of the HTTP method, rendered in the endpoint list
 * of the REST Services tool window. Color scheme matches the one used across the plugin.
 */
class MethodBadgeIcon(private val method: String) : Icon {

    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        val g2 = g?.create() as? Graphics2D ?: return
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val size = iconWidth
            g2.color = colorFor(method)
            g2.fillOval(x, y, size, size)

            val letter = method.take(1).ifEmpty { "?" }
            val font = Font(Font.SANS_SERIF, Font.BOLD, (size * 0.55f).toInt().coerceAtLeast(7))
            val metrics = g2.getFontMetrics(font)
            g2.font = font
            g2.color = Color.WHITE
            g2.drawString(letter, x + (size - metrics.stringWidth(letter)) / 2, y + (size - metrics.height) / 2 + metrics.ascent)
        } finally {
            g2.dispose()
        }
    }

    override fun getIconWidth(): Int = JBUI.scale(16)

    override fun getIconHeight(): Int = JBUI.scale(16)

    companion object {
        fun colorFor(method: String): Color = when (method.uppercase()) {
            "GET" -> Color(0x59A869)
            "POST" -> Color(0x3574F0)
            "PUT" -> Color(0xE08800)
            "DELETE" -> Color(0xDB5C5C)
            "PATCH" -> Color(0x9E6DE3)
            else -> Color(0x6F737A)
        }
    }
}
