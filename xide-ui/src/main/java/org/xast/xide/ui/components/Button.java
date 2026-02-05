package org.xast.xide.ui.components;

import java.awt.Color;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;

import lombok.NonNull;

public class Button {
    private JButton button;

    public Button(String label, Action action) {
        button = new JButton(label);
        button.setForeground(Color.LIGHT_GRAY);
        button.setAction(action);
    }

    public @NonNull JComponent getComponent() {
        return button;
    }
}
