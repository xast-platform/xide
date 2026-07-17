package org.xast.xide.ui.utils;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.*;

import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.event.ThemeChangedEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ThemeSwitcher extends JPanel {

    public ThemeSwitcher(EventBus eventBus) {
        List<UIManager.LookAndFeelInfo> themes = new ArrayList<>();
        themes.add(new UIManager.LookAndFeelInfo("Flat Light", FlatLightLaf.class.getName()));
        themes.add(new UIManager.LookAndFeelInfo("Flat Dark", FlatDarkLaf.class.getName()));
        themes.add(new UIManager.LookAndFeelInfo("Flat IntelliJ", FlatIntelliJLaf.class.getName()));
        themes.add(new UIManager.LookAndFeelInfo("Flat Darcula", FlatDarculaLaf.class.getName()));
        themes.add(new UIManager.LookAndFeelInfo("Flat macOS Light", FlatMacLightLaf.class.getName()));
        themes.add(new UIManager.LookAndFeelInfo("Flat macOS Dark", FlatMacDarkLaf.class.getName()));
        themes.addAll(Arrays.asList(FlatAllIJThemes.INFOS));

        themes.sort(Comparator.comparing(UIManager.LookAndFeelInfo::getName));

        JComboBox<UIManager.LookAndFeelInfo> combo =
            new JComboBox<>(themes.toArray(new UIManager.LookAndFeelInfo[0]));

        combo.addActionListener(e -> {
            eventBus.publish(new ThemeChangedEvent());
        });

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UIManager.LookAndFeelInfo info) {
                    setText(info.getName());
                }
                return this;
            }
        });

        String currentClassName = UIManager.getLookAndFeel().getClass().getName();
        for (UIManager.LookAndFeelInfo info : themes) {
            if (info.getClassName().equals(currentClassName)) {
                combo.setSelectedItem(info);
                break;
            }
        }

        combo.addActionListener(e -> {
            UIManager.LookAndFeelInfo selected = (UIManager.LookAndFeelInfo) combo.getSelectedItem();
            if (selected == null) return;

            try {
                UIManager.setLookAndFeel(selected.getClassName());
                FlatLaf.updateUI(); // repaints all open windows, no restart needed
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to apply theme: " + ex.getMessage());
            }
        });

        add(new JLabel("Theme:"));
        add(combo);
    }
}