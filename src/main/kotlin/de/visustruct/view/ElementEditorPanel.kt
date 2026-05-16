package de.visustruct.view

import de.visustruct.control.Struktogramm
import de.visustruct.i18n.I18n
import de.visustruct.i18n.StructureElementI18n
import de.visustruct.struktogrammelemente.Anweisung
import de.visustruct.struktogrammelemente.Aufruf
import de.visustruct.struktogrammelemente.Aussprung
import de.visustruct.struktogrammelemente.DoUntilSchleife
import de.visustruct.struktogrammelemente.Endlosschleife
import de.visustruct.struktogrammelemente.Fallauswahl
import de.visustruct.struktogrammelemente.ForSchleife
import de.visustruct.struktogrammelemente.LeerElement
import de.visustruct.struktogrammelemente.StruktogrammElement
import de.visustruct.struktogrammelemente.Verzweigung
import de.visustruct.struktogrammelemente.WhileSchleife
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridLayout
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.text.DefaultEditorKit
import kotlin.math.min

/** Unterer Editorbereich fuer das aktuell ausgewaehlte Struktogramm-Element. */
class ElementEditorPanel : JPanel(BorderLayout(8, 8)) {

    private companion object {
        private const val serialVersionUID = 202604261L
        private val PLACEHOLDER_TEXTE: Set<String> = setOf(
            "Statement", "condition", "selector", "i = 0; i < n; i++",
            "\u221e", "break", "method()", "Anweisung", "Verzweigung", "Fallauswahl",
            "For Schleife", "While Schleife", "Do-While Schleife", "Endlosschleife",
            "Aussprung", "Aufruf",
        )
    }

    private val titleLabel = JLabel()
    private val modePanel = JPanel()
    private val statementButton = JRadioButton()
    private val inputButton = JRadioButton()
    private val outputButton = JRadioButton()
    private val hintLabel = JLabel(" ")
    private val editorCardLayout = CardLayout()
    private val editorCardPanel = JPanel(editorCardLayout)
    private val textArea = PlaceholderTextArea(2, 40)
    private val forStartLabel = JLabel()
    private val forConditionLabel = JLabel()
    private val forIncrementLabel = JLabel()
    private val forStartField = PlaceholderTextField()
    private val forConditionField = PlaceholderTextField()
    private val forIncrementField = PlaceholderTextField()
    private val applyButton = JButton()
    private val addCaseButton = JButton()
    private val removeCaseButton = JButton()

    private var struktogramm: Struktogramm? = null
    private var element: StruktogrammElement? = null
    private var statementMode = StatementMode.STATEMENT
    private var updating = false

