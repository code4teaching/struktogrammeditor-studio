package de.visustruct.view;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import de.visustruct.control.GlobalSettings;
import de.visustruct.i18n.StructureElementI18n;

/** Klickbare Kachel zum Ziehen eines neuen Struktogramm-Elements (FlatLaf-Button-Look). */
public class AuswahlPanelElement extends JButton {

	private static final long serialVersionUID = 5455270690460661892L;
	public static final String iconOrdner = "/icons/";

	private final int typ;

	public AuswahlPanelElement(int typ) {
		this.typ = typ;
		setFocusable(false);
		setRequestFocusEnabled(false);
		setRolloverEnabled(true);
		setDefaultCapable(false);
		setIcon(erzeugeIcon(typ));
		setVerticalTextPosition(SwingConstants.CENTER);
		setHorizontalTextPosition(SwingConstants.RIGHT);
		setHorizontalAlignment(SwingConstants.LEFT);
		setIconTextGap(10);
		setMargin(new Insets(4, 10, 4, 10));
		aktualisiereBeschriftung();
		PaletteButtonStyle.apply(this);
		// Ohne released-Event (typisch nach DnD) bleibt das Button-Model „pressed“ → grauer Kasten
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				SwingUtilities.invokeLater(() -> PaletteButtonStyle.clearPressedArmedState(AuswahlPanelElement.this));
			}
		});
	}

	public void aktualisiereBeschriftung() {
		String kurz = GlobalSettings.gibPaletteButtonBeschriftung(typ);
		setFont(new Font(Font.SANS_SERIF, Font.BOLD, textFontSizeFuerZeile(kurz)));
		setText(kurz);
		// Didaktischer Blockname in der UI-Sprache (z. B. Kopf- vs. fußgesteuerte Schleife trotz gleichem Java-Wort auf zwei Buttons).
		setToolTipText(StructureElementI18n.paletteTooltip(typ));
		invalidate();
		revalidate();
		repaint();
	}

	public int gibTyp() {
		return typ;
	}

	private static int textFontSizeFuerZeile(String s) {
		if (s == null || s.length() <= 12) {
			return 12;
		}
		if (s.length() <= 22) {
			return 11;
		}
		return 10;
	}

	private static FlatSVGIcon erzeugeIcon(int typ) {
		String name = iconNameFuerTyp(typ);
		if (name.isEmpty()) {
			return null;
		}
		return new FlatSVGIcon("icons/lucide/" + name + ".svg", 18, 18);
	}

	private static String iconNameFuerTyp(int typ) {
		switch (typ) {
		case 0:
			return "square";
		case 1:
			return "split";
		case 2:
			return "list-tree";
		case 3:
			return "repeat";
		case 4:
			return "refresh-cw";
		case 5:
			return "rotate-ccw";
		case 6:
			return "infinity";
		case 7:
			return "corner-down-left";
		case 8:
			return "log-in";
		default:
			return "";
		}
	}
}
