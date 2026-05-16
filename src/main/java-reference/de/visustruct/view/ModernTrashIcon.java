package de.visustruct.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * Flaches Papierkorbsymbol (ohne Pixelgrafik), passt zu FlatLaf / hellem und dunklem Thema.
 */
public final class ModernTrashIcon implements Icon {

	private static final int W = 34;
	private static final int H = 37;

	private final boolean zielAktiv;

	public ModernTrashIcon(boolean zielAktiv) {
		this.zielAktiv = zielAktiv;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.translate(x, y);
		g2.scale(0.77, 0.77);

		Color fg = UIManager.getColor("Label.foreground");
		if (fg == null) {
			fg = new Color(0x37, 0x41, 0x51);
		}
		Color akzent = UIManager.getColor("Component.accentColor");
		if (akzent == null) {
			akzent = new Color(0x25, 0x63, 0xEB);
		}

		float stroke = 1.35f;
		g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		Color kontur = zielAktiv ? akzent : fg;
		int fillAlpha = zielAktiv ? 110 : 70;

		float lidDy = zielAktiv ? -2f : 0f;
		RoundRectangle2D.Float deckel = new RoundRectangle2D.Float(6f, 7f + lidDy, 32f, 9f, 5f, 5f);
		g2.setColor(mitAlpha(fg, zielAktiv ? 90 : 55));
		g2.fill(deckel);
		g2.setColor(kontur);
		g2.draw(deckel);

		g2.setColor(kontur);
		g2.drawRoundRect(17, 4 + (int) lidDy, 10, 5, 3, 3);

		RoundRectangle2D.Float korpus = new RoundRectangle2D.Float(9f, 16f + lidDy * 0.4f, 26f, 28f, 7f, 7f);
		g2.setColor(mitAlpha(fg, fillAlpha));
		g2.fill(korpus);
		g2.setColor(kontur);
		g2.draw(korpus);

		Color streifen = mitAlpha(fg, 140);
		g2.setColor(streifen);
		g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		float mx = 22f;
		float top = 22f + lidDy * 0.4f;
		float bottom = 38f + lidDy * 0.4f;
		g2.drawLine((int) (mx - 5.5f), (int) top, (int) (mx - 5.5f), (int) bottom);
		g2.drawLine((int) mx, (int) top, (int) mx, (int) bottom);
		g2.drawLine((int) (mx + 5.5f), (int) top, (int) (mx + 5.5f), (int) bottom);

		g2.dispose();
	}

	private static Color mitAlpha(Color c, int alpha) {
		int a = Math.max(0, Math.min(255, alpha));
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
	}

	@Override
	public int getIconWidth() {
		return W;
	}

	@Override
	public int getIconHeight() {
		return H;
	}
}
