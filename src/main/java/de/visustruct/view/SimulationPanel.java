package de.visustruct.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import de.visustruct.control.Controlling;
import de.visustruct.control.Struktogramm;
import de.visustruct.i18n.I18n;
import de.visustruct.simulation.SimulationEngine;
import de.visustruct.simulation.SimulationInputRequest;

/**
 * Simulations-Ansicht: Transport (Play/Pause/Step/Stop), Highlight-Modus, Variablen, Ausgabe, Eingabe.
 */
public class SimulationPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int PLAY_INTERVAL_MS = 220;
	/** Pro Timer-Tick: mehrere Simulationsschritte, damit die Wiedergabe flüssiger wirkt (ohne Eingabe). */
	private static final int PLAY_STEPS_PER_TICK = 16;

	/** Mediensymbole (an SF Symbols / Swift-Steuerung angelehnt). */
	private static final String SYM_PLAY = "\u25B6";
	private static final String SYM_PAUSE = "\u23F8";
	private static final String SYM_STEP = "\u23ED";
	private static final String SYM_STOP = "\u23F9";
	private static final String SYM_BACK = "\u2190";
	private static final String SYM_SUBMIT = "\u2713";

	private final Controlling controlling;
	private SimulationEngine engine;

	private final JTextArea variablesArea = new JTextArea(8, 40);
	private final JTextArea outputArea = new JTextArea(8, 40);
	private final JLabel messageLabel = new JLabel(" ");
	private final JLabel traceLabel = new JLabel(" ");
	private final JLabel pathLabel = new JLabel(" ");
	private final JLabel inputPromptLabel = new JLabel(" ");
	private final JTextField inputField = new JTextField(24);
	private final JButton inputSubmitButton = new JButton();
	private final JButton playButton = new JButton();
	private final JButton pauseButton = new JButton();
	private final JButton stepButton = new JButton();
	private final JButton stopButton = new JButton();
	private final JButton backButton = new JButton();

	private final JLabel highlightLabel = new JLabel();
	private final JRadioButton highlightNextRadio = new JRadioButton();
	private final JRadioButton highlightLastRadio = new JRadioButton();

	private final Timer playTimer;
	private boolean playing;
	private JPanel controlsNorthPanel;
	private JPanel inputSectionPanel;

	/** Pfad des zuletzt ausgeführten Schritts (für „Letzter Schritt“). */
	private List<Integer> lastExecutedSimulationPath;

	public SimulationPanel(Controlling controlling) {
		super(new BorderLayout(8, 8));
		this.controlling = controlling;
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		Font mono = Font.decode(Font.MONOSPACED + "-PLAIN-13");
		variablesArea.setFont(mono);
		outputArea.setFont(mono);
		variablesArea.setEditable(false);
		outputArea.setEditable(false);

		playTimer = new Timer(PLAY_INTERVAL_MS, e -> onPlayTick());
		playTimer.setRepeats(true);

		playButton.addActionListener(e -> onPlay());
		pauseButton.addActionListener(e -> onPause());
		stepButton.addActionListener(e -> onStep());
		stopButton.addActionListener(e -> onStop());
		backButton.addActionListener(e -> controlling.leaveSimulationMode());

		JPanel transport = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		transport.add(playButton);
		transport.add(pauseButton);
		transport.add(stepButton);
		transport.add(stopButton);
		transport.add(Box.createHorizontalStrut(12));
		transport.add(backButton);

		ButtonGroup highlightGroup = new ButtonGroup();
		highlightGroup.add(highlightNextRadio);
		highlightGroup.add(highlightLastRadio);
		highlightNextRadio.setSelected(true);
		var highlightListener = (java.awt.event.ItemListener) e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				applyDiagramHighlight();
			}
		};
		highlightNextRadio.addItemListener(highlightListener);
		highlightLastRadio.addItemListener(highlightListener);

		JPanel highlightRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		highlightRow.add(highlightLabel);
		highlightRow.add(highlightNextRadio);
		highlightRow.add(highlightLastRadio);

		controlsNorthPanel = new JPanel(new BorderLayout(0, 6));
		controlsNorthPanel.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.controls")));
		controlsNorthPanel.add(transport, BorderLayout.NORTH);
		controlsNorthPanel.add(highlightRow, BorderLayout.SOUTH);

		inputSectionPanel = new JPanel(new BorderLayout(0, 6));
		inputSectionPanel.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.input")));
		inputSectionPanel.add(inputPromptLabel, BorderLayout.NORTH);
		JPanel inputRow = new JPanel(new BorderLayout(8, 0));
		inputRow.add(inputField, BorderLayout.CENTER);
		inputRow.add(inputSubmitButton, BorderLayout.EAST);
		inputSectionPanel.add(inputRow, BorderLayout.CENTER);

		JPanel northStack = new JPanel(new BorderLayout(0, 10));
		northStack.add(inputSectionPanel, BorderLayout.NORTH);
		northStack.add(controlsNorthPanel, BorderLayout.CENTER);
		add(northStack, BorderLayout.NORTH);

		applyTransportLabelsAndTips();
		applyHighlightLabels();

		JScrollPane varScroll = new JScrollPane(variablesArea);
		varScroll.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.variables")));
		JScrollPane outScroll = new JScrollPane(outputArea);
		outScroll.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.output")));
		javax.swing.JSplitPane split = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, varScroll, outScroll);
		split.setResizeWeight(0.45);
		split.setDividerLocation(200);
		split.setContinuousLayout(true);
		add(split, BorderLayout.CENTER);

		inputSubmitButton.addActionListener(e -> onSubmitInput());
		inputField.addActionListener(e -> onSubmitInput());

		JPanel south = new JPanel(new BorderLayout(4, 6));
		JPanel labels = new JPanel(new BorderLayout(2, 4));
		labels.add(messageLabel, BorderLayout.NORTH);
		labels.add(traceLabel, BorderLayout.CENTER);
		labels.add(pathLabel, BorderLayout.SOUTH);
		south.add(labels, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);

		setMinimumSize(new Dimension(260, 200));
		clearEngine();
	}

	private static List<Integer> copyPath(List<Integer> path) {
		if (path == null || path.isEmpty()) {
			return null;
		}
		return new ArrayList<>(path);
	}

	private void applyTransportLabelsAndTips() {
		Font symFont = buttonSymbolFont();

		playButton.setFont(symFont);
		playButton.setText(SYM_PLAY);
		playButton.setMargin(new Insets(4, 10, 4, 10));
		playButton.setToolTipText(I18n.tr("simulation.play.tooltip"));
		playButton.getAccessibleContext().setAccessibleName(I18n.tr("simulation.play"));

		pauseButton.setFont(symFont);
		pauseButton.setText(SYM_PAUSE);
		pauseButton.setMargin(new Insets(4, 10, 4, 10));
		pauseButton.setToolTipText(I18n.tr("simulation.pause.tooltip"));
		pauseButton.getAccessibleContext().setAccessibleName(I18n.tr("simulation.pause"));

		stepButton.setFont(symFont);
		stepButton.setText(SYM_STEP);
		stepButton.setMargin(new Insets(4, 10, 4, 10));
		stepButton.setToolTipText(I18n.tr("simulation.step.tooltip"));
		stepButton.getAccessibleContext().setAccessibleName(I18n.tr("simulation.step"));

		stopButton.setFont(symFont);
		stopButton.setText(SYM_STOP);
		stopButton.setMargin(new Insets(4, 10, 4, 10));
		stopButton.setToolTipText(I18n.tr("simulation.stop.tooltip"));
		stopButton.getAccessibleContext().setAccessibleName(I18n.tr("simulation.stop"));

		backButton.setFont(symFont);
		backButton.setText(SYM_BACK);
		backButton.setMargin(new Insets(4, 10, 4, 10));
		backButton.setToolTipText(I18n.tr("simulation.back.tooltip"));
		backButton.getAccessibleContext().setAccessibleName(I18n.tr("simulation.back"));

		inputSubmitButton.setFont(symFont);
		inputSubmitButton.setText(SYM_SUBMIT);
		inputSubmitButton.setMargin(new Insets(4, 10, 4, 10));
		inputSubmitButton.setToolTipText(I18n.tr("simulation.input.submit.tooltip"));
		inputSubmitButton.getAccessibleContext().setAccessibleName(I18n.tr("simulation.input.submit"));
	}

	private static Font buttonSymbolFont() {
		Font base = UIManager.getFont("Button.font");
		if (base == null) {
			base = Font.decode(Font.SANS_SERIF + "-PLAIN-13");
		}
		return base.deriveFont(Font.PLAIN, Math.max(15f, base.getSize2D() + 3f));
	}

	private void applyHighlightLabels() {
		highlightLabel.setText(I18n.tr("simulation.highlight"));
		highlightNextRadio.setText(I18n.tr("simulation.highlight.nextStep"));
		highlightLastRadio.setText(I18n.tr("simulation.highlight.lastStep"));
		highlightNextRadio.getAccessibleContext().setAccessibleName(I18n.tr("simulation.highlight.nextStep"));
		highlightLastRadio.getAccessibleContext().setAccessibleName(I18n.tr("simulation.highlight.lastStep"));
	}

	private void clearDiagramSimulationHighlight() {
		Struktogramm str = controlling.gibAktuellesStruktogramm();
		if (str != null) {
			str.setzeSimulationSpotlightPfad(null);
		}
	}

	private void applyDiagramHighlight() {
		Struktogramm str = controlling.gibAktuellesStruktogramm();
		if (str == null) {
			return;
		}
		if (engine == null) {
			str.setzeSimulationSpotlightPfad(null);
			return;
		}
		List<Integer> p;
		if (highlightNextRadio.isSelected()) {
			p = copyPath(engine.getCurrentStepPath());
			boolean simAmEnde =
				engine.getInputRequestForUi() == null && !engine.getCanStep();
			if ((p == null || p.isEmpty()) && simAmEnde && lastExecutedSimulationPath != null) {
				// Kein „nächster“ Block mehr: letzte ausgeführte Position bis Stopp sichtbar lassen
				p = copyPath(lastExecutedSimulationPath);
			}
		} else {
			p = copyPath(lastExecutedSimulationPath);
		}
		str.setzeSimulationSpotlightPfad(p);
	}

	@Override
	public void removeNotify() {
		stopPlayInternal();
		if (playTimer != null) {
			playTimer.stop();
		}
		super.removeNotify();
	}

	public void setEngine(SimulationEngine eng) {
		stopPlayInternal();
		lastExecutedSimulationPath = null;
		this.engine = eng;
		refreshFromEngine();
	}

	public void clearEngine() {
		stopPlayInternal();
		this.engine = null;
		lastExecutedSimulationPath = null;
		clearDiagramSimulationHighlight();
		variablesArea.setText("");
		outputArea.setText("");
		messageLabel.setText(" ");
		traceLabel.setText(" ");
		pathLabel.setText(" ");
		inputPromptLabel.setText(" ");
		inputField.setText("");
		inputField.setEnabled(false);
		inputSubmitButton.setEnabled(false);
		updateTransportButtons();
	}

	public void refreshFromEngine() {
		if (engine == null) {
			clearEngine();
			return;
		}
		StringBuilder vars = new StringBuilder();
		for (var e : new java.util.TreeMap<>(engine.getVariablesSnapshot()).entrySet()) {
			vars.append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
		}
		variablesArea.setText(vars.toString());

		StringBuilder out = new StringBuilder();
		for (String line : engine.getOutputLinesSnapshot()) {
			out.append(line).append('\n');
		}
		outputArea.setText(out.toString());

		String msg = engine.getUiMessage();
		messageLabel.setText(msg != null && !msg.isEmpty() ? msg : " ");
		String tr = engine.getUiLastTrace();
		traceLabel.setText(tr != null && !tr.isEmpty() ? tr : " ");

		List<Integer> path = engine.getCurrentStepPath();
		if (path == null || path.isEmpty()) {
			pathLabel.setText(I18n.tr("simulation.path") + ": —");
		} else {
			StringBuilder pb = new StringBuilder();
			for (int i = 0; i < path.size(); i++) {
				if (i > 0) {
					pb.append(" / ");
				}
				pb.append(path.get(i));
			}
			pathLabel.setText(I18n.tr("simulation.path") + ": " + pb);
		}

		SimulationInputRequest req = engine.getInputRequestForUi();
		String err = engine.getInputErrorForUi();
		if (req != null) {
			stopPlayInternal();
			inputPromptLabel.setText(req.getPrompt());
			inputField.setEnabled(true);
			inputSubmitButton.setEnabled(true);
			if (err != null && !err.isEmpty()) {
				messageLabel.setText(err);
			}
			SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
		} else {
			inputPromptLabel.setText(" ");
			inputField.setEnabled(false);
			inputSubmitButton.setEnabled(false);
		}

		updateTransportButtons();
		applyDiagramHighlight();
	}

	private void stopPlayInternal() {
		playing = false;
		playTimer.stop();
	}

	private void updateTransportButtons() {
		boolean hasEngine = engine != null;
		boolean canStep = hasEngine && engine.getCanStep();
		playButton.setEnabled(hasEngine && !playing && canStep);
		pauseButton.setEnabled(hasEngine && playing);
		stepButton.setEnabled(hasEngine && canStep);
		stopButton.setEnabled(hasEngine);
		backButton.setEnabled(true);
	}

	private void onPlay() {
		if (engine == null || !engine.getCanStep()) {
			return;
		}
		playing = true;
		playTimer.start();
		updateTransportButtons();
	}

	private void onPause() {
		stopPlayInternal();
		updateTransportButtons();
	}

	private void onPlayTick() {
		if (engine == null || !playing) {
			return;
		}
		if (!engine.getCanStep()) {
			stopPlayInternal();
			refreshFromEngine();
			return;
		}
		int burst = 0;
		while (burst < PLAY_STEPS_PER_TICK && engine != null && playing && engine.getCanStep()) {
			List<Integer> before = copyPath(engine.getCurrentStepPath());
			engine.step();
			lastExecutedSimulationPath = before;
			burst++;
			if (engine.getInputRequestForUi() != null) {
				break;
			}
		}
		if (burst > 0) {
			refreshFromEngine();
		}
		if (engine != null && engine.getInputRequestForUi() != null) {
			stopPlayInternal();
			updateTransportButtons();
		} else if (engine == null || !engine.getCanStep()) {
			stopPlayInternal();
			updateTransportButtons();
		}
	}

	private void onStep() {
		if (engine == null) {
			return;
		}
		stopPlayInternal();
		if (!engine.getCanStep()) {
			updateTransportButtons();
			applyDiagramHighlight();
			return;
		}
		List<Integer> before = copyPath(engine.getCurrentStepPath());
		engine.step();
		lastExecutedSimulationPath = before;
		refreshFromEngine();
	}

	private void onStop() {
		if (engine == null) {
			return;
		}
		stopPlayInternal();
		lastExecutedSimulationPath = null;
		engine.reset(null);
		refreshFromEngine();
	}

	private void onSubmitInput() {
		if (engine == null || !inputField.isEnabled()) {
			return;
		}
		List<Integer> before = copyPath(engine.getCurrentStepPath());
		engine.provideInput(inputField.getText());
		inputField.setText("");
		lastExecutedSimulationPath = before;
		refreshFromEngine();
	}

	/** Menü-Sprache wechseln: Texte neu setzen. */
	public void refreshLocalizedTexts() {
		applyTransportLabelsAndTips();
		applyHighlightLabels();
		javax.swing.border.Border inB = inputSectionPanel.getBorder();
		if (inB instanceof javax.swing.border.TitledBorder) {
			((javax.swing.border.TitledBorder) inB).setTitle(I18n.tr("simulation.input"));
		}
		javax.swing.border.Border b = controlsNorthPanel.getBorder();
		if (b instanceof javax.swing.border.TitledBorder) {
			((javax.swing.border.TitledBorder) b).setTitle(I18n.tr("simulation.controls"));
		}
		if (engine != null) {
			refreshFromEngine();
		} else {
			clearEngine();
		}
	}
}
