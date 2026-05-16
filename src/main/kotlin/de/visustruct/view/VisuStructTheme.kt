package de.visustruct.view

import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

/**
 * Angepasste Farben für **Modern · hell** (visustruct.de) und **Modern · dunkel** (deutlich dunkle Grundflächen).
 * Schlüssel `VisuStruct.paletteBackground` für die linke Werkzeugleiste.
 */
object VisuStructTheme {

    const val KEY_PALETTE_BACKGROUND = "VisuStruct.paletteBackground"

    private fun c(rgb: Int) = ColorUIResource(rgb)

    @JvmStatic
    fun applyLightPalette() {
        val bg = 0xFAFAFA
        val surface = 0xFFFFFF
        val toolbar = 0xF0F0F0
        val border = 0xE5E7EB
        val text = 0x111827
        val muted = 0x6B7280
        val accent = 0x3B82F6
        val select = 0xE5E7EB
        val tabHover = 0xF3F4F6

        UIManager.put(KEY_PALETTE_BACKGROUND, c(toolbar))
        UIManager.put("Panel.background", c(bg))
        UIManager.put("Viewport.background", c(bg))
        UIManager.put("ScrollPane.background", c(bg))
        UIManager.put("SplitPane.background", c(bg))
        UIManager.put("MenuBar.background", c(surface))
        UIManager.put("MenuBar.foreground", c(text))
        UIManager.put("Menu.background", c(surface))
        UIManager.put("Menu.foreground", c(text))
        UIManager.put("PopupMenu.background", c(surface))
        UIManager.put("PopupMenu.foreground", c(text))
        UIManager.put("Menu.selectionBackground", c(select))
        UIManager.put("Menu.selectionForeground", c(text))
        UIManager.put("Menu.acceleratorForeground", c(muted))
        UIManager.put("MenuItem.background", c(surface))
        UIManager.put("MenuItem.foreground", c(text))
        UIManager.put("MenuItem.disabledForeground", c(muted))
        UIManager.put("MenuItem.selectionBackground", c(select))
        UIManager.put("MenuItem.selectionForeground", c(text))
        UIManager.put("CheckBoxMenuItem.background", c(surface))
        UIManager.put("CheckBoxMenuItem.foreground", c(text))
        UIManager.put("CheckBoxMenuItem.disabledForeground", c(muted))
        UIManager.put("CheckBoxMenuItem.selectionBackground", c(select))
        UIManager.put("CheckBoxMenuItem.selectionForeground", c(text))
        UIManager.put("RadioButtonMenuItem.background", c(surface))
        UIManager.put("RadioButtonMenuItem.foreground", c(text))
        UIManager.put("RadioButtonMenuItem.disabledForeground", c(muted))
        UIManager.put("RadioButtonMenuItem.selectionBackground", c(select))
        UIManager.put("RadioButtonMenuItem.selectionForeground", c(text))
        UIManager.put("TabbedPane.background", c(bg))
        UIManager.put("TabbedPane.foreground", c(text))
        UIManager.put("TabbedPane.selectedBackground", c(surface))
        UIManager.put("TabbedPane.selectedForeground", c(text))
        UIManager.put("TabbedPane.underlineColor", c(accent))
        UIManager.put("TabbedPane.hoverColor", c(tabHover))
        UIManager.put("TabbedPane.focusColor", c(accent))
        UIManager.put("Separator.foreground", c(border))
        UIManager.put("Component.borderColor", c(border))
        UIManager.put("Label.foreground", c(text))
        if (UIManager.getLookAndFeel() !is FlatMacLightLaf) {
            UIManager.put("Button.foreground", c(text))
            UIManager.put("Button.background", c(surface))
            UIManager.put("Button.focusedBackground", c(tabHover))
            UIManager.put("Button.default.background", c(accent))
            UIManager.put("Button.default.foreground", c(surface))
            val accentHover = 0x2563EB
            val accentPressed = 0x1D4ED8
            UIManager.put("Button.default.focusedBackground", c(accent))
            UIManager.put("Button.default.hoverBackground", c(accentHover))
            UIManager.put("Button.default.pressedBackground", c(accentPressed))
        }
        UIManager.put("TextField.background", c(surface))
        UIManager.put("TextField.foreground", c(text))
        UIManager.put("TextArea.background", c(surface))
        UIManager.put("TextArea.foreground", c(text))
        UIManager.put("TitledBorder.titleColor", c(muted))
        UIManager.put("OptionPane.background", c(bg))
        UIManager.put("OptionPane.messageForeground", c(text))
    }

