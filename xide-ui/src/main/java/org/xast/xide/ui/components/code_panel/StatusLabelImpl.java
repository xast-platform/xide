package org.xast.xide.ui.components.code_panel;

import javax.swing.JLabel;

import org.xast.xide.core.plugin.ui.StatusLabel;
import org.xast.xide.ui.utils.XideStyle;

public class StatusLabelImpl extends JLabel implements StatusLabel {
    public StatusLabelImpl() {
        XideStyle style = XideStyle.getCurrent();

        setFont(style.uiFont().deriveFont(16f));
    }

    @Override
    public void setValue(String value) {
        setText(value);
    }
}
