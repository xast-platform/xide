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
import org.xast.xide.ui.component.side.ToolBar;
import org.xast.xide.ui.component.side.ToolButton;
import org.xast.xide.ui.state.tool.DummyTool;
import org.xast.xide.ui.state.tool.FolderTreeTool;
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
    private final ToolBar toolBar;
    @Getter
    private final JTabbedPane bottomPanel;
    @Getter
    private final JMenuBar menuBar;

    // Services
    private final PluginRegistry pluginRegistry;
    private Workspace workspace;

    static {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        FlatMTMaterialDarkerIJTheme.setup();
    }

    public MainFrame(
        Workspace workspace,
        PluginRegistry pluginRegistry
    ) {
        this.workspace = workspace;
        this.pluginRegistry = pluginRegistry;

        setupStyle();

        frame = new JFrame();
        codePanel = new CodePanel();
        sideBar = new SideBar();
        bottomPanel = new JTabbedPane();
        menuBar = new JMenuBar();
        toolBar = new ToolBar();

        setupLayout();
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

    private void setupStyle() {
        XideStyle style = XideStyle.getCurrent();

        // Title bar
        UIManager.put("TitlePane.iconSize", new Dimension(
            XideStyle.ICON_WIDTH, 
            XideStyle.ICON_HEIGHT
        ));
        UIManager.put("TitlePane.titleMargins", new Insets(8,8,8,8));
        UIManager.put("TitlePane.font", style.uiFont());

        // Tabbed panes
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("TabbedPane.tabInsets", new Insets(6, 14, 6, 14));
        UIManager.put("TabbedPane.showTabSeparators", true);

        // Focused items
        UIManager.put("Component.focusWidth", 3);
        UIManager.put("Component.focusColor", new Color(172, 108, 64, 172)); // softer orange

        // File chooser
        UIManager.put("FileChooser.font", style.uiFont());
        UIManager.put("FileChooser.listFont", style.uiFont());
        UIManager.put("FileChooser.textFont", style.uiFont());
        UIManager.put("FileChooser.buttonFont", style.uiFont());
        UIManager.put("FileChooser.labelFont", style.uiFont());
    }

    private void setupLayout() {
        XideStyle style = XideStyle.getCurrent();
        
        // Main frame properties
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(style.windowSize().width, style.windowSize().height);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(style.favicon().getImage());
        frame.setLayout(new BorderLayout());

        // Menu bar
        menuBar.setFont(style.uiFont());

        var file = new JMenu("File");
        var openFolder = new JMenuItem("Open folder");
        openFolder.addActionListener(a -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                workspace = new Workspace.Directory(fileChooser.getSelectedFile());
                var tool = (FolderTreeTool) toolBar.getTool(FolderTreeTool.class);
                tool.setWorkspace(workspace);
                tool.show();
            }
        });
        file.add(openFolder);

        menuBar.add(file);
        frame.setJMenuBar(menuBar);

        // Central panel 
        // ...

        // Side panel
        toolBar.addToolButtonNorth(new ToolButton(
            LucideIcon.FOLDER_TREE, 
            "Project tree", 
            new FolderTreeTool(sideBar, workspace)
        ));
        toolBar.addToolButtonNorth(new ToolButton(
            LucideIcon.FILE_SEARCH_CORNER, 
            "Search", 
            new DummyTool()
        ));
        toolBar.addToolButtonSouth(new ToolButton(
            LucideIcon.COG, 
            "Settings", 
            new SettingsTool(this)
        ));
        toolBar.setDefaultTool(FolderTreeTool.class);

        // Split panes
        JSplitPane verticalSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            codePanel,
            bottomPanel
        );
        verticalSplit.setResizeWeight(0.2);
        verticalSplit.setContinuousLayout(false);
        verticalSplit.setBorder(null);

        JSplitPane horizontalSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            sideBar,
            verticalSplit
        );
        horizontalSplit.setResizeWeight(0.75);
        horizontalSplit.setContinuousLayout(false);
        horizontalSplit.setBorder(null);
        horizontalSplit.setDividerLocation(XideStyle.SIDEBAR_WIDTH);

        sideBar.setSplitPane(horizontalSplit);

        frame.add(horizontalSplit, BorderLayout.CENTER);
        frame.add(toolBar, BorderLayout.WEST);

        SwingUtilities.invokeLater(() -> {
            int totalHeight = verticalSplit.getHeight();
            verticalSplit.setDividerLocation(totalHeight - XideStyle.BOTTOM_BAR_HEIGHT);
        });
    }
}