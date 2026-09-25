package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Color
import java.awt.FontMetrics
import org.xast.xide.ui.utils.XideStyle
import scala.swing.Component
import scala.swing.Color
import scala.swing.Dimension
import scala.swing.Font

case class EditorStyleMetrics(
   fontMetrics: FontMetrics,
   font: Font,
   fontColor: Color,
   bgColor: Color,
   xideStyle: XideStyle,
   size: Dimension,
)

object EditorStyleMetrics:

   final val caretBlinkIntervalMs: Int = 500
   final val scrollbarMinThumb: Int = 20
   final val scrollLinesPerNotch: Int = 2
   final val scrollbarWidth: Int = 16
   final val caretWidth: Int = 2
   final val gutterRightMargin: Int = 28
   final val gutterPadding: Int = 32
   final val defaultGutterWidth: Int = 40
   final val fontSize: Float = 20f

   def initial(component: Component): EditorStyleMetrics =
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
      component.background = bgColor

      EditorStyleMetrics(
         fontMetrics,
         font,
         fontColor,
         bgColor,
         xideStyle,
         component.size,
      )

   def computeThumbHeight(trackHeight: Int, contentHeight: Int): Int =
      Math.max(
         EditorStyleMetrics.scrollbarMinThumb, 
         (trackHeight.toLong * trackHeight / contentHeight).toInt
      )