package org.xast.xide.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.*;

import org.xast.xide.core.PluginRegistry;
import org.xast.xide.core.Workspace;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.FileOpenRequestedEvent;
import org.xast.xide.core.event.WorkspaceChangedEvent;
import org.xast.xide.core.plugin.ui.SideBarContext;
import org.xast.xide.core.plugin.ui.UIContext;
import org.xast.xide.ui.components.bottom.BottomPanel;
import org.xast.xide.ui.components.code_panel.CodePanel;
import org.xast.xide.ui.components.menu.MenuBar;
import org.xast.xide.ui.components.menu.MenuItem;
import org.xast.xide.ui.components.menu.SingleItem;
import org.xast.xide.ui.components.side.SideBar;
import org.xast.xide.ui.components.side.ToolBar;
import org.xast.xide.ui.components.side.ToolButton;
import org.xast.xide.ui.utils.FileChooser;
import org.xast.xide.ui.utils.XideStyle;
import org.xast.xide.ui.utils.FileChooser.FileChooserMode;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme;

import lombok.Getter;

public class MainFrame implements UIContext {
    private JFrame frame;

    // Top-level panels
    @Getter
    private CodePanel codePanel;
    @Getter
    private SideBar sideBar;
    @Getter
    private ToolBar toolBar;
    @Getter
    private BottomPanel bottomPanel;
    @Getter
    private MenuBar menuBar;

    // Services
    private PluginRegistry pluginRegistry;
    private Workspace workspace;
    private EventBus eventBus;

    static {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        FlatMTMaterialDarkerIJTheme.setup();
    }

    public MainFrame(
        Workspace workspace,
        PluginRegistry pluginRegistry,
        EventBus eventBus
    ) {
        this.workspace = workspace;
        this.pluginRegistry = pluginRegistry;
        this.eventBus = eventBus;

        setupStyle();

        frame = new JFrame();
        codePanel = new CodePanel(eventBus, pluginRegistry);
        sideBar = new SideBar();
        bottomPanel = new BottomPanel(eventBus);
        menuBar = new MenuBar(frame);
        toolBar = new ToolBar();

        setupLayout();
    }
    
    public void loadPlugins() {
        for (var toolPlugin : pluginRegistry.getToolPlugins()) {            
            toolBar.addToolButton(
                new ToolButton(
                    toolPlugin.icon(), 
                    toolPlugin.toolTip(), 
                    toolPlugin.tool(this)
                ),
                toolPlugin.orientation(),
                toolPlugin.runAtStartup()
            );
        }
        
        for (var plugin : pluginRegistry.getBottomPanelPlugins()) {
            bottomPanel.addPlugin(plugin);
        }
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

    @Override
    public JFrame frame() {
        return frame;
    }

    @Override
    public Workspace currentWorkspace() {
        return workspace;
    }

    @Override
    public SideBarContext sideBar() {
        return sideBar;
    }

    @Override
    public EventBus eventBus() {
        return eventBus;
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
        menuBar.addMenu("File", new MenuItem[] {
            new SingleItem("New file...", "control N", () -> {
                FileChooser fileChooser = new FileChooser(FileChooserMode.FILES);
                fileChooser.save(frame, file -> {
                    workspace = workspace.withFile(file);
                    eventBus.publish(new WorkspaceChangedEvent(workspace));
                    eventBus.publish(new FileOpenRequestedEvent(file));
                });
            }),

            new SingleItem("Open file", "control O", () -> {
                FileChooser fileChooser = new FileChooser(FileChooserMode.FILES);
                fileChooser.open(frame, file -> {
                    workspace = workspace.withFile(file);
                    eventBus.publish(new WorkspaceChangedEvent(workspace));
                    eventBus.publish(new FileOpenRequestedEvent(file));
                });
            }),

            new SingleItem("Open folder", "control shift O", () -> {
                FileChooser fileChooser = new FileChooser(FileChooserMode.DIRS);
                fileChooser.open(frame, file -> {
                    workspace = new Workspace.Directory(file);
                    eventBus.publish(new WorkspaceChangedEvent(workspace));
                });
            }),

            new SingleItem("Save", "control S", () -> {
                codePanel.saveCurrentFile();
            }),
        });

        // Split panes
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, codePanel, bottomPanel) {{
            setResizeWeight(0.2);
            setContinuousLayout(false);
            setBorder(null);
        }};

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sideBar, verticalSplit) {{
            setResizeWeight(0.75);
            setContinuousLayout(false);
            setBorder(null);
            setDividerLocation(XideStyle.SIDEBAR_WIDTH);
        }};

        sideBar.setSplitPane(horizontalSplit);
        frame.add(horizontalSplit, BorderLayout.CENTER);
        frame.add(toolBar, BorderLayout.WEST);

        SwingUtilities.invokeLater(() -> {
            int totalHeight = verticalSplit.getHeight();
            verticalSplit.setDividerLocation(totalHeight - XideStyle.BOTTOM_BAR_HEIGHT);
        });
    }
}