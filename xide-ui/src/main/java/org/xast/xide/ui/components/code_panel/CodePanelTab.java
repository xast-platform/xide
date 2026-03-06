package org.xast.xide.ui.components.code_panel;

import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.EventHandler;
import org.xast.xide.core.event.FileSaveRequestedEvent;
import org.xast.xide.core.event.TabCloseRequestedEvent;
import org.xast.xide.core.utils.LucideIcon;
import org.xast.xide.ui.utils.XideStyle;

public class CodePanelTab extends JPanel implements EventHandler {
    private JButton close;
    private CodePanelTabModel model;

    public CodePanelTab(EventBus eventBus, JTabbedPane pane, CodePanelTabModel model) {
        XideStyle style = XideStyle.getCurrent();

        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        CodePanelTabTitle title = new CodePanelTabTitle(model.isSaved(), pane, this);
        title.setFont(style.uiFont());

        this.close = new JButton(LucideIcon.X.icon(12, Color.WHITE));
        this.model = model;

        add(title);
        add(Box.createHorizontalStrut(6));
        add(close);

        setupEventListeners(eventBus);
    }

    @Override
    public void setupEventListeners(EventBus eventBus) {
        eventBus.subscribe(FileSaveRequestedEvent.class, e -> {
            if (!model.isSaved()) {
                model.setSaved(true);
            }
        });

        close.addActionListener(e -> {
            eventBus.publish(new TabCloseRequestedEvent(model.getFile()));
        });
    }
}
