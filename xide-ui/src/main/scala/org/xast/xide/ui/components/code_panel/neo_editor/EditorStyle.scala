package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.FontMetrics
import scala.swing.Font
import scala.swing.Color
import org.xast.xide.ui.utils.XideStyle

case class EditorStyle(
   fontMetrics: FontMetrics,
   fontColor: Color,
   bgColor: Color,
   xideStyle: XideStyle,
)