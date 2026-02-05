package org.xast.xide.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.xast.xide.core.PluginRegistry;
import org.xast.xide.ui.utils.XideStyle;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme;

public class MainFrame {
    private JFrame frame;

    public MainFrame(XideStyle style) {
        setStyle(style);

        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        FlatMTMaterialDarkerIJTheme.setup();

        frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1280, 960);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(style.favicon().getImage());
    }

    public void initializePlugins(PluginRegistry registry) {
        
    }

    public void show() {
        frame.setVisible(true);
    }

    private static void setStyle(XideStyle style) {
        UIManager.put("TitlePane.iconSize", new Dimension(
            XideStyle.ICON_WIDTH, 
            XideStyle.ICON_HEIGHT
        ));
        UIManager.put("TitlePane.titleMargins", new Insets(8,8,8,8));
        UIManager.put("TitlePane.font", style.uiFont());
        UIManager.put("TabbedPane.tabHeight", 32);
        UIManager.put("TabbedPane.tabInsets", new Insets(6, 14, 6, 14));
    }
}