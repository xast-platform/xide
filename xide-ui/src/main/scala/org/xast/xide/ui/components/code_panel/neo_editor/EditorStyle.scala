package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.FontMetrics
import scala.swing.Color
import org.xast.xide.ui.utils.XideStyle
import scala.swing.Component
import java.awt.Color
import scala.swing.Dimension

case class EditorStyle(
   fontMetrics: FontMetrics,
   fontColor: Color,
   bgColor: Color,
   xideStyle: XideStyle,
   size: Dimension,
)

object EditorStyle:

   final val FONT_SIZE: Float = 20f

   def initial(component: Component): EditorStyle =
      val xideStyle = XideStyle.getCurrent()
      val fontColor = 
         if xideStyle.isDarkTheme then
            Color.WHITE
         else 
            Color.BLACK

      val font = xideStyle.codeFont.deriveFont(FONT_SIZE)
      val fontMetrics = component.peer.getFontMetrics(font)
      val bgColor = xideStyle.shiftAccent(0.3f)

      component.peer.setFont(font)

      EditorStyle(
         fontMetrics,
         fontColor,
         bgColor,
         xideStyle,
         component.size,
      )