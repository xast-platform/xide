package org.xast.xide.app;

import javax.swing.SwingUtilities;

import org.xast.xide.core.PluginManager;
import org.xast.xide.core.Workspace;
import org.xast.xide.core.config.XideConfig;
import org.xast.xide.core.event.EventBus;
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

            EventBus eventBus = new EventBus();
            XideConfig config = XideConfig.load(eventBus);
            MainFrame frame = new MainFrame(
                workspace, 
                pm.getRegistry(),
                eventBus,
                config
            );
            
            frame.loadPlugins();
            frame.setTitle("Xide");
            frame.show();
        });
    }
}
