package de.visustruct.view;

import java.awt.Font;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import de.visustruct.control.CanvasStyle;

/** Globale UI-Defaults nach Theme-Wechsel; eigene Paletten für „Modern · hell“ und „Modern · dunkel“. */
public final class UiTheme {

	private UiTheme() {
	}

	public static void applyAfterTheme() {
		UIManager.put("ScrollPane.smoothScrolling", Boolean.TRUE);

		if (UIManager.getLookAndFeel() instanceof FlatLaf) {
			String os = System.getProperty("os.name", "").toLowerCase();
			Font uiFont = os.contains("win")
					? new Font("Segoe UI", Font.PLAIN, 13)
					: new Font(Font.SANS_SERIF, Font.PLAIN, 13);
			UIManager.put("defaultFont", new FontUIResource(uiFont));

			if (UIManager.getLookAndFeel() instanceof FlatLightLaf) {
				VisuStructTheme.applyLightPalette();
			} else if (UIManager.getLookAndFeel() instanceof FlatDarkLaf
					|| UIManager.getLookAndFeel() instanceof FlatMacDarkLaf) {
				VisuStructTheme.applyDarkPalette();
			}

			UIManager.put("Component.arc", Integer.valueOf(8));
			UIManager.put("Button.arc", Integer.valueOf(10));
			UIManager.put("TextComponent.arc", Integer.valueOf(8));
			UIManager.put("ScrollBar.width", Integer.valueOf(12));
			UIManager.put("ScrollBar.thumbArc", Integer.valueOf(999));
			UIManager.put("TabbedPane.tabHeight", Integer.valueOf(34));
			UIManager.put("TabbedPane.tabsOverlap", Integer.valueOf(0));
			UIManager.put("TabbedPane.tabInsets", new InsetsUIResource(6, 14, 6, 14));
			UIManager.put("TabbedPane.showTabSeparators", Boolean.TRUE);
			UIManager.put("MenuItem.margin", new InsetsUIResource(4, 10, 4, 10));
			UIManager.put("MenuBar.horizontalGripEnabled", Boolean.FALSE);
		} else {
			UIManager.put("Component.arc", Integer.valueOf(6));
			UIManager.put("Button.arc", Integer.valueOf(8));
			UIManager.put("TextComponent.arc", Integer.valueOf(8));
			UIManager.put("ScrollBar.width", Integer.valueOf(10));
			UIManager.put("TabbedPane.tabHeight", Integer.valueOf(28));
			UIManager.put("TabbedPane.tabsOverlap", Integer.valueOf(0));
		}

		CanvasStyle.syncToTheme();
	}
}
