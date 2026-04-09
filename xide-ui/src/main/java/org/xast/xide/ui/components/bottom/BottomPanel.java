package org.xast.xide.ui.components.bottom;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.plugin.bottom.BottomPanelPlugin;
import org.xast.xide.ui.utils.XideStyle;

public class BottomPanel extends JPanel {
    private JTabbedPane pane;
    private EventBus eventBus;

    public BottomPanel(EventBus eventBus) {
        setLayout(new BorderLayout());

        XideStyle style = XideStyle.getCurrent();

        this.eventBus = eventBus;
        this.pane = new JTabbedPane();
        this.pane.setFont(style.uiFont());

        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x424242)));

        add(pane, BorderLayout.CENTER);
    }

    public void addPlugin(BottomPanelPlugin plugin) {
        pane.addTab(plugin.tabName(), plugin.view(eventBus));
    }
}
