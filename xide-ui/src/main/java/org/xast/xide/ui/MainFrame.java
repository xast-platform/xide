package org.xast.xide.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Optional;
import java.util.stream.Stream;

import javax.swing.*;

import org.xast.xide.core.PluginRegistry;
import org.xast.xide.core.Workspace;
import org.xast.xide.core.config.XideConfig;
import org.xast.xide.core.event.*;
import org.xast.xide.core.plugin.ui.SideBarContext;
import org.xast.xide.core.plugin.ui.UIContext;
import org.xast.xide.core.utils.LucideIcon;
import org.xast.xide.core.utils.RecentWorkspaces;
import org.xast.xide.ui.components.bottom.BottomPanel;
import org.xast.xide.ui.components.code_panel.CodePanel;
import org.xast.xide.ui.components.menu.*;
import org.xast.xide.ui.components.side.*;
import org.xast.xide.ui.utils.*;
import org.xast.xide.ui.utils.FileChooser.FileChooserMode;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme;

import lombok.Getter;

public class MainFrame implements UIContext, EventHandler {
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
    @Getter
    private Menu openRecentMenu;

    // Services
    private PluginRegistry pluginRegistry;
    private Workspace workspace;
    private EventBus eventBus;
    private XideConfig config;
    private RecentWorkspaces recentWorkspaces;

    static {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        FlatMTGitHubDarkIJTheme.setup();
    }

    public MainFrame(
        Workspace workspace,
        PluginRegistry pluginRegistry,
        EventBus eventBus,
        XideConfig config
    ) {
        this.workspace = workspace;
        this.pluginRegistry = pluginRegistry;
        this.eventBus = eventBus;
        this.recentWorkspaces = new RecentWorkspaces(eventBus);

        setupStyle();

        frame = new JFrame();
        codePanel = new CodePanel(eventBus, pluginRegistry);
        sideBar = new SideBar();
        bottomPanel = new BottomPanel(eventBus);
        menuBar = new MenuBar(frame);
        toolBar = new ToolBar(eventBus);
        openRecentMenu = new Menu("Open recent...", new MenuItem[] {});

        updateRecentItems();
        setupLayout();
        setupEventListeners(eventBus);
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

    @Override
    public XideConfig config() {
        return config;
    }

    private void setupStyle() {
        XideStyle style = XideStyle.getCurrent();

        // Title bar
        UIManager.put("TitlePane.iconSize", new Dimension(
            XideStyle.ICON_WIDTH, 
            XideStyle.ICON_HEIGHT
        ));
        UIManager.put("TitlePane.titleMargins", new Insets(12,12,12,12));
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

        // Menu
        UIManager.put("PopupMenu.borderInsets", new Insets(6, 0, 6, 0));
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
        menuBar.addMenu("File", fileMenu());
        menuBar.addMenu("Edit", new MenuItem[] {});
        menuBar.addMenu("View", new MenuItem[] {});
        menuBar.addMenu("Plugins", new MenuItem[] {});
        menuBar.addMenu("Help", new MenuItem[] {});
        menuBar.add(Box.createGlue());

        // Menu buttons
        menuBar.add(new MenuButton(LucideIcon.HAMMER, "Build application", () -> {

        }));
        menuBar.add(new MenuButton(LucideIcon.PLAY, "Run application", MenuButton.PLAY_BUTTON, () -> {

        }));
        menuBar.add(new MenuButton(LucideIcon.SUN_MOON, "Change theme", () -> {

        }));
        menuBar.add(new MenuButton(LucideIcon.ELLIPSIS_VERTICAL, "More...", new Insets(8, 2, 8, 2), () -> {

        }));

        menuBar.add(new ThemeSwitcher(eventBus));

        // Split panes
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, codePanel, bottomPanel) {{
            setResizeWeight(0.2);
            setContinuousLayout(false);
        }};

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sideBar, verticalSplit) {{
            setResizeWeight(0.75);
            setContinuousLayout(false);
            setDividerLocation(XideStyle.SIDEBAR_WIDTH);
        }};

