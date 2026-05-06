package org.xast.xide.logs_plugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

import org.xast.xide.core.plugin.bottom.BottomPanelView
import org.xast.xide.core.utils.LucideIcon
import org.xast.xide.ui.utils.XideStyle

import java.awt.*
import java.io.PrintStream
import java.nio.charset.StandardCharsets

import javax.swing.*

class LogsView : BottomPanelView() {
    companion object {
        const val MAX_LINES = 1000
        const val FLUSH_INTERVAL_MS = 500
        private val ANSI_REGEX = Regex("\u001B\\[[\\d]*m")
    }
    
    private val listModel = DefaultListModel<LogLine>()
    private val list = JList(listModel)
    private val allLines = mutableListOf<LogLine>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val channel = Channel<LogLine>(Channel.UNLIMITED)
    private val stdOutIcon = LucideIcon.INFO.icon(16, UIManager.getColor("Label.foreground"))
    private val stdErrIcon = LucideIcon.CIRCLE_X.icon(16, Color.RED)
    private var showStdOut = true
    private var showStdErr = true

    init {
        layout = BorderLayout()

        val style = XideStyle.getCurrent()
        val bgColor = UIManager.getColor("TextArea.background")
        val fgColor = UIManager.getColor("Label.foreground")

        // List
        list.background = bgColor
        list.setCellRenderer { listComp, value, index, isSelected, cellHasFocus ->
            val label = JLabel(value.text.stripAnsi())

            label.font = style.uiFont()
            label.background = bgColor
            label.border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
            label.icon = if (value.isError) stdErrIcon else stdOutIcon
            label.iconTextGap = 8

            if (isSelected)
                label.background = listComp.selectionBackground

            label.isOpaque = true

            if (value.isError)
                label.foreground = Color.RED

            label
        }

        add(
            JScrollPane(list).apply {
                border = BorderFactory.createMatteBorder(1, 0, 0, 0, Color(0x424242))
            }, 
            BorderLayout.CENTER
        )

        // Top bar
        var topBar = Box.createHorizontalBox()
        topBar.border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
        topBar.add(JCheckBox("StdOut").apply {
            isSelected = showStdOut
            font = style.uiFont()
            addActionListener {
                showStdOut = isSelected
                rebuildVisibleLines()
            }
        })
        topBar.add(Box.createRigidArea(Dimension(8,0)))
        topBar.add(JCheckBox("StdErr").apply{
            isSelected = showStdErr
            font = style.uiFont()
            addActionListener {
                showStdErr = isSelected
                rebuildVisibleLines()
            }
        })
        topBar.add(Box.createRigidArea(Dimension(8,0)))
        topBar.add(JButton("Clear").apply {
            setForeground(Color.LIGHT_GRAY)
            setFont(style.uiFont())
            addActionListener {
                while (true) {
                    if (channel.tryReceive().getOrNull() == null)
                        break
                }
                allLines.clear()
                listModel.clear()
            }
        })
        add(topBar, BorderLayout.NORTH)

        scope.launch {
            val buffer = mutableListOf<LogLine>()

            while (true) {
                val first = channel.receive()
                buffer.add(first)

                delay(FLUSH_INTERVAL_MS.milliseconds)

                while (true) {
                    val next = channel.tryReceive().getOrNull() ?: break
                    buffer.add(next)
                }

                val batch = buffer.toList()
                buffer.clear()

                withContext(Dispatchers.Swing) {
                    flushBatch(batch)
                }
            }
        }

        // Print stream
        System.setOut(PrintStream(
            ConsoleRedirectStream(this, false),
            true,
            StandardCharsets.UTF_8
        ))
        System.setErr(PrintStream(
            ConsoleRedirectStream(this, true),
            true,
            StandardCharsets.UTF_8
        ))
    }

    fun addLine(text: String, error: Boolean) {
        channel.trySend(LogLine(text, error))
    }

    private fun flushBatch(batch: List<LogLine>) {
        allLines.addAll(batch)

        val overflow = allLines.size - MAX_LINES
        val trimmed = overflow > 0
        if (trimmed)
            allLines.subList(0, overflow).clear()

        if (trimmed)
            rebuildVisibleLines()
        else
            appendVisibleLines(batch)
    }

    private fun appendVisibleLines(lines: List<LogLine>) {
        for (line in lines) {
            if (shouldShow(line))
                listModel.addElement(line)
        }

        if (listModel.size > 0)
            list.ensureIndexIsVisible(listModel.size - 1)
    }

    private fun rebuildVisibleLines() {
        listModel.clear()

        for (line in allLines) {
            if (shouldShow(line))
                listModel.addElement(line)
        }

        if (listModel.size > 0)
            list.ensureIndexIsVisible(listModel.size - 1)
    }

    private fun shouldShow(line: LogLine): Boolean =
        if (line.isError) showStdErr else showStdOut

    private fun String.stripAnsi(): String =
        replace(ANSI_REGEX, "")
}
