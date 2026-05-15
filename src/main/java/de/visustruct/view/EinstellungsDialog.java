package de.visustruct.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import de.visustruct.control.GlobalSettings;
import de.visustruct.i18n.I18n;

public class EinstellungsDialog extends JDialog {

	private static final long serialVersionUID = -6402017961524470279L;

	public static final int anzahlStruktogrammElemente = 10;

	private final GUI hostGui;
	private final JComboBox<Double> simulationSpeedCombo;
	private final JRadioButton simulationHighlightLastRadio = new JRadioButton();
	private final JRadioButton simulationHighlightNextRadio = new JRadioButton();

	public EinstellungsDialog(GUI gui, boolean modal) {
		super(gui, I18n.tr("dialog.elementText.title"), modal);
		hostGui = gui;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setSize(500, 540);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((d.width - getSize().width) / 2, (d.height - getSize().height) / 2);
		setLayout(new BorderLayout(10, 10));

		JLabel kopf = new JLabel("<html><div style='width:420px'>" + I18n.tr("dialog.elementText.intro") + "</div></html>");
		kopf.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
		add(kopf, BorderLayout.NORTH);

		simulationSpeedCombo = new JComboBox<>();
		for (double sec : GlobalSettings.SIMULATION_PLAY_DELAY_SECONDS) {
			simulationSpeedCombo.addItem(sec);
		}
		simulationSpeedCombo.setRenderer(new SimulationSpeedListRenderer());
		simulationSpeedCombo.setSelectedIndex(GlobalSettings.getSimulationPlayDelayIndex());

		JPanel simulationPanel = new JPanel();
		simulationPanel.setLayout(new BoxLayout(simulationPanel, BoxLayout.Y_AXIS));
		simulationPanel.setBorder(BorderFactory.createTitledBorder(I18n.tr("dialog.elementText.simulationSection")));

		JPanel speedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		speedRow.add(new JLabel(I18n.tr("dialog.elementText.simulationSpeedLabel")));
		speedRow.add(simulationSpeedCombo);
		simulationPanel.add(speedRow);

		JPanel highlightBlock = new JPanel();
		highlightBlock.setLayout(new BoxLayout(highlightBlock, BoxLayout.Y_AXIS));
		highlightBlock.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		highlightBlock.add(new JLabel(I18n.tr("dialog.elementText.simulationHighlightLabel")));
		ButtonGroup highlightGroup = new ButtonGroup();
		highlightGroup.add(simulationHighlightLastRadio);
		highlightGroup.add(simulationHighlightNextRadio);
		simulationHighlightLastRadio.setText(I18n.tr("dialog.elementText.simulationHighlightLast"));
		simulationHighlightNextRadio.setText(I18n.tr("dialog.elementText.simulationHighlightNext"));
		if (GlobalSettings.isSimulationHighlightNextStep()) {
			simulationHighlightNextRadio.setSelected(true);
		} else {
			simulationHighlightLastRadio.setSelected(true);
		}
		JPanel highlightRadios = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		highlightRadios.add(simulationHighlightLastRadio);
		highlightRadios.add(simulationHighlightNextRadio);
		highlightBlock.add(highlightRadios);
		simulationPanel.add(Box.createVerticalStrut(6));
		simulationPanel.add(highlightBlock);

		int startIdx = GlobalSettings.getElementBeschriftungPresetIndex();
		if (startIdx < 0 || startIdx >= ElementBeschriftungPresets.ANZAHL_PRESETS) {
			startIdx = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA;
		}

		JPanel radioPanel = new JPanel();
		radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
		radioPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		ButtonGroup gruppe = new ButtonGroup();
		JRadioButton[] radios = new JRadioButton[ElementBeschriftungPresets.ANZAHL_PRESETS];
		int startDialogPlatz = ElementBeschriftungPresets.dialogPlatzFuerPreset(startIdx);
		for (int u = 0; u < ElementBeschriftungPresets.PRESET_DIALOG_REIHENFOLGE.length; u++) {
			int preset = ElementBeschriftungPresets.PRESET_DIALOG_REIHENFOLGE[u];
			JRadioButton rb = new JRadioButton(ElementBeschriftungPresets.getPresetAnzeigename(preset));
			rb.setActionCommand(Integer.toString(u));
			gruppe.add(rb);
			radioPanel.add(rb);
			radioPanel.add(Box.createVerticalStrut(2));
			radios[u] = rb;
			if (u == startDialogPlatz) {
				rb.setSelected(true);
			}
		}

		JTextArea vorschau = new JTextArea();
		vorschau.setEditable(false);
		vorschau.setLineWrap(true);
		vorschau.setWrapStyleWord(true);
		vorschau.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		vorschau.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		vorschau.setText(ElementBeschriftungPresets.alsVorschauText(startIdx));

		Runnable vorschauAktualisieren = () -> {
			int sel = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA;
			for (int u = 0; u < radios.length; u++) {
				if (radios[u].isSelected()) {
					sel = ElementBeschriftungPresets.presetIndexAtDialogPlatz(u);
					break;
				}
			}
			vorschau.setText(ElementBeschriftungPresets.alsVorschauText(sel));
		};
		for (JRadioButton rb : radios) {
			rb.addActionListener(e -> vorschauAktualisieren.run());
		}

		JScrollPane scroll = new JScrollPane(vorschau);
		scroll.setBorder(BorderFactory.createTitledBorder(I18n.tr("dialog.elementText.previewTitle")));

		JPanel mitte = new JPanel(new BorderLayout(0, 8));
		mitte.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));
		mitte.add(simulationPanel, BorderLayout.NORTH);
		mitte.add(radioPanel, BorderLayout.CENTER);
		mitte.add(scroll, BorderLayout.SOUTH);
		add(mitte, BorderLayout.CENTER);

		JPanel unten = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton abbrechen = new JButton(I18n.tr("dialog.elementText.cancel"));
		abbrechen.addActionListener(e -> setVisible(false));
		JButton ok = new JButton(I18n.tr("dialog.elementText.ok"));
		ok.addActionListener(e -> applyAndClose(radios));
		unten.add(abbrechen);
		unten.add(ok);
		add(unten, BorderLayout.SOUTH);

		setVisible(true);
	}

	private void applyAndClose(JRadioButton[] radios) {
		Double speed = (Double) simulationSpeedCombo.getSelectedItem();
		if (speed != null) {
			GlobalSettings.setSimulationPlayDelaySec(speed);
		}
		GlobalSettings.setSimulationHighlightMode(
			simulationHighlightNextRadio.isSelected()
				? GlobalSettings.SimulationHighlightMode.NEXT_STEP
				: GlobalSettings.SimulationHighlightMode.LAST_EXECUTED);
		int sel = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA;
		for (int u = 0; u < radios.length; u++) {
			if (radios[u].isSelected()) {
				sel = ElementBeschriftungPresets.presetIndexAtDialogPlatz(u);
				break;
			}
		}
		GlobalSettings.wendeElementBeschriftungsPresetAn(sel);
		GlobalSettings.saveSettings();
		SwingUtilities.invokeLater(() -> {
			hostGui.rebuildMenuBar();
			hostGui.gibAuswahlPanel().aktualisiereBeschriftungen();
			hostGui.gibAuswahlPanel().revalidate();
			hostGui.gibAuswahlPanel().repaint();
			hostGui.getSimulationPanel().onSimulationSettingsChanged();
			SwingUtilities.invokeLater(() -> hostGui.gibAuswahlPanel().aktualisiereBeschriftungen());
		});
		setVisible(false);
	}

	static String formatSimulationSpeedLabel(double seconds) {
		NumberFormat nf = NumberFormat.getNumberInstance(GlobalSettings.getUiLocale());
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(0);
		return nf.format(seconds) + " " + I18n.tr("dialog.elementText.simulationSpeedUnit");
	}

	private static final class SimulationSpeedListRenderer extends JLabel implements ListCellRenderer<Double> {

		private static final long serialVersionUID = 1L;

		SimulationSpeedListRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Double> list, Double value, int index,
				boolean isSelected, boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setText(value == null ? "" : formatSimulationSpeedLabel(value));
			return this;
		}
	}
}
