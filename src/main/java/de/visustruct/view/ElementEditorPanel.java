package de.visustruct.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.Set;
import java.util.StringJoiner;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;

import de.visustruct.control.Struktogramm;
import de.visustruct.struktogrammelemente.Anweisung;
import de.visustruct.struktogrammelemente.Aufruf;
import de.visustruct.struktogrammelemente.Aussprung;
import de.visustruct.struktogrammelemente.DoUntilSchleife;
import de.visustruct.struktogrammelemente.Endlosschleife;
import de.visustruct.struktogrammelemente.Fallauswahl;
import de.visustruct.struktogrammelemente.ForSchleife;
import de.visustruct.struktogrammelemente.LeerElement;
import de.visustruct.struktogrammelemente.StruktogrammElement;
import de.visustruct.struktogrammelemente.Verzweigung;
import de.visustruct.struktogrammelemente.WhileSchleife;

/** Unterer Editorbereich fuer das aktuell ausgewaehlte Struktogramm-Element. */
public class ElementEditorPanel extends JPanel {

	private static final long serialVersionUID = 202604261L;
	private static final Set<String> PLACEHOLDER_TEXTE = Set.of(
			"Statement", "condition", "selector", "i = 0; i < n; i++",
			"\u221e", "break", "method()", "Anweisung", "Verzweigung", "Fallauswahl",
			"For Schleife", "While Schleife", "Do-While Schleife", "Endlosschleife",
			"Aussprung", "Aufruf");

	private final JLabel titleLabel = new JLabel("No selection");
	private final JPanel modePanel = new JPanel();
	private final JRadioButton statementButton = new JRadioButton("Statement");
	private final JRadioButton inputButton = new JRadioButton("Input");
	private final JRadioButton outputButton = new JRadioButton("Output");
	private final JLabel hintLabel = new JLabel(" ");
	private final CardLayout editorCardLayout = new CardLayout();
	private final JPanel editorCardPanel = new JPanel(editorCardLayout);
	private final PlaceholderTextArea textArea = new PlaceholderTextArea(2, 40);
	private final PlaceholderTextField forStartField = new PlaceholderTextField();
	private final PlaceholderTextField forConditionField = new PlaceholderTextField();
	private final PlaceholderTextField forIncrementField = new PlaceholderTextField();
	private final JButton applyButton = new JButton("Apply Text");
	private final JButton addCaseButton = new JButton("Add Case");
	private final JButton removeCaseButton = new JButton("Remove Case");

	private Struktogramm struktogramm;
	private StruktogrammElement element;
	private StatementMode statementMode = StatementMode.STATEMENT;
	private boolean updating;

