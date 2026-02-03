package org.xast.xide;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialLighterIJTheme;

import java.awt.Color;

import javax.swing.*;

// FlatOneDarkIJTheme
// FlatMonokaiProIJTheme

public class Main {
    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        FlatMTMaterialDarkerIJTheme.setup();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Xide");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            JMenuBar menuBar = new JMenuBar();
            menuBar.add(new JMenu("File"));
            menuBar.add(new JMenu("Edit"));
            menuBar.add(new JMenu("View"));
            menuBar.add(new JMenu("Font"));
            menuBar.add(new JMenu("Options"));
            menuBar.add(new JMenu("Help"));
            frame.setJMenuBar(menuBar);
            frame.setSize(1280, 960);
            frame.setLocationRelativeTo(null);

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            JButton button = new JButton("Click me");
            button.addActionListener(e -> JOptionPane.showMessageDialog(frame, "Button clicked!"));
            button.setForeground(Color.LIGHT_GRAY);
            mainPanel.add(button);

            JTextField textField = new JTextField("Enter text here");
            mainPanel.add(textField);

            JCheckBox checkBox = new JCheckBox("Accept terms");
            mainPanel.add(checkBox);

            JComboBox<String> comboBox = new JComboBox<>(new String[]{"Option 1", "Option 2", "Option 3"});
            mainPanel.add(comboBox);

            JTextArea textArea = new JTextArea("Multi-line text area", 5, 20);
            mainPanel.add(new JScrollPane(textArea));

            frame.setContentPane(mainPanel);
            frame.setVisible(true);
        });
    }
}