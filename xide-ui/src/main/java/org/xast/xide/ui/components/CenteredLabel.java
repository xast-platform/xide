package org.xast.xide.ui.components;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.xast.xide.ui.utils.XideStyle;

public class CenteredLabel extends JLabel {
    public CenteredLabel(String text, int width, float fontSize) {
        super(
            "<html><div style='text-align: center; width: "+width+"px;'>"+text+"</div></html>"
        );

        XideStyle style = XideStyle.getCurrent();

        setFont(style.uiFont().deriveFont(fontSize));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
    }
}
