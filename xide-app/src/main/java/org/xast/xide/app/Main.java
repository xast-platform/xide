package org.xast.xide.app;

import javax.swing.SwingUtilities;

import org.xast.xide.core.PluginManager;
import org.xast.xide.core.Workspace;
import org.xast.xide.ui.MainFrame;
import org.xast.xide.ui.utils.XideStyle;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            XideStyle.setCurrent(XideStyle.defaultStyle());
            
            PluginManager pm = new PluginManager();
            pm.loadPlugins();
            
            MainFrame frame = new MainFrame(
                Workspace.init(args),
                pm.getRegistry()
            );
            
            frame.setTitle("Xide");
            frame.show();
        });
    }
}
