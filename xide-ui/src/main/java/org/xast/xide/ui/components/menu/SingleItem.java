package org.xast.xide.ui.components.menu;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.xast.xide.ui.utils.XideStyle;

public class SingleItem implements MenuItem {
    private JMenuItem component;

    public SingleItem(String label, String keyStroke, Runnable onClick) {
        XideStyle style = XideStyle.getCurrent();

        component = new JMenuItem(label);
        component.setFont(style.uiFont());
        component.addActionListener(e -> onClick.run());
        component.setAccelerator(KeyStroke.getKeyStroke(keyStroke));
    }

    @Override
    public JComponent component() {
        return component;
    }
}
