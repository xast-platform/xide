package org.xast.xide.ui.components.bottom;

import com.jediterm.core.Color;
import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.emulator.ColorPalette;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import org.xast.xide.ui.utils.XideStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TerminalView extends JPanel {
    private JediTermWidget terminal;
    private PtyProcess process;

    public TerminalView() {
        setLayout(new BorderLayout());

        try {
            String shell = System.getProperty("os.name")
                .toLowerCase()
                .contains("win")
                    ? "cmd.exe"
                    : "/bin/bash";

            HashMap<String, String> envs = new HashMap<>(System.getenv());
            envs.put("TERM", "xterm-256color");

            String[] command = System.getProperty("os.name").toLowerCase().contains("win")
                ? new String[]{shell}
                : new String[]{shell, "-l"};

            process = new PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(envs)
                .setInitialColumns(80)
                .setInitialRows(24)
                .start();

            ProcessTtyConnector connector = new ProcessTtyConnector(process, StandardCharsets.UTF_8) {
                @Override
                public String getName() {
                    return "Local Terminal";
                }

                @Override
                public void resize(TermSize termSize) {
                    if (process.isAlive()) {
                        process.setWinSize(new WinSize(
                            termSize.getColumns(),
                            termSize.getRows()
                        ));
                    }
                }
            };

            terminal = new JediTermWidget(new ThemedSettingsProvider());
            terminal.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            terminal.setTtyConnector(connector);
            terminal.start();

            add(terminal, BorderLayout.CENTER);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateTerminalSize();
                }
            });

            SwingUtilities.invokeLater(this::updateTerminalSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTerminalSize() {
        if (terminal != null && process != null && process.isAlive()) {
            TermSize termSize = terminal.getTerminalPanel().getTerminalSizeFromComponent();
            
            if (termSize != null && termSize.getColumns() > 0 && termSize.getRows() > 0) {
                process.setWinSize(new WinSize(
                    termSize.getColumns(),
                    termSize.getRows()
                ));
            }
        }
    }

    private static class ThemedSettingsProvider extends DefaultSettingsProvider {
        @Override
        public Font getTerminalFont() {
            return XideStyle.getCurrent()
                .codeFont()
                .deriveFont(17f);
        }

        @Override
        public float getTerminalFontSize() {
            return XideStyle.getCurrent()
                .codeFont()
                .deriveFont(16f)
                .getSize2D();
        }

        @Override
        public ColorPalette getTerminalColorPalette() {
            return new ThemedColorPalette();
        }

        @Override
        public TerminalColor getDefaultForeground() {
            java.awt.Color fg = UIManager.getColor("TextArea.foreground");
            if (fg == null) fg = new java.awt.Color(200, 200, 200);
            return new TerminalColor(fg.getRed(), fg.getGreen(), fg.getBlue());
        }

        @Override
        public TerminalColor getDefaultBackground() {
            java.awt.Color bg = UIManager.getColor("TextArea.background");
            if (bg == null) bg = new java.awt.Color(40, 40, 40);
            return new TerminalColor(bg.getRed(), bg.getGreen(), bg.getBlue());
        }

        @Override
        public TextStyle getDefaultStyle() {
            return new TextStyle(getDefaultForeground(), getDefaultBackground());
        }

        @Override
        public TextStyle getSelectionColor() {
            java.awt.Color selectionBg = UIManager.getColor("TextArea.selectionBackground");
            java.awt.Color selectionFg = UIManager.getColor("TextArea.selectionForeground");
            
            if (selectionBg == null) selectionBg = new java.awt.Color(80, 120, 200);
            if (selectionFg == null) selectionFg = java.awt.Color.WHITE;
            
            return new TextStyle(
                new TerminalColor(selectionFg.getRed(), selectionFg.getGreen(), selectionFg.getBlue()),
                new TerminalColor(selectionBg.getRed(), selectionBg.getGreen(), selectionBg.getBlue())
            );
        }
    }

    private static class ThemedColorPalette extends ColorPalette {
        private final Color defaultForeground;
        private final Color defaultBackground;
        private final Color[] colors = new Color[256];

        public ThemedColorPalette() {
            java.awt.Color fg = UIManager.getColor("TextArea.foreground");
            java.awt.Color bg = UIManager.getColor("TextArea.background");
            
            if (fg == null) fg = java.awt.Color.WHITE;
            if (bg == null) bg = new java.awt.Color(40, 40, 40);
            
            defaultForeground = new Color(fg.getRed(), fg.getGreen(), fg.getBlue());
            defaultBackground = new Color(bg.getRed(), bg.getGreen(), bg.getBlue());
            
            colors[0] = new Color(0, 0, 0);
            colors[1] = new Color(205, 49, 49);
            colors[2] = new Color(13, 188, 121);
            colors[3] = new Color(229, 229, 16);
            colors[4] = new Color(36, 114, 200);
            colors[5] = new Color(188, 63, 188);
            colors[6] = new Color(17, 168, 205);
            colors[7] = new Color(229, 229, 229);
            colors[8] = new Color(102, 102, 102);
            colors[9] = new Color(241, 76, 76);
            colors[10] = new Color(35, 209, 139);
            colors[11] = new Color(245, 245, 67);
            colors[12] = new Color(59, 142, 234);
            colors[13] = new Color(214, 112, 214);
            colors[14] = new Color(41, 184, 219);
            colors[15] = new Color(229, 229, 229);
            
            for (int i = 16; i < 256; i++) {
                if (i < 232) {
                    int index = i - 16;
                    int r = (index / 36) * 51;
                    int g = ((index % 36) / 6) * 51;
                    int b = (index % 6) * 51;
                    colors[i] = new Color(r, g, b);
                } else {
                    int gray = 8 + (i - 232) * 10;
                    colors[i] = new Color(gray, gray, gray);
                }
            }
        }

        @Override
        protected Color getForegroundByColorIndex(int index) {
            if (index >= 0 && index < colors.length && colors[index] != null) {
                return colors[index];
            }
            return defaultForeground;
        }

        @Override
        protected Color getBackgroundByColorIndex(int index) {
            if (index >= 0 && index < colors.length && colors[index] != null) {
                return colors[index];
            }
            return defaultBackground;
        }
    }
}
