package de.visustruct.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import de.visustruct.control.Controlling;
import de.visustruct.control.GlobalSettings;
import de.visustruct.control.Struktogramm;
import de.visustruct.i18n.I18n;
import de.visustruct.simulation.SimulationEngine;
import de.visustruct.simulation.SimulationInputRequest;

/**
 * Simulations-Ansicht: Steuerung, Eingabe, Variablen, aktueller Block, Ausgabe (von oben nach unten).
 * Diagramm-Markierung: {@link GlobalSettings#getSimulationHighlightMode()}.
 */
public class SimulationPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final String SYM_PLAY = "\u25B6";
	private static final String SYM_PAUSE = "\u23F8";
	private static final String SYM_STEP = "\u2192";
	private static final String SYM_STOP = "\u23F9";
	private static final String SYM_SUBMIT = "\u2713";

	private final Controlling controlling;
	private SimulationEngine engine;

	private final JTextArea variablesArea = new JTextArea(4, 40);
	private final JTextArea currentBlockArea = new JTextArea(3, 40);
	private final JTextArea outputArea = new JTextArea(6, 40);
	private final JLabel messageLabel = new JLabel(" ");
	private final JLabel traceLabel = new JLabel(" ");
	private final JLabel inputPromptLabel = new JLabel(" ");
	private final JTextField inputField = new JTextField(24);
	private final JButton inputSubmitButton = new JButton();
	private final JButton playButton = new JButton();
	private final JButton pauseButton = new JButton();
	private final JButton stepButton = new JButton();
	private final JButton stopButton = new JButton();

	private final Timer playTimer;
	private boolean playing;
	private boolean resumePlayAfterInput;

	private JPanel controlsNorthPanel;
	private JPanel inputSectionPanel;
	private JPanel currentBlockPanel;
	private JScrollPane variablesScroll;
	private JScrollPane outputScroll;

	private List<Integer> lastExecutedSimulationPath;

	public SimulationPanel(Controlling controlling) {
		super(new BorderLayout(8, 8));
		this.controlling = controlling;
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		Font mono = Font.decode(Font.MONOSPACED + "-PLAIN-13");
		variablesArea.setFont(mono);
		currentBlockArea.setFont(mono);
		outputArea.setFont(mono);
		variablesArea.setEditable(false);
		currentBlockArea.setEditable(false);
		outputArea.setEditable(false);
		currentBlockArea.setLineWrap(true);
		currentBlockArea.setWrapStyleWord(true);

		Font statusFont = mono.deriveFont(Font.ITALIC, 12f);
		messageLabel.setFont(statusFont);
		traceLabel.setFont(statusFont);

		playTimer = new Timer(GlobalSettings.getSimulationPlayDelayMs(), e -> onPlayTick());
		playTimer.setRepeats(true);

		playButton.addActionListener(e -> onPlay());
		pauseButton.addActionListener(e -> onPause());
		stepButton.addActionListener(e -> onStep());
		stopButton.addActionListener(e -> onStop());
		inputSubmitButton.addActionListener(e -> onSubmitInput());
		inputField.addActionListener(e -> onSubmitInput());

		JPanel transport = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		transport.add(playButton);
		transport.add(pauseButton);
		transport.add(stepButton);
		transport.add(stopButton);

		controlsNorthPanel = new JPanel(new BorderLayout(0, 4));
		controlsNorthPanel.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.controls")));
		controlsNorthPanel.add(transport, BorderLayout.NORTH);
		controlsNorthPanel.add(messageLabel, BorderLayout.SOUTH);

		inputSectionPanel = new JPanel(new BorderLayout(0, 6));
		inputSectionPanel.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.input")));
		inputSectionPanel.add(inputPromptLabel, BorderLayout.NORTH);
		JPanel inputRow = new JPanel(new BorderLayout(8, 0));
		inputRow.add(inputField, BorderLayout.CENTER);
		inputRow.add(inputSubmitButton, BorderLayout.EAST);
		inputSectionPanel.add(inputRow, BorderLayout.CENTER);
		inputSectionPanel.setPreferredSize(new Dimension(260, 72));

		variablesScroll = new JScrollPane(variablesArea);
		variablesScroll.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.variables")));
		variablesScroll.setPreferredSize(new Dimension(260, 88));

		currentBlockPanel = new JPanel(new BorderLayout(0, 4));
		currentBlockPanel.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.path")));
		currentBlockPanel.add(new JScrollPane(currentBlockArea), BorderLayout.CENTER);
		currentBlockPanel.add(traceLabel, BorderLayout.SOUTH);
		currentBlockPanel.setPreferredSize(new Dimension(260, 96));

		outputScroll = new JScrollPane(outputArea);
		outputScroll.setBorder(BorderFactory.createTitledBorder(I18n.tr("simulation.output")));

		JPanel topStack = new JPanel();
		topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
		topStack.add(controlsNorthPanel);
		topStack.add(Box.createVerticalStrut(8));
		topStack.add(inputSectionPanel);
		topStack.add(Box.createVerticalStrut(8));
		topStack.add(variablesScroll);
		topStack.add(Box.createVerticalStrut(8));
		topStack.add(currentBlockPanel);

		add(topStack, BorderLayout.NORTH);
		add(outputScroll, BorderLayout.CENTER);

		applyTransportLabelsAndTips();
		setMinimumSize(new Dimension(260, 320));
		clearEngine();
	}

	private static List<Integer> copyPath(List<Integer> path) {
		if (path == null || path.isEmpty()) {
			return null;
		}
		return new ArrayList<>(path);
	}

	private static boolean isStrictPathExtension(List<Integer> before, List<Integer> after) {
		if (before == null || after == null || after.size() <= before.size()) {
			return false;
		}
		for (int i = 0; i < before.size(); i++) {
			if (!before.get(i).equals(after.get(i))) {
				return false;
			}
		}
		return true;
	}

	private void recordStepHighlightPath(List<Integer> pathBeforeStep) {
		if (engine == null) {
			return;
		}
		List<Integer> after = copyPath(engine.getCurrentStepPath());
		if (isStrictPathExtension(pathBeforeStep, after)) {
			lastExecutedSimulationPath = after;
		} else {
			lastExecutedSimulationPath = pathBeforeStep;
		}
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

	public void onSimulationSettingsChanged() {
		if (playTimer != null) {
			playTimer.setDelay(GlobalSettings.getSimulationPlayDelayMs());
		}
		if (engine != null) {
			refreshFromEngine();
		}
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
		str.setzeSimulationSpotlightPfad(resolveHighlightPath());
	}

	private List<Integer> resolveHighlightPath() {
		if (engine == null) {
			return null;
		}
		List<Integer> current = copyPath(engine.getCurrentStepPath());
		boolean simAmEnde = engine.getInputRequestForUi() == null && !engine.getCanStep();

		if (GlobalSettings.isSimulationHighlightNextStep()) {
			if (current != null && !current.isEmpty()) {
				return current;
			}
			if (simAmEnde && lastExecutedSimulationPath != null) {
				return copyPath(lastExecutedSimulationPath);
			}
			return null;
		}

		List<Integer> last = copyPath(lastExecutedSimulationPath);
		if (last != null && !last.isEmpty()) {
			return last;
		}
		if (!simAmEnde && current != null && !current.isEmpty()) {
			return current;
		}
		return null;
	}

	private String resolveCurrentBlockDisplayText() {
		if (engine == null) {
			return "";
		}
		List<Integer> highlightPath = resolveHighlightPath();
		String stepText = engine.findStepTextByPath(highlightPath);
		if (stepText != null && !stepText.isBlank()) {
			return stepText.trim();
		}
		if (!engine.getCanStep()) {
			String msg = engine.getUiMessage();
			if (msg != null && !msg.isBlank()) {
				return msg.trim();
			}
			return "—";
		}
		return "—";
	}

	private void updateStatusAndBlockPanels() {
		String msg = engine != null ? engine.getUiMessage() : null;
		boolean showStatus =
			msg != null && !msg.isBlank()
				&& (engine.getInputRequestForUi() == null || engine.getInputErrorForUi() == null);
		messageLabel.setText(showStatus ? msg.trim() : " ");
		messageLabel.setVisible(showStatus);

		currentBlockArea.setText(resolveCurrentBlockDisplayText());

		String tr = engine != null ? engine.getUiLastTrace() : null;
		boolean showTrace = tr != null && !tr.isBlank();
		traceLabel.setText(showTrace ? tr.trim() : " ");
		traceLabel.setVisible(showTrace);
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
		resumePlayAfterInput = false;
		lastExecutedSimulationPath = null;
		this.engine = eng;
		refreshFromEngine();
	}

	public void clearEngine() {
		stopPlayInternal();
		resumePlayAfterInput = false;
		this.engine = null;
		lastExecutedSimulationPath = null;
		clearDiagramSimulationHighlight();
		variablesArea.setText("");
		currentBlockArea.setText("");
		outputArea.setText("");
		messageLabel.setText(" ");
		messageLabel.setVisible(false);
		traceLabel.setText(" ");
		traceLabel.setVisible(false);
		inputPromptLabel.setText(" ");
		inputField.setText("");
		setInputSectionIdle();
		updateTransportButtons();
	}

	private void setInputSectionIdle() {
		inputPromptLabel.setText(I18n.tr("simulation.input.idle"));
		inputField.setText("");
		inputField.setEnabled(false);
		inputSubmitButton.setEnabled(false);
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
		String outText = out.toString();
		outputArea.setText(outText);
		if (!outText.isEmpty()) {
			outputArea.setCaretPosition(outText.length());
		}

		updateStatusAndBlockPanels();

		SimulationInputRequest req = engine.getInputRequestForUi();
		String err = engine.getInputErrorForUi();
		if (req != null) {
			if (playing) {
				resumePlayAfterInput = true;
			}
			stopPlayInternal();
			inputPromptLabel.setText(req.getPrompt());
			inputField.setEnabled(true);
			inputSubmitButton.setEnabled(true);
			if (err != null && !err.isEmpty()) {
				messageLabel.setText(err);
				messageLabel.setVisible(true);
			}
			SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
		} else {
			setInputSectionIdle();
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
	}

	private void onPlay() {
		if (engine == null || !engine.getCanStep()) {
			return;
		}
		playTimer.setDelay(GlobalSettings.getSimulationPlayDelayMs());
		playing = true;
		playTimer.start();
		updateTransportButtons();
	}

	private void onPause() {
		resumePlayAfterInput = false;
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
		List<Integer> before = copyPath(engine.getCurrentStepPath());
		engine.step();
		recordStepHighlightPath(before);
		refreshFromEngine();
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
		resumePlayAfterInput = false;
		stopPlayInternal();
		if (!engine.getCanStep()) {
			updateTransportButtons();
			applyDiagramHighlight();
			return;
		}
		List<Integer> before = copyPath(engine.getCurrentStepPath());
		engine.step();
		recordStepHighlightPath(before);
		refreshFromEngine();
	}

	private void onStop() {
		if (engine == null) {
			return;
		}
		resumePlayAfterInput = false;
		stopPlayInternal();
		lastExecutedSimulationPath = null;
		engine.reset(null);
		inputField.setText("");
		refreshFromEngine();
	}

	private void onSubmitInput() {
		if (engine == null || !inputField.isEnabled()) {
			return;
		}
		boolean shouldResumePlay = resumePlayAfterInput;
		List<Integer> before = copyPath(engine.getCurrentStepPath());
		engine.provideInput(inputField.getText());
		inputField.setText("");
		recordStepHighlightPath(before);
		refreshFromEngine();
		if (shouldResumePlay && engine != null && engine.getInputRequestForUi() == null && engine.getCanStep()) {
			resumePlayAfterInput = false;
			onPlay();
		}
	}

	public void refreshLocalizedTexts() {
		applyTransportLabelsAndTips();
		setTitledBorderTitle(controlsNorthPanel, I18n.tr("simulation.controls"));
		setTitledBorderTitle(inputSectionPanel, I18n.tr("simulation.input"));
		setTitledBorderTitle(variablesScroll, I18n.tr("simulation.variables"));
		setTitledBorderTitle(currentBlockPanel, I18n.tr("simulation.path"));
		setTitledBorderTitle(outputScroll, I18n.tr("simulation.output"));
		if (engine != null) {
			refreshFromEngine();
		} else {
			clearEngine();
		}
	}

	private static void setTitledBorderTitle(JPanel panel, String title) {
		if (panel.getBorder() instanceof javax.swing.border.TitledBorder tb) {
			tb.setTitle(title);
		}
	}

	private static void setTitledBorderTitle(JScrollPane scroll, String title) {
		if (scroll.getBorder() instanceof javax.swing.border.TitledBorder tb) {
			tb.setTitle(title);
		}
	}
}
