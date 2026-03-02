package org.xast.xide.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.*;

import org.xast.xide.core.PluginRegistry;
import org.xast.xide.core.Workspace;
import org.xast.xide.ui.component.code_panel.CodePanel;
import org.xast.xide.ui.component.side.SideBar;
import org.xast.xide.ui.component.side.ToolButton;
import org.xast.xide.ui.dialog.SettingsDialog;
import org.xast.xide.ui.state.tool.DummyTool;
import org.xast.xide.ui.state.tool.SettingsTool;
import org.xast.xide.ui.utils.LucideIcon;
import org.xast.xide.ui.utils.XideStyle;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme;

import lombok.Getter;

public class MainFrame {
    private JFrame frame;

    // Top-level panels
    @Getter
    private final CodePanel codePanel;
    @Getter
    private final SideBar sideBar;
    @Getter
    private final JTabbedPane bottomPanel;
    @Getter
    private final JMenuBar menuBar;

    // Services
    private final PluginRegistry pluginRegistry;
    private final Workspace workspace;

    static {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        FlatMTMaterialDarkerIJTheme.setup();
    }

    public MainFrame(
        Workspace workspace,
        PluginRegistry pluginRegistry,
        XideStyle style
    ) {
        this.workspace = workspace;
        this.pluginRegistry = pluginRegistry;

        setupStyle(style);

        frame = new JFrame();
        codePanel = new CodePanel(style);
        sideBar = new SideBar();
        bottomPanel = new JTabbedPane();
        menuBar = new JMenuBar();

        setupLayout(style);
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public void show() {
        frame.setVisible(true);

        SwingUtilities.invokeLater(() ->
            frame.getRootPane().requestFocusInWindow()
        );
    }

    public JFrame getFrame() {
        return frame;
    }

    private void setupStyle(XideStyle style) {
        // Title bar
        UIManager.put("TitlePane.iconSize", new Dimension(
            XideStyle.ICON_WIDTH, 
            XideStyle.ICON_HEIGHT
        ));
        UIManager.put("TitlePane.titleMargins", new Insets(8,8,8,8));
        UIManager.put("TitlePane.font", style.uiFont());

        // Tabbed panes
        UIManager.put("TabbedPane.tabHeight", 32);
        UIManager.put("TabbedPane.tabInsets", new Insets(6, 14, 6, 14));
        UIManager.put("TabbedPane.showTabSeparators", true);

        // Focused items
        UIManager.put("Component.focusWidth", 3);
        UIManager.put("Component.focusColor", new Color(172, 108, 64, 172)); // softer orange
    }

    private void setupLayout(XideStyle style) {
        // ----- Main frame properties
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1280, 960);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(style.favicon().getImage());
        frame.setLayout(new BorderLayout());

        // ----- Menu bar
        menuBar.setFont(style.uiFont());
        menuBar.add(new JMenu("File"));
        frame.setJMenuBar(menuBar);

        // ----- Central panel 
        // ...

        // ----- Side panel
        // North toolbar
        sideBar.addToolButtonNorth(new ToolButton(LucideIcon.FOLDER_TREE, "Project tree", new DummyTool()));
        sideBar.addToolButtonNorth(new ToolButton(LucideIcon.FILE_SEARCH_CORNER, "Search", new DummyTool()));

        // South toolbar
        sideBar.addToolButtonSouth(new ToolButton(LucideIcon.COG, "Settings", new SettingsTool(this)));


        frame.add(codePanel, BorderLayout.CENTER);
        frame.add(sideBar, BorderLayout.WEST);
    }
}