package org.xast.xide.ui.components.code_panel.neo_editor;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;

public class NeoEditorStatus extends JPanel {
    @Getter
    private int currentLine = 0;
    @Getter
    private int currentChar = 0;

    private JLabel displayData;

    public NeoEditorStatus() {
        setLayout(new GridLayout());

        displayData = new JLabel();
        add(displayData);

        updateDisplayData();
    }

    public void setCurrentLine(int line) {
        if (line > 0) {
            this.currentLine = line;
            updateDisplayData();
        }
    }

    public void setCurrentChar(int ch) {
        if (ch > 0) {
            this.currentChar = ch;
            updateDisplayData();
        }
    }

    public void updateDisplayData() {
        if (currentChar == 0 || currentLine == 0) {
            displayData.setText("Ln -, Ch -");
            return;
        }

        displayData.setText("Ln "+currentLine+", Ch "+currentChar);
    }
}

