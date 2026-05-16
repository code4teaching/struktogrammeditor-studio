package de.visustruct.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import de.visustruct.control.GlobalSettings;
import de.visustruct.i18n.I18n;

/** Einstellungen zur Simulation (Geschwindigkeit, Diagramm-Markierung). */
public class SimulationEinstellungenDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final GUI hostGui;
	private final JComboBox<Double> simulationSpeedCombo;
	private final JRadioButton simulationHighlightLastRadio = new JRadioButton();
	private final JRadioButton simulationHighlightNextRadio = new JRadioButton();

	public SimulationEinstellungenDialog(GUI gui, boolean modal) {
		super(gui, I18n.tr("menu.settings.simulation"), modal);
		hostGui = gui;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setSize(440, 220);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((d.width - getSize().width) / 2, (d.height - getSize().height) / 2);
		setLayout(new BorderLayout(10, 10));

		simulationSpeedCombo = new JComboBox<>();
		for (double sec : GlobalSettings.SIMULATION_PLAY_DELAY_SECONDS) {
			simulationSpeedCombo.addItem(sec);
		}
		simulationSpeedCombo.setRenderer(new EinstellungsDialog.SimulationSpeedListRenderer());
		simulationSpeedCombo.setSelectedIndex(GlobalSettings.getSimulationPlayDelayIndex());

		JPanel simulationPanel = new JPanel();
		simulationPanel.setLayout(new BoxLayout(simulationPanel, BoxLayout.Y_AXIS));
		simulationPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

		JPanel speedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		speedRow.add(new JLabel(I18n.tr("dialog.elementText.simulationSpeedLabel")));
		speedRow.add(simulationSpeedCombo);
		simulationPanel.add(speedRow);

		JPanel highlightBlock = new JPanel();
		highlightBlock.setLayout(new BoxLayout(highlightBlock, BoxLayout.Y_AXIS));
		highlightBlock.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
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

		add(simulationPanel, BorderLayout.CENTER);

		JPanel unten = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton abbrechen = new JButton(I18n.tr("dialog.elementText.cancel"));
		abbrechen.addActionListener(e -> setVisible(false));
		JButton ok = new JButton(I18n.tr("dialog.elementText.ok"));
		ok.addActionListener(e -> applyAndClose());
		unten.add(abbrechen);
		unten.add(ok);
		add(unten, BorderLayout.SOUTH);

		setVisible(true);
	}

	private void applyAndClose() {
		Double speed = (Double) simulationSpeedCombo.getSelectedItem();
		if (speed != null) {
			GlobalSettings.setSimulationPlayDelaySec(speed);
		}
		GlobalSettings.setSimulationHighlightMode(
				simulationHighlightNextRadio.isSelected()
						? GlobalSettings.SimulationHighlightMode.NEXT_STEP
						: GlobalSettings.SimulationHighlightMode.LAST_EXECUTED);
		GlobalSettings.saveSettings();
		SwingUtilities.invokeLater(() -> hostGui.getSimulationPanel().onSimulationSettingsChanged());
		setVisible(false);
	}
}
