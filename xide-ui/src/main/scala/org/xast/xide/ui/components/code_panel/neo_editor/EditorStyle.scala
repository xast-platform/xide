package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Color
import java.awt.FontMetrics
import org.xast.xide.ui.utils.XideStyle
import scala.swing.Component
import scala.swing.Color
import scala.swing.Dimension

case class EditorStyle(
   fontMetrics: FontMetrics,
   fontColor: Color,
   bgColor: Color,
   xideStyle: XideStyle,
   size: Dimension,
)

object EditorStyle:

   final val gutterRightMargin: Int = 28
   final val gutterPadding: Int = 32
   final val fontSize: Float = 20f

   def initial(component: Component): EditorStyle =
      val xideStyle = XideStyle.getCurrent()
      val fontColor = 
         if xideStyle.isDarkTheme then
            Color.WHITE
         else 
            Color.BLACK

      val font = xideStyle.codeFont.deriveFont(fontSize)
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