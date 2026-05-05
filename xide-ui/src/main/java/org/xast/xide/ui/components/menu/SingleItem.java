package org.xast.xide.ui.components.menu;

import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.xast.xide.ui.utils.XideStyle;

public class SingleItem implements MenuItem {
    private JMenuItem component;

    public SingleItem(String label, Optional<String> keyStroke, Runnable onClick) {
        XideStyle style = XideStyle.getCurrent();

        component = new JMenuItem(label);
        component.setFont(style.uiFont());
        component.addActionListener(e -> onClick.run());

        if (keyStroke.isPresent()) {
            component.setAccelerator(KeyStroke.getKeyStroke(keyStroke.get()));
        }
    }

    @Override
    public JComponent component() {
        return component;
    }
}
