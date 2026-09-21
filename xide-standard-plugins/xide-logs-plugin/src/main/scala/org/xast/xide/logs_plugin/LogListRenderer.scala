package org.xast.xide.logs_plugin

import org.xast.xide.ui.utils.XideStyle
import org.xast.xide.core.utils.LucideIcon

import scala.swing.*
import javax.swing.{UIManager, BorderFactory}
import java.awt.Color

class LogListRenderer extends ListView.Renderer[LogLine]:
    
    private val stdOutIcon = 
        LucideIcon.INFO.icon(16, UIManager.getColor("Label.foreground"))
    
    private val stdErrIcon = 
        LucideIcon.CIRCLE_X.icon(16, Color.RED)

    def componentFor(
        list: ListView[? <: LogLine],
        isSelected: Boolean,
        focused: Boolean,
        value: LogLine,
        index: Int
    ): Component =
        val style = XideStyle.getCurrent
        val bgColor = UIManager.getColor("TextArea.background")
        val label = new Label(stripAnsi(value.text))

        label.xAlignment = Alignment.Left
        label.font = style.uiFont
        label.background = bgColor
        label.border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
        label.iconTextGap = 8
        label.icon = 
            if value.isError then 
                stdErrIcon 
            else 
                stdOutIcon

        if isSelected then
            label.background = list.selectionBackground

        label.opaque = true

        if (value.isError)
            label.foreground = Color.RED

        label