    @JvmStatic
    fun applyDarkPalette() {
        val bg = 0x1A1A1C
        val surface = 0x2A2A2D
        val toolbar = 0x222224
        val border = 0x48484C
        val text = 0xF4F4F5
        val muted = 0x98989E
        val accent = 0x5B9FFF
        val select = 0x3D3D42
        val tabHover = 0x323236

        UIManager.put(KEY_PALETTE_BACKGROUND, c(toolbar))
        UIManager.put("Panel.background", c(bg))
        UIManager.put("Viewport.background", c(bg))
        UIManager.put("ScrollPane.background", c(bg))
        UIManager.put("SplitPane.background", c(bg))
        UIManager.put("MenuBar.background", c(surface))
        UIManager.put("MenuBar.foreground", c(text))
        UIManager.put("Menu.background", c(surface))
        UIManager.put("Menu.foreground", c(text))
        UIManager.put("PopupMenu.background", c(surface))
        UIManager.put("PopupMenu.foreground", c(text))
        UIManager.put("Menu.selectionBackground", c(select))
        UIManager.put("Menu.selectionForeground", c(text))
        UIManager.put("Menu.acceleratorForeground", c(muted))
        UIManager.put("MenuItem.background", c(surface))
        UIManager.put("MenuItem.foreground", c(text))
        UIManager.put("MenuItem.disabledForeground", c(muted))
        UIManager.put("MenuItem.selectionBackground", c(select))
        UIManager.put("MenuItem.selectionForeground", c(text))
        UIManager.put("CheckBoxMenuItem.background", c(surface))
        UIManager.put("CheckBoxMenuItem.foreground", c(text))
        UIManager.put("CheckBoxMenuItem.disabledForeground", c(muted))
        UIManager.put("CheckBoxMenuItem.selectionBackground", c(select))
        UIManager.put("CheckBoxMenuItem.selectionForeground", c(text))
        UIManager.put("RadioButtonMenuItem.background", c(surface))
        UIManager.put("RadioButtonMenuItem.foreground", c(text))
        UIManager.put("RadioButtonMenuItem.disabledForeground", c(muted))
        UIManager.put("RadioButtonMenuItem.selectionBackground", c(select))
        UIManager.put("RadioButtonMenuItem.selectionForeground", c(text))
        UIManager.put("TabbedPane.background", c(bg))
        UIManager.put("TabbedPane.foreground", c(text))
        UIManager.put("TabbedPane.selectedBackground", c(surface))
        UIManager.put("TabbedPane.selectedForeground", c(text))
        UIManager.put("TabbedPane.underlineColor", c(accent))
        UIManager.put("TabbedPane.hoverColor", c(tabHover))
        UIManager.put("TabbedPane.focusColor", c(accent))
        UIManager.put("Separator.foreground", c(border))
        UIManager.put("Component.borderColor", c(border))
        UIManager.put("Label.foreground", c(text))
        if (UIManager.getLookAndFeel() !is FlatMacDarkLaf) {
            UIManager.put("Button.foreground", c(text))
            UIManager.put("Button.background", c(surface))
            UIManager.put("Button.focusedBackground", c(tabHover))
            UIManager.put("Button.default.background", c(accent))
            UIManager.put("Button.default.foreground", c(0x0A0A0B))
            val accentHover = 0x7AB8FF
            val accentPressed = 0x4A8AE8
            UIManager.put("Button.default.focusedBackground", c(accent))
            UIManager.put("Button.default.hoverBackground", c(accentHover))
            UIManager.put("Button.default.pressedBackground", c(accentPressed))
        }
        UIManager.put("TextField.background", c(0x1E1E21))
        UIManager.put("TextField.foreground", c(text))
        UIManager.put("TextArea.background", c(0x1E1E21))
        UIManager.put("TextArea.foreground", c(text))
        UIManager.put("TitledBorder.titleColor", c(muted))
        UIManager.put("OptionPane.background", c(bg))
        UIManager.put("OptionPane.messageForeground", c(text))
    }
}
