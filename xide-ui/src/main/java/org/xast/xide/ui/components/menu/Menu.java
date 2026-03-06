package org.xast.xide.ui.components.menu;

import javax.swing.JComponent;
import javax.swing.JMenu;

import org.xast.xide.ui.utils.XideStyle;

public class Menu implements MenuItem {
    private JMenu component;

    public Menu(String label, MenuItem[] items) {
        XideStyle style = XideStyle.getCurrent();

        component = new JMenu(label);
        component.setFont(style.uiFont());

        for (MenuItem item : items) {
            component.add(item.component());
        }
    }

    @Override
    public JComponent component() {
        return component;
    }
}