    init {
        minimumSize = Dimension(0, 190)
        preferredSize = Dimension(0, 220)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor()),
            BorderFactory.createEmptyBorder(10, 12, 10, 12),
        )

        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD)
        hintLabel.foreground = UIManager.getColor("Label.disabledForeground")

        val group = ButtonGroup()
        group.add(statementButton)
        group.add(inputButton)
        group.add(outputButton)
        modePanel.add(statementButton)
        modePanel.add(inputButton)
        modePanel.add(outputButton)
        statementButton.addActionListener { setStatementModeFromUi(StatementMode.STATEMENT) }
        inputButton.addActionListener { setStatementModeFromUi(StatementMode.INPUT) }
        outputButton.addActionListener { setStatementModeFromUi(StatementMode.OUTPUT) }

        val headerPanel = JPanel(BorderLayout(8, 4))
        headerPanel.isOpaque = false
        headerPanel.add(titleLabel, BorderLayout.NORTH)
        headerPanel.add(modePanel, BorderLayout.CENTER)
        headerPanel.add(hintLabel, BorderLayout.SOUTH)
        add(headerPanel, BorderLayout.NORTH)

        textArea.font = Font.decode(Font.MONOSPACED + "-PLAIN-15")
        textArea.lineWrap = false
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "apply-text")
        textArea.actionMap.put(
            "apply-text",
            object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    applyText()
                }
            },
        )
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("shift ENTER"),
            DefaultEditorKit.insertBreakAction,
        )
        editorCardPanel.add(JScrollPane(textArea), "text")
        editorCardPanel.add(forLoopEditorPanel(), "for")
        add(editorCardPanel, BorderLayout.CENTER)

        applyButton.addActionListener { applyText() }
        addCaseButton.addActionListener { addCaseToSwitch() }
        removeCaseButton.addActionListener { removeCaseFromSwitch() }
        val actions = JPanel(BorderLayout())
        actions.isOpaque = false
        val caseActions = JPanel(GridLayout(1, 2, 8, 0))
        caseActions.isOpaque = false
        caseActions.add(addCaseButton)
        caseActions.add(removeCaseButton)
        actions.add(applyButton, BorderLayout.WEST)
        actions.add(caseActions, BorderLayout.CENTER)
        add(actions, BorderLayout.SOUTH)

        refreshLocalizedTexts()
        setSelectedElement(null, null)
    }

    /** Nach Sprachwechsel (Menü „Einstellungen → Sprachen“). */
    fun refreshLocalizedTexts() {
        statementButton.text = I18n.tr("editor.mode.statement")
        inputButton.text = I18n.tr("editor.mode.input")
        outputButton.text = I18n.tr("editor.mode.output")
        applyButton.text = I18n.tr("editor.apply")
        addCaseButton.text = I18n.tr("editor.addCase")
        removeCaseButton.text = I18n.tr("editor.removeCase")
        forStartLabel.text = I18n.tr("editor.for.start")
        forConditionLabel.text = I18n.tr("editor.for.condition")
        forIncrementLabel.text = I18n.tr("editor.for.increment")
        forStartField.setPlaceholder(I18n.tr("editor.for.placeholder.start"))
        forConditionField.setPlaceholder(I18n.tr("editor.for.placeholder.condition"))
        forIncrementField.setPlaceholder(I18n.tr("editor.for.placeholder.increment"))
        if (element != null) {
            setSelectedElement(struktogramm, element)
        } else {
            titleLabel.text = I18n.tr("editor.noSelection")
        }
    }

    fun setSelectedElement(struktogramm: Struktogramm?, element: StruktogrammElement?) {
        this.struktogramm = struktogramm
        this.element = element

        updating = true
        try {
            val editable = element != null
            val statement = element is Anweisung && element !is Aufruf && element !is Aussprung
            val forLoop = element is ForSchleife
            val switchElement = element is Fallauswahl && element !is Verzweigung
            var elementText = if (editable) textAusElement(element!!) else ""
            if (element is LeerElement) {
                elementText = ""
            }
            statementMode = if (statement) erkenneStatementMode(elementText) else StatementMode.STATEMENT
            val editorText = if (statement) editorTextFuerStatement(elementText, statementMode) else elementText
            val placeholder = editable && (
                istPlatzhalterText(editorText) ||
                    (element is LeerElement && editorText.trim().isEmpty())
                )
            titleLabel.text =
                if (editable) elementTitel(element!!) else I18n.tr("editor.noEditable")
            statementButton.isSelected = statementMode == StatementMode.STATEMENT
            inputButton.isSelected = statementMode == StatementMode.INPUT
            outputButton.isSelected = statementMode == StatementMode.OUTPUT
            modePanel.isVisible = statement
            hintLabel.isVisible = editable
            hintLabel.text = if (statement) hintFuerMode(statementMode) else hintFuerElement(element)
            textArea.setPlaceholder(if (placeholder) editorText else placeholderFuerElement(element, statementMode))
            textArea.text = if (editable && !placeholder) editorText else ""
            textArea.isEnabled = editable
            setForLoopFields(element, forLoop && editable)
            editorCardLayout.show(editorCardPanel, if (forLoop) "for" else "text")
            applyButton.isEnabled = editable
            addCaseButton.isVisible = switchElement
            addCaseButton.isEnabled = switchElement
            removeCaseButton.isVisible = switchElement
            removeCaseButton.isEnabled = switchElement && element!!.gibAnzahlListen() > 2
            if (forLoop && editable) {
                SwingUtilities.invokeLater { forStartField.requestFocusInWindow() }
            } else if (editable) {
                val caret = caretPositionFuerMode(textArea.text, statementMode)
                textArea.caretPosition = caret
                SwingUtilities.invokeLater {
                    textArea.requestFocusInWindow()
                    textArea.caretPosition = caretPositionFuerMode(textArea.text, statementMode)
                }
            }
        } finally {
            updating = false
        }
    }

    private fun applyText() {
        if (updating || struktogramm == null || element == null) return

        var text = textArea.text
        if (element is ForSchleife) {
            struktogramm!!.elementTextAusEditorSetzen(
                element,
                arrayOf(
                    forStartField.text.trim(),
                    forConditionField.text.trim(),
                    forIncrementField.text.trim(),
                ),
            )
            return
        }
        if (element is Anweisung) {
            text = textMitStatementMode(text, statementMode)
        }
        struktogramm!!.elementTextAusEditorSetzen(element, text.split("\\R".toRegex(), -1).toTypedArray())
    }

    /** Wie der Button „Text übernehmen“: Editorinhalt ins markierte Element schreiben (z. B. vor Simulation). */
    fun applyPendingTextToDiagram() {
        applyText()
    }

    private fun addCaseToSwitch() {
        if (updating || struktogramm == null || element !is Fallauswahl || element is Verzweigung) return

        (element as Fallauswahl).erstelleNeueSpalte()
        struktogramm!!.zeichenbereichAktualisieren()
        struktogramm!!.zeichne()
        struktogramm!!.rueckgaengigPunktSetzen()
        setSelectedElement(struktogramm, element)
    }

    private fun removeCaseFromSwitch() {
        if (updating || struktogramm == null || element !is Fallauswahl || element is Verzweigung) return
        if (element!!.gibAnzahlListen() <= 2) return

        val fa = element as Fallauswahl
        fa.entferneSpalte(fa.gibAnzahlListen() - 2)
        struktogramm!!.zeichenbereichAktualisieren()
        struktogramm!!.zeichne()
        struktogramm!!.rueckgaengigPunktSetzen()
        setSelectedElement(struktogramm, element)
    }

    private fun forLoopEditorPanel(): JPanel {
        val panel = JPanel(GridLayout(1, 3, 8, 0))
        panel.isOpaque = false
        panel.add(labeledField(forStartLabel, forStartField))
        panel.add(labeledField(forConditionLabel, forConditionField))
        panel.add(labeledField(forIncrementLabel, forIncrementField))
        return panel
    }

    private fun labeledField(fieldLabel: JLabel, field: PlaceholderTextField): JPanel {
        val panel = JPanel(BorderLayout(0, 4))
        panel.isOpaque = false
        field.font = Font.decode(Font.MONOSPACED + "-PLAIN-15")
        field.addActionListener { applyText() }
        panel.add(fieldLabel, BorderLayout.NORTH)
        panel.add(field, BorderLayout.CENTER)
        return panel
    }

    private fun setForLoopFields(element: StruktogrammElement?, enabled: Boolean) {
        val teile = if (enabled) forLoopTeile(element!!.gibText()) else arrayOf("", "", "")
        forStartField.text = teile[0]
        forConditionField.text = teile[1]
        forIncrementField.text = teile[2]
        forStartField.isEnabled = enabled
        forConditionField.isEnabled = enabled
        forIncrementField.isEnabled = enabled
    }

    private fun forLoopTeile(text: Array<String>): Array<String> {
        val teile = arrayOf("", "", "")
        if (text.isEmpty()) return teile
        if (text.size >= 3) {
            teile[0] = text[0]
            teile[1] = text[1]
            teile[2] = text[2]
            return teile
        }
        val einzeilig = text[0]?.trim() ?: ""
        if (PLACEHOLDER_TEXTE.contains(einzeilig)) return teile
        val gesplittet = einzeilig.split("\\s*;\\s*".toRegex(), -1)
        for (i in 0 until min(3, gesplittet.size)) {
            teile[i] = gesplittet[i]
        }
        return teile
    }

    private fun setStatementModeFromUi(mode: StatementMode) {
        if (updating) return

        statementMode = mode
        val content = stripIOPrefixes(textArea.text)
        textArea.text = textMitStatementMode(content, statementMode)
        hintLabel.text = hintFuerMode(statementMode)
        textArea.setPlaceholder(placeholderFuerMode(statementMode, true))
        SwingUtilities.invokeLater {
            textArea.requestFocusInWindow()
            textArea.caretPosition = caretPositionFuerMode(textArea.text, statementMode)
        }
    }

    private fun istPlatzhalterText(text: String): Boolean =
        PLACEHOLDER_TEXTE.contains(text.trim())

    private fun erkenneStatementMode(text: String): StatementMode {
        val lower = text.trim().lowercase()
        if (lower.startsWith("input:")) return StatementMode.INPUT
        if (lower.startsWith("output:") || lower.startsWith("print:")) return StatementMode.OUTPUT
        return StatementMode.STATEMENT
    }

    private fun stripIOPrefixes(text: String): String {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()
        if (lower.startsWith("input:")) return trimmed.substring("input:".length).trim()
        if (lower.startsWith("output:")) return trimmed.substring("output:".length).trim()
        if (lower.startsWith("print:")) return trimmed.substring("print:".length).trim()
        return text
    }

    private fun editorTextFuerStatement(text: String, mode: StatementMode): String =
        if (mode == StatementMode.INPUT || mode == StatementMode.OUTPUT) {
            textMitStatementMode(stripIOPrefixes(text), mode)
        } else {
            text
        }

    private fun textMitStatementMode(text: String, mode: StatementMode): String {
        val trimmed = text.trim()
        if (mode == StatementMode.INPUT) {
            if (trimmed.lowercase().startsWith("input:")) return text
            return prefix("input:", trimmed)
        }
        if (mode == StatementMode.OUTPUT) {
            val lower = trimmed.lowercase()
            if (lower.startsWith("output:") || lower.startsWith("print:")) return text
            return prefix("output:", trimmed)
        }
        return stripIOPrefixes(text)
    }

    private fun prefix(prefix: String, text: String): String =
        if (text.isEmpty()) "$prefix " else "$prefix $text"

    private fun caretPositionFuerMode(text: String?, mode: StatementMode): Int {
        if (text.isNullOrEmpty()) return 0
        if (mode == StatementMode.INPUT && text.trim().equals("input:", ignoreCase = true)) {
            return text.length
        }
        if (mode == StatementMode.OUTPUT && text.trim().equals("output:", ignoreCase = true)) {
            return text.length
        }
        return text.length
    }

    private fun hintFuerMode(mode: StatementMode): String =
        when (mode) {
            StatementMode.INPUT -> I18n.tr("editor.hint.input")
            StatementMode.OUTPUT -> I18n.tr("editor.hint.output")
            StatementMode.STATEMENT -> I18n.tr("editor.hint.statement")
        }

    private fun placeholderFuerMode(mode: StatementMode, statement: Boolean): String {
        if (!statement) return ""
        return when (mode) {
            StatementMode.INPUT -> I18n.tr("editor.placeholder.input")
            StatementMode.OUTPUT -> I18n.tr("editor.placeholder.output")
            StatementMode.STATEMENT -> I18n.tr("editor.placeholder.statement")
        }
    }

    private fun placeholderFuerElement(element: StruktogrammElement?, mode: StatementMode): String {
        if (element is Anweisung && element !is Aufruf && element !is Aussprung) {
            return placeholderFuerMode(mode, true)
        }
        if (element is ForSchleife) return ""
        if (element is Verzweigung || element is WhileSchleife ||
            element is DoUntilSchleife || element is Endlosschleife
        ) {
            return I18n.tr("editor.placeholder.condition")
        }
        if (element is Fallauswahl) return I18n.tr("editor.placeholder.selector")
        if (element is Aufruf) return I18n.tr("editor.placeholder.call")
        if (element is Aussprung) return I18n.tr("editor.placeholder.breakComment")
        return ""
    }

    private fun hintFuerElement(element: StruktogrammElement?): String {
        if (element is Verzweigung) return I18n.tr("editor.hint.decision")
        if (element is ForSchleife) return I18n.tr("editor.hint.forLoop")
        if (element is WhileSchleife) return I18n.tr("editor.hint.whileLoop")
        if (element is DoUntilSchleife) return I18n.tr("editor.hint.doWhileLoop")
        if (element is Endlosschleife) return I18n.tr("editor.hint.infiniteLoop")
        if (element is Fallauswahl) return I18n.tr("editor.hint.switch")
        if (element is Aufruf) return I18n.tr("editor.hint.call")
        if (element is Aussprung) return I18n.tr("editor.hint.breakExit")
        return " "
    }

    private fun textAusElement(element: StruktogrammElement): String =
        element.gibText().joinToString("\n")

    private fun elementTitel(element: StruktogrammElement): String {
        val typ = when {
            element is Anweisung && element !is Aufruf && element !is Aussprung -> Struktogramm.typAnweisung
            element is Verzweigung -> Struktogramm.typVerzweigung
            element is Fallauswahl -> Struktogramm.typFallauswahl
            element is ForSchleife -> Struktogramm.typForSchleife
            element is WhileSchleife -> Struktogramm.typWhileSchleife
            element is DoUntilSchleife -> Struktogramm.typDoUntilSchleife
            element is Endlosschleife -> Struktogramm.typEndlosschleife
            element is Aussprung -> Struktogramm.typAussprung
            element is Aufruf -> Struktogramm.typAufruf
            else -> -1
        }
        if (typ >= 0) {
            val label = StructureElementI18n.paletteShortLabel(typ)
            if (label.isNotEmpty()) {
                return label
            }
        }
        return I18n.tr("editor.title.generic")
    }

    private fun borderColor(): Color {
        var c: Color? = UIManager.getColor("Component.borderColor")
        if (c == null) c = UIManager.getColor("Separator.foreground")
        return c ?: Color.LIGHT_GRAY
    }

    private class PlaceholderTextArea(rows: Int, columns: Int) : JTextArea(rows, columns) {
        private companion object {
            private const val serialVersionUID = 202604262L
        }

        private var placeholder: String = ""

        fun setPlaceholder(placeholder: String) {
            this.placeholder = placeholder
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (text.isNotEmpty() || placeholder.isEmpty()) return

            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                var c: Color? = UIManager.getColor("TextField.inactiveForeground")
                if (c == null) c = Color.GRAY
                g2.color = c
                g2.font = font
                g2.drawString(placeholder, insets.left + 2, insets.top + getFontMetrics(font).ascent)
            } finally {
                g2.dispose()
            }
        }
    }

    private class PlaceholderTextField : JTextField() {
        private companion object {
            private const val serialVersionUID = 202604264L
        }

        private var placeholder: String = ""

        fun setPlaceholder(placeholder: String) {
            this.placeholder = placeholder
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (text.isNotEmpty() || placeholder.isEmpty()) return

            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                var c: Color? = UIManager.getColor("TextField.inactiveForeground")
                if (c == null) c = Color.GRAY
                g2.color = c
                g2.font = font
                g2.drawString(
                    placeholder,
                    insets.left + 2,
                    height / 2 + getFontMetrics(font).ascent / 2 - 2,
                )
            } finally {
                g2.dispose()
            }
        }
    }

    private enum class StatementMode {
        STATEMENT, INPUT, OUTPUT
    }
}
