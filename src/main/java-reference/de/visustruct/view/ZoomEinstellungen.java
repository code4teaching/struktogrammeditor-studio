package de.visustruct.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import de.visustruct.control.GlobalSettings;
import de.visustruct.other.JNumberField;

public class ZoomEinstellungen extends JDialog {
	private static final long serialVersionUID = -4780523744293396039L;
	private JNumberField numberfieldX, numberfieldY;

	public ZoomEinstellungen(GUI gui){
		super(gui, "Zoom Settings", true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());

		JPanel shell = new JPanel(new BorderLayout());
		// Einheitlicher Abstand Schrift zu allen Fensterrändern (u. a. macOS)
		shell.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
		add(shell, BorderLayout.CENTER);

		JLabel header = new JLabel(
				"<html><body style='width:300px;'><b>How many pixels should a block grow or shrink per step (horizontal and vertical)?</b></body></html>");
		header.setBorder(BorderFactory.createEmptyBorder(6, 0, 14, 0));
		shell.add(header, BorderLayout.NORTH);

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.insets = new Insets(8, 0, 8, 12);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		form.add(new JLabel("Horizontal step (px):"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		numberfieldX = new JNumberField();
		numberfieldX.setColumns(6);
		numberfieldX.setInt(GlobalSettings.getXZoomProSchritt());
		form.add(numberfieldX, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		form.add(new JLabel("Vertical step (px):"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		numberfieldY = new JNumberField();
		numberfieldY.setColumns(6);
		numberfieldY.setInt(GlobalSettings.getYZoomProSchritt());
		form.add(numberfieldY, gbc);

		shell.add(form, BorderLayout.CENTER);

		JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
		buttonRow.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				okGeklickt();
			}
		});
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				abbrechenGeklickt();
			}
		});
		buttonRow.add(ok);
		buttonRow.add(cancel);
		shell.add(buttonRow, BorderLayout.SOUTH);

		pack();
		setMinimumSize(new Dimension(320, getPreferredSize().height));
		setLocationRelativeTo(gui);
		setVisible(true);
	}

	private void okGeklickt(){
		if(numberfieldX.isNumeric() && numberfieldY.isNumeric()){
			GlobalSettings.setXZoomProSchritt(numberfieldX.getInt());
			GlobalSettings.setYZoomProSchritt(numberfieldY.getInt());
			GlobalSettings.saveSettings();
			dispose();
		}
	}

	private void abbrechenGeklickt(){
		dispose();
	}
}
