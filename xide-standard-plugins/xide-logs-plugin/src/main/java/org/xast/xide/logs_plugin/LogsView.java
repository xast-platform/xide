package org.xast.xide.logs_plugin;

import org.xast.xide.core.plugin.bottom.BottomPanelView;
import org.xast.xide.core.utils.LucideIcon;
import org.xast.xide.ui.utils.XideStyle;

import java.awt.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

public class LogsView extends BottomPanelView {
    private static final int MAX_LINES = 1000;
    private static final int FLUSH_INTERVAL_MS = 500;
    
    private DefaultListModel<LogLine> listModel;
    private JList<LogLine> list;
    private final List<LogLine> allLines = new ArrayList<>();
    private final List<LogLine> pendingLines = new ArrayList<>();
    private final Object pendingLock = new Object();
    private final AtomicBoolean flushRequestPosted = new AtomicBoolean(false);
    private Timer flushTimer;
    private Icon stdOutIcon;
    private Icon stdErrIcon;
    private boolean flushScheduled;
    private boolean showStdOut = true;
    private boolean showStdErr = true;

    public LogsView() {
        setLayout(new BorderLayout());

        XideStyle style = XideStyle.getCurrent();
        Color bgColor = UIManager.getColor("TextArea.background");
        Color fgColor = UIManager.getColor("Label.foreground");

        stdOutIcon = LucideIcon.INFO.icon(16, fgColor);
        stdErrIcon = LucideIcon.CIRCLE_X.icon(16, Color.RED);

        // List
        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setBackground(bgColor);
        list.setCellRenderer((listComp, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(stripAnsi(value.text()));

            label.setFont(style.uiFont().deriveFont(16f));
            label.setBackground(bgColor);
            label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            label.setIcon(value.isError() ? stdErrIcon : stdOutIcon);
            label.setIconTextGap(8);

            if (isSelected)
                label.setBackground(listComp.getSelectionBackground());

            label.setOpaque(true);

            if (value.isError())
                label.setForeground(Color.RED);

            return label;
        });
        add(
            new JScrollPane(list) {{
                setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x424242)));
            }}, 
            BorderLayout.CENTER
        );

        flushTimer = new Timer(FLUSH_INTERVAL_MS, e -> flushPendingLines());
        flushTimer.setRepeats(false);

        // Top bar
        Box topBar = Box.createHorizontalBox();
        topBar.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        topBar.add(new JCheckBox("StdOut") {{
            setSelected(showStdOut);
            setFont(style.uiFont());
            addActionListener(e -> {
                showStdOut = isSelected();
                rebuildVisibleLines();
            });
        }});
        topBar.add(Box.createRigidArea(new Dimension(8,0)));
        topBar.add(new JCheckBox("StdErr") {{
            setSelected(showStdErr);
            setFont(style.uiFont());
            addActionListener(e -> {
                showStdErr = isSelected();
                rebuildVisibleLines();
            });
        }});
        topBar.add(Box.createRigidArea(new Dimension(8,0)));
        topBar.add(new JButton("Clear") {{
            setForeground(Color.LIGHT_GRAY);
            setFont(style.uiFont());
            addActionListener(e -> {
                synchronized (pendingLock) {
                    pendingLines.clear();
                }
                allLines.clear();
                listModel.clear();
            });
        }});
        add(topBar, BorderLayout.NORTH);

        // Print stream
        System.setOut(new PrintStream(
            new ConsoleRedirectStream(this, false),
            true,
            StandardCharsets.UTF_8
        ));
        System.setErr(new PrintStream(
            new ConsoleRedirectStream(this, true),
            true,
            StandardCharsets.UTF_8
        ));
    }

    public void addLine(String text, boolean error) {
        synchronized (pendingLock) {
            pendingLines.add(new LogLine(text, error));
        }

        requestFlush();
    }

    private void requestFlush() {
        if (!flushRequestPosted.compareAndSet(false, true))
            return;

        SwingUtilities.invokeLater(() -> {
            flushRequestPosted.set(false);

            if (!flushScheduled) {
                flushScheduled = true;
                flushTimer.restart();
            }
        });
    }

    private void flushPendingLines() {
        flushScheduled = false;

        List<LogLine> batch;
        synchronized (pendingLock) {
            if (pendingLines.isEmpty())
                return;

            batch = new ArrayList<>(pendingLines);
            pendingLines.clear();
        }

        allLines.addAll(batch);

        int overflow = allLines.size() - MAX_LINES;
        boolean trimmed = overflow > 0;
        if (trimmed)
            allLines.subList(0, overflow).clear();

        if (trimmed)
            rebuildVisibleLines();
        else
            appendVisibleLines(batch);

        synchronized (pendingLock) {
            if (!pendingLines.isEmpty()) {
                flushScheduled = true;
                flushTimer.restart();
            }
        }
    }

    private void appendVisibleLines(List<LogLine> lines) {
        for (LogLine line : lines) {
            if (shouldShow(line))
                listModel.addElement(line);
        }

        if (!listModel.isEmpty())
            list.ensureIndexIsVisible(listModel.size() - 1);
    }

    private void rebuildVisibleLines() {
        listModel.clear();

        for (LogLine line : allLines) {
            if (shouldShow(line))
                listModel.addElement(line);
        }

        if (!listModel.isEmpty())
            list.ensureIndexIsVisible(listModel.size() - 1);
    }

    private boolean shouldShow(LogLine line) {
        return line.isError() ? showStdErr : showStdOut;
    }

    private String stripAnsi(String text) {
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}
