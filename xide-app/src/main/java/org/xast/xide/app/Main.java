package org.xast.xide.app;

import org.xast.xide.core.PluginManager;
import org.xast.xide.ui.MainFrame;
import org.xast.xide.ui.utils.XideStyle;

public class Main {
    public static void main(String[] args) {
        XideStyle style = XideStyle.defaultStyle();
        MainFrame frame = new MainFrame(style);

        PluginManager pm = new PluginManager();
        pm.loadPlugins();
        
        frame.initializePlugins(pm.getRegistry());
        frame.show();
    }
}