        sideBar.setSplitPane(horizontalSplit);
        frame.add(horizontalSplit, BorderLayout.CENTER);
        frame.add(toolBar, BorderLayout.WEST);

        applyBorders(menuBar, verticalSplit);
        eventBus.subscribe(ThemeChangedEvent.class, e -> applyBorders(menuBar, verticalSplit));

        SwingUtilities.invokeLater(() -> {
            int totalHeight = verticalSplit.getHeight();
            verticalSplit.setDividerLocation(totalHeight - XideStyle.BOTTOM_BAR_HEIGHT);
        });
    }

    private void applyBorders(MenuBar menuBar, JSplitPane verticalSplit) {
        Color borderColor = XideStyle.getCurrent().shiftAccent(-0.25f);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor));
        verticalSplit.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, borderColor));
    }

    public void setupEventListeners(EventBus eventBus) {
        eventBus.subscribe(WorkspaceChangedEvent.class, e -> {
            updateRecentItems();
        });
    }

    private MenuItem[] fileMenu() {
        return new MenuItem[] {
            new SingleItem("New file...", Optional.of("control N"), () -> {
                FileChooser fileChooser = new FileChooser(FileChooserMode.FILES);
                fileChooser.save(frame, file -> {
                    workspace = workspace.withFile(file);
                    eventBus.publish(new WorkspaceChangedEvent(workspace));
                    eventBus.publish(new FileOpenRequestedEvent(file));
                });
            }),

            new SeparatorItem(),

            new SingleItem("Open file", Optional.of("control O"), () -> {
                FileChooser fileChooser = new FileChooser(FileChooserMode.FILES);
                fileChooser.open(frame, file -> {
                    workspace = workspace.withFile(file);
                    eventBus.publish(new WorkspaceChangedEvent(workspace));
                    eventBus.publish(new FileOpenRequestedEvent(file));
                });
            }),

            new SingleItem("Open folder", Optional.of("control shift O"), () -> {
                FileChooser fileChooser = new FileChooser(FileChooserMode.DIRS);
                fileChooser.open(frame, file -> {
                    workspace = new Workspace.Directory(file);
                    eventBus.publish(new WorkspaceChangedEvent(workspace));
                });
            }),

            openRecentMenu,

            new SeparatorItem(),

            new SingleItem("Save", Optional.of("control S"), () -> {
                codePanel.saveCurrentFile();
            }),

            new SingleItem("Save all", Optional.of("control shift S"), () -> {
                codePanel.saveAllFiles();
            }),

            new SeparatorItem(),

            new SingleItem("Exit", Optional.of("control Q"), () -> {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }),
        };
    }

    private void updateRecentItems() {
        var workspaces = recentWorkspaces.getWorkspaces();
        var firstRecentItem = new AtomicBoolean(true);

        openRecentMenu.setItems(
            Stream.concat(
                workspaces.stream()
                    .map(ws -> ws.getPath().map(path -> new SingleItem(
                        path.toString(),
                        firstRecentItem.getAndSet(false)
                            ? Optional.of("control shift R")
                            : Optional.empty(),
                        () -> {
                            switch (ws) {
                                case Workspace.ExistingFile f -> {
                                    eventBus.publish(new FileOpenRequestedEvent(f.file()));
                                }

                                case Workspace.Directory _ -> {
                                    eventBus.publish(new WorkspaceChangedEvent(ws));
                                }
                            
                                default -> {}
                            }
                        }
                    ))
                ).flatMap(Optional::stream),

                workspaces.isEmpty()
                    ? Stream.empty()
                    : Stream.of(
                        new SeparatorItem(),
                        new SingleItem("Clear recent workspaces...", Optional.empty(), () -> {
                            recentWorkspaces.clearWorkspaces();
                            updateRecentItems();
                        })
                    )
            ).toArray(MenuItem[]::new)
        );
    }
}