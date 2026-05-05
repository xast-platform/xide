package org.xast.xide.logs_plugin

import org.xast.xide.core.plugin.bottom.BottomPanelView
import org.xast.xide.core.utils.LucideIcon
import org.xast.xide.ui.utils.XideStyle

import java.awt.*
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

import javax.swing.*

class LogsView : BottomPanelView() {
    companion object {
        const val MAX_LINES: Int = 1000
        const val FLUSH_INTERVAL_MS: Int = 500
    }
    
    private var listModel: DefaultListModel<LogLine>
    private var list: JList<LogLine>
    private val allLines: List<LogLine> = mutableListOf()
    private val pendingLines: List<LogLine> = mutableListOf()
    private val pendingLock: Any = Any()
    private val flushRequestPosted: AtomicBoolean = AtomicBoolean(false)
    private var flushTimer: Timer
    private var stdOutIcon: Icon
    private var stdErrIcon: Icon
    private var flushScheduled: Boolean = false
    private var showStdOut: Boolean = true
    private var showStdErr: Boolean = true

    init {
        layout = BorderLayout()

        val style = XideStyle.getCurrent()
        val bgColor = UIManager.getColor("TextArea.background")
        val fgColor = UIManager.getColor("Label.foreground")

        stdOutIcon = LucideIcon.INFO.icon(16, fgColor)
        stdErrIcon = LucideIcon.CIRCLE_X.icon(16, Color.RED)

        // List
        listModel = DefaultListModel()
        list = JList(listModel)
        list.background = bgColor
        list.setCellRenderer { listComp, value, index, isSelected, cellHasFocus ->
            val label = JLabel(stripAnsi(value.text))

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

        flushTimer = Timer(FLUSH_INTERVAL_MS) { e -> flushPendingLines() }
        flushTimer.isRepeats = false

        // Top bar
        var topBar = Box.createHorizontalBox()
        topBar.border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
        topBar.add(JCheckBox("StdOut").apply {
            isSelected = showStdOut
            font = style.uiFont()
            addActionListener { e ->
                showStdOut = isSelected
                rebuildVisibleLines()
            }
        })
        topBar.add(Box.createRigidArea(Dimension(8,0)))
        topBar.add(JCheckBox("StdErr").apply{
            isSelected = showStdErr
            font = style.uiFont()
            addActionListener { e -> 
                showStdErr = isSelected
                rebuildVisibleLines()
            }
        })
        topBar.add(Box.createRigidArea(Dimension(8,0)))
        topBar.add(JButton("Clear").apply {
            setForeground(Color.LIGHT_GRAY)
            setFont(style.uiFont())
            addActionListener { e -> 
                synchronized<Unit>(pendingLock) {
                    (pendingLines as MutableList<LogLine>).clear()
                }
                (allLines as MutableList<LogLine>).clear()
                listModel.clear()
            }
        })
        add(topBar, BorderLayout.NORTH)

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
        synchronized(pendingLock) {
            (pendingLines as MutableList<LogLine>).add(LogLine(text, error))
        }

        requestFlush()
    }

    private fun requestFlush() {
        if (!flushRequestPosted.compareAndSet(false, true))
            return

        SwingUtilities.invokeLater {
            flushRequestPosted.set(false)

            if (!flushScheduled) {
                flushScheduled = true
                flushTimer.restart()
            }
        }
    }

    private fun flushPendingLines() {
        flushScheduled = false

        var batch: List<LogLine>
        synchronized(pendingLock) {
            if (pendingLines.isEmpty())
                return

            batch = ArrayList(pendingLines)
            (pendingLines as MutableList<LogLine>).clear()
        }

        (allLines as MutableList<LogLine>).addAll(batch)

        val overflow = allLines.size - MAX_LINES
        val trimmed = overflow > 0
        if (trimmed)
            allLines.subList(0, overflow).clear()

        if (trimmed)
            rebuildVisibleLines()
        else
            appendVisibleLines(batch)

        synchronized(pendingLock) {
            if (!pendingLines.isEmpty()) {
                flushScheduled = true
                flushTimer.restart()
            }
        }
    }

    private fun appendVisibleLines(lines: List<LogLine>) {
        for (line in lines) {
            if (shouldShow(line))
                listModel.addElement(line)
        }

        if (!listModel.isEmpty)
            list.ensureIndexIsVisible(listModel.size - 1)
    }

    private fun rebuildVisibleLines() {
        listModel.clear()

        for (line in allLines) {
            if (shouldShow(line))
                listModel.addElement(line)
        }

        if (!listModel.isEmpty)
            list.ensureIndexIsVisible(listModel.size - 1)
    }

    private fun shouldShow(line: LogLine): Boolean {
        return if (line.isError) showStdErr else showStdOut
    }

    private fun stripAnsi(text: String): String =
        text.replace("\u001B\\[[\\d]*m", "")
}
