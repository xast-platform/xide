package org.xast.xide.logs_plugin

import org.xast.xide.core.plugin.bottom.BottomPanelView
import org.xast.xide.ui.utils.XideStyle

import java.awt.{BorderLayout, Color}
import java.util.concurrent.LinkedBlockingQueue
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.swing.{Timer, UIManager, BorderFactory}

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*
import scala.swing.*
import javax.swing.SwingUtilities

object LogsView {
    val MAX_LINES = 1000
    val FLUSH_INTER_MS = 500
    val ANSI_REGEX = Pattern.compile("\u001B\\[[\\d]*m")
}

class LogsView extends BottomPanelView:
    private val list      = new ListView[LogLine]()
    private val allLines  = new ArrayBuffer[LogLine]()
    private val channel   = new LinkedBlockingQueue[LogLine]()
    private val timer     = new Timer(LogsView.FLUSH_INTER_MS, _ => flush())
    private var showOut   = true
    private var showErr   = true

    setLayout(BorderLayout())

    val style = XideStyle.getCurrent()
    val bgColor = UIManager.getColor("TextArea.background")
    val fgColor = UIManager.getColor("Label.foreground")

    list.background = bgColor
    list.renderer = new LogListRenderer()

    // Scroll pane
    val scrollPane = new ScrollPane {
        contents = list
        border = BorderFactory.createMatteBorder(
            1, 0, 0, 0,
            Color(0x424242)
        )
    }
    // TODO: replace later with `content +=`
    add(scrollPane.peer, BorderLayout.CENTER)

    // Top bar
    val stdOutCheckbox = new CheckBox("StdOut") {
        selected = showOut
        font = style.uiFont
    };

    val stdErrCheckbox = new CheckBox("StdErr") {
        selected = showErr
        font = style.uiFont
    }

    val clearButton = new Button("Clear") {
        foreground = fgColor
        font = style.uiFont
    }

    val topBar = new BoxPanel(Orientation.Horizontal) {
        border = BorderFactory.createEmptyBorder(4, 0, 4, 0)

        contents += stdOutCheckbox
        contents += Swing.HStrut(8)
        contents += stdErrCheckbox
        contents += Swing.HStrut(8)
        contents += clearButton
    }

    // TODO: replace later with `listenTo(stdOutCheckBox)` and `reactions += {...}`
    stdOutCheckbox.peer.addActionListener(_ => 
        showOut = stdOutCheckbox.selected
        rebuildVisibleLines()
    )

    stdErrCheckbox.peer.addActionListener(_ => 
        showErr = stdErrCheckbox.selected
        rebuildVisibleLines()
    )

    clearButton.peer.addActionListener(_ =>
        channel.clear()
        allLines.clear()
        list.listData = Seq.empty  
    )

    add(topBar.peer, BorderLayout.NORTH)

    // Print stream
    System.setOut(new PrintStream(
        ConsoleRedirectStream(this, false),
        true,
        StandardCharsets.UTF_8
    ))

    System.setErr(new PrintStream(
        ConsoleRedirectStream(this, true),
        true,
        StandardCharsets.UTF_8
    ))

    // Start flush timer
    timer.start()

    SwingUtilities.invokeLater(() =>
        println("Sasi")
    )

    def addLine(text: String, error: Boolean) = 
        channel.offer(LogLine(text, error))

    private def flush() =
        val batch = new ArrayBuffer[LogLine]()

        channel.drainTo(batch.asJava)

        if batch.nonEmpty then
            flushBatch(batch)

    private def flushBatch(batch: ArrayBuffer[LogLine]) =
        allLines.addAll(batch)

        val overflow = allLines.size - LogsView.MAX_LINES
        val trimmed = overflow > 0
        
        if trimmed then 
            allLines.remove(0, overflow)

        if trimmed then
            rebuildVisibleLines()
        else
            appendVisibleLines(batch)

    private def appendVisibleLines(lines: ArrayBuffer[LogLine]) =
        val shownLines = lines.filter(shouldShow)
        list.listData = list.listData.appendedAll(shownLines)

        if list.listData.nonEmpty then
            list.ensureIndexIsVisible(list.listData.size - 1)

    private def rebuildVisibleLines() =
        list.listData = allLines.filter(shouldShow).toSeq

        if list.listData.nonEmpty then
            list.ensureIndexIsVisible(list.listData.size - 1)

    private def shouldShow(line: LogLine): Boolean =
        if line.isError then showErr else showOut

end LogsView

def stripAnsi(text: String): String =
    LogsView.ANSI_REGEX.matcher(text).replaceAll("")