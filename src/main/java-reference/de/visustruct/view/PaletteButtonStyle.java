package de.visustruct.view;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * Gleiche Darstellung aller Schaltflächen in der linken Palette (Farbe, Form, kein „Default“-Button).
 */
public final class PaletteButtonStyle {

	private static final int MIN_HEIGHT = 32;

	private PaletteButtonStyle() {
	}

	public static void apply(JButton b) {
		b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
		b.putClientProperty(FlatClientProperties.MINIMUM_HEIGHT, Integer.valueOf(MIN_HEIGHT));
		b.setDefaultCapable(false);
		b.setRolloverEnabled(true);
		b.setOpaque(true);
		b.setContentAreaFilled(true);
		b.setBorderPainted(true);

		Color bg = UIManager.getColor("Button.background");
		Color fg = UIManager.getColor("Button.foreground");
		if (bg != null) {
			b.setBackground(bg);
		}
		if (fg != null) {
			b.setForeground(fg);
		}
	}

	/**
	 * Nach Drag&amp;Drop bleibt bei {@link JButton} oft {@code pressed}/{@code armed} hängen — FlatLaf
	 * zeichnet dann dauerhaft grau. Aufruf nach Beendigung des Drags oder in {@code mouseReleased}.
	 */
	public static void clearPressedArmedState(Component c) {
		if (!(c instanceof JButton)) {
			return;
		}
		JButton b = (JButton) c;
		Runnable reset = () -> {
			b.getModel().setPressed(false);
			b.getModel().setArmed(false);
			b.repaint();
		};
		if (SwingUtilities.isEventDispatchThread()) {
			reset.run();
		} else {
			SwingUtilities.invokeLater(reset);
		}
	}
}
