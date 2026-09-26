package org.xast.xide.ui.components.code_panel.neo_editor;

import scala.swing.Label
import scala.swing.GridPanel
import org.xast.xide.ui.utils.XideStyle

class EditorStatus extends GridPanel(1, 1):
    
    private var currentLine: Int = 0;
    private var currentChar: Int = 0;
    private val displayData: Label = new Label();

    val style = XideStyle.getCurrent()
    displayData.font = style.uiFont
    contents += displayData

    updateDisplayData()

    def setCurrentLine(line: Int): Unit =
        if line > 0 then
            currentLine = line
            updateDisplayData()

    def setCurrentChar(ch: Int): Unit = 
        if ch > 0 then
            currentChar = ch
            updateDisplayData()

    def updateDisplayData(): Unit =
        if currentChar == 0 || currentLine == 0 then
            displayData.text = "Ln -, Ch -"
        else
            displayData.text = "Ln "+currentLine+", Ch "+currentChar
