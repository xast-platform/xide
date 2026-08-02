package org.xast.xide.ui.components.bottom;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.ThemeChangedEvent;
import org.xast.xide.core.plugin.bottom.BottomPanelPlugin;
import org.xast.xide.ui.utils.XideStyle;

public class BottomPanel extends JPanel {
    private JTabbedPane pane;
    private EventBus eventBus;
    private XideStyle style;

    public BottomPanel(EventBus eventBus) {
        setLayout(new BorderLayout());

        style = XideStyle.getCurrent();

        this.eventBus = eventBus;
        this.pane = new JTabbedPane();
        this.pane.setFont(style.uiFont());

        applyTheme();

        eventBus.subscribe(ThemeChangedEvent.class, e -> applyTheme());

        add(pane, BorderLayout.CENTER);
    }

    private void applyTheme() {
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, style.shiftAccent(-0.25f)));
    }

    public void addPlugin(BottomPanelPlugin plugin) {
        pane.addTab(plugin.tabName(), plugin.view(eventBus));
    }
}
