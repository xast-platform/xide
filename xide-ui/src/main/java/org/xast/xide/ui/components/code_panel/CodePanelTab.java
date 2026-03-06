package org.xast.xide.ui.components.code_panel;

import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.util.HashSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.FileSaveRequestedEvent;
import org.xast.xide.core.event.TabCloseRequestedEvent;
import org.xast.xide.ui.utils.LucideIcon;
import org.xast.xide.ui.utils.XideStyle;

public class CodePanelTab extends JPanel {
    private boolean saved;

    public CodePanelTab(JTabbedPane pane, HashSet<File> openedFiles, File file, boolean saved) {
        XideStyle style = XideStyle.getCurrent();

        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JLabel title = new JLabel() {
            @Override
            public String getText() {
                int index = pane.indexOfTabComponent(CodePanelTab.this);
                if (index != -1) {
                    String title = pane.getTitleAt(index);
                    if (!saved) {
                        title += "*";
                    }
                    return title;
                }
                return "";
            }
        };
        title.setFont(style.uiFont());

        JButton close = new JButton(LucideIcon.X.icon(12, Color.WHITE));
        close.addActionListener(e -> {
            EventBus.getInstance().publish(new TabCloseRequestedEvent(file));
        });

        add(title);
        add(Box.createHorizontalStrut(6));
        add(close);

        this.saved = saved;

        EventBus.getInstance().subscribe(FileSaveRequestedEvent.class, e -> {
            if (!this.saved) {
                this.saved = true;
            }
        });
    }
}
