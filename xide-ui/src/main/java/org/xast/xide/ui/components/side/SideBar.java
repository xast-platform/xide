package org.xast.xide.ui.components.side;

import java.awt.BorderLayout;
import java.util.Optional;

import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;

import org.xast.xide.core.plugin.ui.SideBarContext;
import org.xast.xide.core.plugin.ui.SideBarView;
import org.xast.xide.ui.utils.XideStyle;

public class SideBar extends JPanel implements SideBarContext {
    private Optional<JSplitPane> splitPane = Optional.empty();
    private SideBarView currentView;
    private int lastDividerLocation = XideStyle.SIDEBAR_WIDTH;

    public SideBar() {
        super(new BorderLayout());

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(sep.getForeground().darker());

        add(sep, BorderLayout.EAST);
    }

    @Override
    public void setView(SideBarView view) {
        if (view != currentView) {
            currentView = view;

            var oldView = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
            if (oldView != null) {
                remove(oldView);
            }

            add(view, BorderLayout.CENTER);
            if (splitPane.isPresent() && splitPane.get().getDividerLocation() == 0) {
                splitPane.get().setDividerLocation(lastDividerLocation);
            }
        } else {
            if (splitPane.isPresent()) {
                if (splitPane.get().getDividerLocation() == 0) {
                    splitPane.get().setDividerLocation(lastDividerLocation);
                } else {
                    lastDividerLocation = splitPane.get().getDividerLocation();
                    splitPane.get().setDividerLocation(0);
                }
            }
        }

        revalidate();
        repaint();
    }

    public void setSplitPane(JSplitPane splitPane) {
        this.splitPane = Optional.of(splitPane);
    }
}
