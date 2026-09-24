package org.xast.xide.ui.components.code_panel.neo_editor

import java.awt.Color
import java.awt.Graphics

import javax.swing.Timer

import org.xast.xide.core.event.EventBus
import org.xast.xide.core.event.ThemeChangedEvent
import org.xast.xide.ui.components.RepaintRegion
import org.xast.xide.ui.utils.XideStyle

object OldCaret:
   private val BLINK_INTERVAL_MS: Int = 500

class OldCaret(val eventBus: EventBus, val repaintRegion: RepaintRegion):
   import OldCaret.*

   private var deltaX: Int = 0
   private var deltaY: Int = 0
   private var x: Int = 0
   private var y: Int = 0
   private var height: Int = 20
   private var visible: Boolean = true

   private var caretColor: Color = scala.compiletime.uninitialized
   private val style: XideStyle = XideStyle.getCurrent()

   applyTheme()
   eventBus.subscribe(classOf[ThemeChangedEvent], _ => applyTheme())

   private val timer: Timer = new Timer(
      BLINK_INTERVAL_MS,
      _ => {
         visible = !visible
         repaintCurrent()
      },
   )
   timer.setInitialDelay(BLINK_INTERVAL_MS)
   timer.start()

   def getDeltaX(): Int = deltaX
   def setDeltaX(deltaX: Int): Unit = this.deltaX = deltaX

   def getDeltaY(): Int = deltaY
   def setDeltaY(deltaY: Int): Unit = this.deltaY = deltaY

   def getX(): Int = x
   def getY(): Int = y

   def getHeight(): Int = height
   def setHeight(height: Int): Unit = this.height = height

   def isVisible(): Boolean = visible

   private def applyTheme(): Unit = {
      caretColor = if (style.isDarkTheme()) Color.WHITE else Color.BLACK
   }

   def moveTo(nextX: Int, nextY: Int): Unit = {
      val previousX = x
      val previousY = y

      x = nextX
      y = nextY
      visible = true

      repaintAt(previousX, previousY)
      repaintCurrent()

      timer.restart()
   }

   def setVisible(visible: Boolean): Unit = {
      if (this.visible == visible) {
         if (visible) {
            timer.restart()
         }
         return
      }

      this.visible = visible
      repaintCurrent()

      if (visible) {
         timer.restart()
      } else {
         timer.stop()
      }
   }

   def paintComponent(g: Graphics): Unit = {
      if (visible) {
         g.setColor(caretColor)
         g.fillRect(x * deltaX, y * deltaY, 2, height)
      }
   }

   private def repaintCurrent(): Unit = {
      repaintAt(x, y)
   }

   private def repaintAt(x: Int, y: Int): Unit = {
      repaintRegion.repaint(x * deltaX, y * deltaY, 2, height)
   }
