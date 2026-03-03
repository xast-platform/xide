package org.xast.xide.app;

import javax.swing.SwingUtilities;

import org.xast.xide.core.PluginManager;
import org.xast.xide.core.Workspace;
import org.xast.xide.core.utils.Debug;
import org.xast.xide.ui.MainFrame;
import org.xast.xide.ui.utils.XideStyle;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            XideStyle.setCurrent(XideStyle.defaultStyle());
            
            PluginManager pm = new PluginManager();
            pm.loadPlugins();

            Workspace workspace = Workspace.init(args);
            if (workspace.hasMultipleDirs()) {
                Debug.error("Workspace cannot contains multiple folders");
                System.exit(-1);
            }
            
            MainFrame frame = new MainFrame(workspace, pm.getRegistry());
            
            frame.setTitle("Xide");
            frame.show();
        });
    }
}
