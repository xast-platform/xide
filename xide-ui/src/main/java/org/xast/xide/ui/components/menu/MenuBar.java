package org.xast.xide.ui.components.menu;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.xast.xide.ui.utils.XideStyle;

public class MenuBar extends JMenuBar {
    public MenuBar(JFrame frame) {
        XideStyle style = XideStyle.getCurrent();

        setFont(style.uiFont());
        frame.setJMenuBar(this);
    }

    public void addMenu(String label, MenuItem[] items) {
        add(new JMenu(label) {{
            for (MenuItem item : items) {
                add(item.component());
            }
        }});
    }
}