	public ElementEditorPanel() {
		super(new BorderLayout(8, 8));
		setMinimumSize(new Dimension(0, 190));
		setPreferredSize(new Dimension(0, 220));
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor()),
				BorderFactory.createEmptyBorder(10, 12, 10, 12)));

		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

		ButtonGroup group = new ButtonGroup();
		group.add(statementButton);
		group.add(inputButton);
		group.add(outputButton);
		modePanel.add(statementButton);
		modePanel.add(inputButton);
		modePanel.add(outputButton);
		statementButton.addActionListener(e -> setStatementModeFromUi(StatementMode.STATEMENT));
		inputButton.addActionListener(e -> setStatementModeFromUi(StatementMode.INPUT));
		outputButton.addActionListener(e -> setStatementModeFromUi(StatementMode.OUTPUT));

		JPanel headerPanel = new JPanel(new BorderLayout(8, 4));
		headerPanel.setOpaque(false);
		headerPanel.add(titleLabel, BorderLayout.NORTH);
		headerPanel.add(modePanel, BorderLayout.CENTER);
		headerPanel.add(hintLabel, BorderLayout.SOUTH);
		add(headerPanel, BorderLayout.NORTH);

		textArea.setFont(Font.decode(Font.MONOSPACED + "-PLAIN-15"));
		textArea.setLineWrap(false);
		textArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "apply-text");
		textArea.getActionMap().put("apply-text", new AbstractAction() {
			private static final long serialVersionUID = 202604263L;

			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				applyText();
			}
		});
		textArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("shift ENTER"),
				DefaultEditorKit.insertBreakAction);
		editorCardPanel.add(new JScrollPane(textArea), "text");
		editorCardPanel.add(forLoopEditorPanel(), "for");
		add(editorCardPanel, BorderLayout.CENTER);

		applyButton.addActionListener(e -> applyText());
		addCaseButton.addActionListener(e -> addCaseToSwitch());
		removeCaseButton.addActionListener(e -> removeCaseFromSwitch());
		JPanel actions = new JPanel(new BorderLayout());
		actions.setOpaque(false);
		JPanel caseActions = new JPanel(new GridLayout(1, 2, 8, 0));
		caseActions.setOpaque(false);
		caseActions.add(addCaseButton);
		caseActions.add(removeCaseButton);
		actions.add(applyButton, BorderLayout.WEST);
		actions.add(caseActions, BorderLayout.CENTER);
		add(actions, BorderLayout.SOUTH);

		setSelectedElement(null, null);
	}

	public void setSelectedElement(Struktogramm struktogramm, StruktogrammElement element) {
		this.struktogramm = struktogramm;
		this.element = element;

		updating = true;
		try {
			boolean editable = element != null;
			boolean statement = element instanceof Anweisung && !(element instanceof Aufruf)
					&& !(element instanceof Aussprung);
			boolean forLoop = element instanceof ForSchleife;
			boolean switchElement = element instanceof Fallauswahl && !(element instanceof Verzweigung);
			String elementText = editable ? textAusElement(element) : "";
			if (element instanceof LeerElement) {
				elementText = "";
			}
			statementMode = statement ? erkenneStatementMode(elementText) : StatementMode.STATEMENT;
			String editorText = statement ? editorTextFuerStatement(elementText, statementMode) : elementText;
			boolean placeholder = editable && (istPlatzhalterText(editorText)
					|| (element instanceof LeerElement && editorText.trim().isEmpty()));
			titleLabel.setText(editable ? elementTitel(element) : "No editable element selected");
			statementButton.setSelected(statementMode == StatementMode.STATEMENT);
			inputButton.setSelected(statementMode == StatementMode.INPUT);
			outputButton.setSelected(statementMode == StatementMode.OUTPUT);
			modePanel.setVisible(statement);
			hintLabel.setVisible(editable);
			hintLabel.setText(statement ? hintFuerMode(statementMode) : hintFuerElement(element));
			textArea.setPlaceholder(placeholder ? editorText : placeholderFuerElement(element, statementMode));
			textArea.setText(editable && !placeholder ? editorText : "");
			textArea.setEnabled(editable);
			setForLoopFields(element, forLoop && editable);
			editorCardLayout.show(editorCardPanel, forLoop ? "for" : "text");
			applyButton.setEnabled(editable);
			addCaseButton.setVisible(switchElement);
			addCaseButton.setEnabled(switchElement);
			removeCaseButton.setVisible(switchElement);
			removeCaseButton.setEnabled(switchElement && element.gibAnzahlListen() > 2);
			if (forLoop && editable) {
				SwingUtilities.invokeLater(() -> forStartField.requestFocusInWindow());
			} else if (editable) {
				int caret = caretPositionFuerMode(textArea.getText(), statementMode);
				textArea.setCaretPosition(caret);
				SwingUtilities.invokeLater(() -> {
					textArea.requestFocusInWindow();
					textArea.setCaretPosition(caretPositionFuerMode(textArea.getText(), statementMode));
				});
			}
		} finally {
			updating = false;
		}
	}

	private void applyText() {
		if (updating || struktogramm == null || element == null) {
			return;
		}

		String text = textArea.getText();
		if (element instanceof ForSchleife) {
			struktogramm.elementTextAusEditorSetzen(element, new String[] {
					forStartField.getText().trim(),
					forConditionField.getText().trim(),
					forIncrementField.getText().trim()
			});
			return;
		}
		if (element instanceof Anweisung) {
			text = textMitStatementMode(text, statementMode);
		}
		struktogramm.elementTextAusEditorSetzen(element, text.split("\\R", -1));
	}

	private void addCaseToSwitch() {
		if (updating || struktogramm == null || !(element instanceof Fallauswahl) || element instanceof Verzweigung) {
			return;
		}

		((Fallauswahl) element).erstelleNeueSpalte();
		struktogramm.zeichenbereichAktualisieren();
		struktogramm.zeichne();
		struktogramm.rueckgaengigPunktSetzen();
		setSelectedElement(struktogramm, element);
	}

	private void removeCaseFromSwitch() {
		if (updating || struktogramm == null || !(element instanceof Fallauswahl) || element instanceof Verzweigung) {
			return;
		}
		if (element.gibAnzahlListen() <= 2) {
			return;
		}

		((Fallauswahl) element).entferneSpalte(element.gibAnzahlListen() - 2);
		struktogramm.zeichenbereichAktualisieren();
		struktogramm.zeichne();
		struktogramm.rueckgaengigPunktSetzen();
		setSelectedElement(struktogramm, element);
	}

	private JPanel forLoopEditorPanel() {
		JPanel panel = new JPanel(new GridLayout(1, 3, 8, 0));
		panel.setOpaque(false);
		panel.add(labeledField("Start", forStartField, "int i = 0"));
		panel.add(labeledField("Condition", forConditionField, "i < n"));
		panel.add(labeledField("Increment", forIncrementField, "i++"));
		return panel;
	}

	private JPanel labeledField(String label, PlaceholderTextField field, String placeholder) {
		JPanel panel = new JPanel(new BorderLayout(0, 4));
		panel.setOpaque(false);
		JLabel fieldLabel = new JLabel(label);
		field.setFont(Font.decode(Font.MONOSPACED + "-PLAIN-15"));
		field.setPlaceholder(placeholder);
		field.addActionListener(e -> applyText());
		panel.add(fieldLabel, BorderLayout.NORTH);
		panel.add(field, BorderLayout.CENTER);
		return panel;
	}

	private void setForLoopFields(StruktogrammElement element, boolean enabled) {
		String[] teile = enabled ? forLoopTeile(element.gibText()) : new String[] { "", "", "" };
		forStartField.setText(teile[0]);
		forConditionField.setText(teile[1]);
		forIncrementField.setText(teile[2]);
		forStartField.setEnabled(enabled);
		forConditionField.setEnabled(enabled);
		forIncrementField.setEnabled(enabled);
	}

	private static String[] forLoopTeile(String[] text) {
		String[] teile = new String[] { "", "", "" };
		if (text == null || text.length == 0) {
			return teile;
		}
		if (text.length >= 3) {
			teile[0] = text[0];
			teile[1] = text[1];
			teile[2] = text[2];
			return teile;
		}
		String einzeilig = text[0] != null ? text[0].trim() : "";
		if (PLACEHOLDER_TEXTE.contains(einzeilig)) {
			return teile;
		}
		String[] gesplittet = einzeilig.split("\\s*;\\s*", -1);
		for (int i = 0; i < Math.min(3, gesplittet.length); i++) {
			teile[i] = gesplittet[i];
		}
		return teile;
	}

	private void setStatementModeFromUi(StatementMode mode) {
		if (updating) {
			return;
		}

		statementMode = mode;
		String content = stripIOPrefixes(textArea.getText());
		textArea.setText(textMitStatementMode(content, statementMode));
		hintLabel.setText(hintFuerMode(statementMode));
		textArea.setPlaceholder(placeholderFuerMode(statementMode, true));
		SwingUtilities.invokeLater(() -> {
			textArea.requestFocusInWindow();
			textArea.setCaretPosition(caretPositionFuerMode(textArea.getText(), statementMode));
		});
	}

	private static boolean istPlatzhalterText(String text) {
		return PLACEHOLDER_TEXTE.contains(text.trim());
	}

	private static StatementMode erkenneStatementMode(String text) {
		String lower = text.trim().toLowerCase();
		if (lower.startsWith("input:")) {
			return StatementMode.INPUT;
		}
		if (lower.startsWith("output:") || lower.startsWith("print:")) {
			return StatementMode.OUTPUT;
		}
		return StatementMode.STATEMENT;
	}

	private static String stripIOPrefixes(String text) {
		String trimmed = text.trim();
		String lower = trimmed.toLowerCase();
		if (lower.startsWith("input:")) {
			return trimmed.substring("input:".length()).trim();
		}
		if (lower.startsWith("output:")) {
			return trimmed.substring("output:".length()).trim();
		}
		if (lower.startsWith("print:")) {
			return trimmed.substring("print:".length()).trim();
		}
		return text;
	}

	private static String editorTextFuerStatement(String text, StatementMode mode) {
		if (mode == StatementMode.INPUT || mode == StatementMode.OUTPUT) {
			return textMitStatementMode(stripIOPrefixes(text), mode);
		}
		return text;
	}

	private static String textMitStatementMode(String text, StatementMode mode) {
		String trimmed = text.trim();
		if (mode == StatementMode.INPUT) {
			if (trimmed.toLowerCase().startsWith("input:")) {
				return text;
			}
			return prefix("input:", trimmed);
		}
		if (mode == StatementMode.OUTPUT) {
			String lower = trimmed.toLowerCase();
			if (lower.startsWith("output:") || lower.startsWith("print:")) {
				return text;
			}
			return prefix("output:", trimmed);
		}
		return stripIOPrefixes(text);
	}

	private static String prefix(String prefix, String text) {
		return text.isEmpty() ? prefix + " " : prefix + " " + text;
	}

	private static int caretPositionFuerMode(String text, StatementMode mode) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		if (mode == StatementMode.INPUT && text.trim().equalsIgnoreCase("input:")) {
			return text.length();
		}
		if (mode == StatementMode.OUTPUT && text.trim().equalsIgnoreCase("output:")) {
			return text.length();
		}
		return text.length();
	}

	private static String hintFuerMode(StatementMode mode) {
		switch (mode) {
		case INPUT:
			return "input: int number \"insert number\"  |  input: String name \"insert name\"";
		case OUTPUT:
			return "output: number  or  output: \"Your text\"";
		case STATEMENT:
		default:
			return "int number = 5  |  double x = 1.5  |  String text = \"Your text\"";
		}
	}

	private static String placeholderFuerMode(StatementMode mode, boolean statement) {
		if (!statement) {
			return "";
		}
		switch (mode) {
		case INPUT:
			return "int number";
		case OUTPUT:
			return "\"Your text\"";
		case STATEMENT:
		default:
			return "Statement";
		}
	}

	private static String placeholderFuerElement(StruktogrammElement element, StatementMode mode) {
		if (element instanceof Anweisung && !(element instanceof Aufruf) && !(element instanceof Aussprung)) {
			return placeholderFuerMode(mode, true);
		}
		if (element instanceof ForSchleife) {
			return "";
		}
		if (element instanceof Verzweigung || element instanceof WhileSchleife
				|| element instanceof DoUntilSchleife || element instanceof Endlosschleife) {
			return "Insert condition\u2026";
		}
		if (element instanceof Fallauswahl) {
			return "Insert selector\u2026";
		}
		if (element instanceof Aufruf) {
			return "Insert call\u2026";
		}
		if (element instanceof Aussprung) {
			return "Comment (optional)";
		}
		return "";
	}

	private static String hintFuerElement(StruktogrammElement element) {
		if (element instanceof Verzweigung) {
			return "Example: age >= 18";
		}
		if (element instanceof ForSchleife) {
			return "Example: int i = 0; i < n; i++";
		}
		if (element instanceof WhileSchleife) {
			return "Example: i < 10";
		}
		if (element instanceof DoUntilSchleife) {
			return "Example: !input.equals(pin)";
		}
		if (element instanceof Endlosschleife) {
			return "Optional condition/comment, e.g. true";
		}
		if (element instanceof Fallauswahl) {
			return "Example: day";
		}
		if (element instanceof Aufruf) {
			return "Example: method()";
		}
		if (element instanceof Aussprung) {
			return "Example: break";
		}
		return " ";
	}

	private static String textAusElement(StruktogrammElement element) {
		StringJoiner joiner = new StringJoiner("\n");
		for (String line : element.gibText()) {
			joiner.add(line);
		}
		return joiner.toString();
	}

	private static String elementTitel(StruktogrammElement element) {
		if (element instanceof Anweisung && !(element instanceof Aufruf) && !(element instanceof Aussprung)) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typAnweisung);
		}
		if (element instanceof Verzweigung) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typVerzweigung);
		}
		if (element instanceof Fallauswahl) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typFallauswahl);
		}
		if (element instanceof ForSchleife) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typForSchleife);
		}
		if (element instanceof WhileSchleife) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typWhileSchleife);
		}
		if (element instanceof DoUntilSchleife) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typDoUntilSchleife);
		}
		if (element instanceof Endlosschleife) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typEndlosschleife);
		}
		if (element instanceof Aussprung) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typAussprung);
		}
		if (element instanceof Aufruf) {
			return StruktogrammPalette.getPaletteButtonKurzEnglish(Struktogramm.typAufruf);
		}
		return "Element";
	}

	private static Color borderColor() {
		Color c = UIManager.getColor("Component.borderColor");
		if (c == null) {
			c = UIManager.getColor("Separator.foreground");
		}
		return c != null ? c : Color.LIGHT_GRAY;
	}

	private static class PlaceholderTextArea extends JTextArea {
		private static final long serialVersionUID = 202604262L;
		private String placeholder = "";

		PlaceholderTextArea(int rows, int columns) {
			super(rows, columns);
		}

		void setPlaceholder(String placeholder) {
			this.placeholder = placeholder != null ? placeholder : "";
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (!getText().isEmpty() || placeholder.isEmpty()) {
				return;
			}

			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				Color c = UIManager.getColor("TextField.inactiveForeground");
				g2.setColor(c != null ? c : Color.GRAY);
				g2.setFont(getFont());
				g2.drawString(placeholder, getInsets().left + 2, getInsets().top + getFontMetrics(getFont()).getAscent());
			} finally {
				g2.dispose();
			}
		}
	}

	private static class PlaceholderTextField extends JTextField {
		private static final long serialVersionUID = 202604264L;
		private String placeholder = "";

		void setPlaceholder(String placeholder) {
			this.placeholder = placeholder != null ? placeholder : "";
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (!getText().isEmpty() || placeholder.isEmpty()) {
				return;
			}

			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				Color c = UIManager.getColor("TextField.inactiveForeground");
				g2.setColor(c != null ? c : Color.GRAY);
				g2.setFont(getFont());
				g2.drawString(placeholder, getInsets().left + 2, getHeight() / 2 + getFontMetrics(getFont()).getAscent() / 2 - 2);
			} finally {
				g2.dispose();
			}
		}
	}

	private enum StatementMode {
		STATEMENT, INPUT, OUTPUT
	}
}
