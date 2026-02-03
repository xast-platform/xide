package org.xast.xide;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import javax.swing.*;

public class Main {
    public static FontLoader fontLoader = new FontLoader();
    public static Font selawik = fontLoader.loadFont("fonts/selawik/selawk.ttf", 16f);
    public static Font monospace = fontLoader.loadFont("fonts/jetbrains_mono/JetBrainsMono-Regular.ttf", 16f);

    public static void main(String[] args) {
        UIManager.put("TitlePane.font", Main.selawik);
        UIManager.put("TitlePane.iconSize", new Dimension(18, 18));
        UIManager.put("TitlePane.titleMargins", new Insets(8,8,8,8));
        UIManager.put("TitlePane.buttonSize", new Dimension(44,36));
        UIManager.put("TabbedPane.tabHeight", 32);
        UIManager.put("TabbedPane.tabInsets", new Insets(6, 14, 6, 14));

        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        FlatMTMaterialDarkerIJTheme.setup();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Xide");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            
            FlatSVGIcon svgIcon = new FlatSVGIcon("icons/logo.svg", 18, 18);
            BufferedImage img = new BufferedImage(
                svgIcon.getIconWidth(),
                svgIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = img.createGraphics();
            svgIcon.paintIcon(null, g, 0, 0);
            g.dispose();

            frame.setIconImage(img);

            JMenuBar menuBar = new JMenuBar();
            menuBar.add(new JMenu("File"));
            menuBar.add(new JMenu("Edit"));
            menuBar.add(new JMenu("View"));
            menuBar.add(new JMenu("Run"));
            menuBar.add(new JMenu("Plugins"));
            menuBar.add(new JMenu("Help"));
            menuBar.setFont(selawik);
            frame.setJMenuBar(menuBar);
            frame.setSize(1280, 960);
            frame.setLocationRelativeTo(null);

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new GridLayout());

            JTabbedPane leftDock = new JTabbedPane();
            leftDock.addTab("Project", new JScrollPane(new JTree()));
            leftDock.addTab("Files", new JLabel("Files view"));
            leftDock.setFont(selawik);

            JTabbedPane editorDock = new JTabbedPane();
            editorDock.addTab("Main.xst", new JScrollPane(new JTextArea("fn main() {}"){{
                setFont(monospace);
            }}));
            editorDock.addTab("Test.xst", new JScrollPane(new JTextArea("// test")));
            editorDock.setFont(selawik);

            JTabbedPane bottomDock = new JTabbedPane();
            bottomDock.addTab("Console", new JScrollPane(new JTextArea("> ready")));
            bottomDock.addTab("Build", new JLabel("Build output"));
            bottomDock.addTab("Logs", new JLabel("Logs"));
            bottomDock.setFont(selawik);

            JSplitPane horizontalSplit = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    leftDock,
                    editorDock
            );
            horizontalSplit.setResizeWeight(0.2);
            horizontalSplit.setContinuousLayout(true);
            horizontalSplit.setBorder(null);

            JSplitPane verticalSplit = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    horizontalSplit,
                    bottomDock
            );
            verticalSplit.setResizeWeight(0.75);
            verticalSplit.setContinuousLayout(true);
            verticalSplit.setBorder(null);

            mainPanel.add(verticalSplit, BorderLayout.CENTER);
            
            frame.setContentPane(mainPanel);
            frame.setVisible(true);
        });
    }
}