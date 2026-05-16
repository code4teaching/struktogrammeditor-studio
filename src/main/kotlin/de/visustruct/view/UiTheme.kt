package de.visustruct.view

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import de.visustruct.control.CanvasStyle
import java.awt.Font
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource
import javax.swing.plaf.InsetsUIResource

/** Globale UI-Defaults nach Theme-Wechsel; eigene Paletten für „Modern · hell“ und „Modern · dunkel“. */
object UiTheme {

    @JvmStatic
    fun applyAfterTheme() {
        UIManager.put("ScrollPane.smoothScrolling", true)

        if (UIManager.getLookAndFeel() is FlatLaf) {
            val os = System.getProperty("os.name", "").lowercase()
            val uiFont =
                if ("win" in os) Font("Segoe UI", Font.PLAIN, 13)
                else Font(Font.SANS_SERIF, Font.PLAIN, 13)
            UIManager.put("defaultFont", FontUIResource(uiFont))

            when (UIManager.getLookAndFeel()) {
                is FlatLightLaf -> VisuStructTheme.applyLightPalette()
                is FlatDarkLaf, is FlatMacDarkLaf -> VisuStructTheme.applyDarkPalette()
            }

            UIManager.put("Component.arc", 8)
            UIManager.put("Button.arc", 10)
            UIManager.put("TextComponent.arc", 8)
            UIManager.put("ScrollBar.width", 12)
            UIManager.put("ScrollBar.thumbArc", 999)
            UIManager.put("TabbedPane.tabHeight", 34)
            UIManager.put("TabbedPane.tabsOverlap", 0)
            UIManager.put("TabbedPane.tabInsets", InsetsUIResource(6, 14, 6, 14))
            UIManager.put("TabbedPane.showTabSeparators", true)
            UIManager.put("MenuItem.margin", InsetsUIResource(4, 10, 4, 10))
            UIManager.put("MenuBar.horizontalGripEnabled", false)
        } else {
            UIManager.put("Component.arc", 6)
            UIManager.put("Button.arc", 8)
            UIManager.put("TextComponent.arc", 8)
            UIManager.put("ScrollBar.width", 10)
            UIManager.put("TabbedPane.tabHeight", 28)
            UIManager.put("TabbedPane.tabsOverlap", 0)
        }

        CanvasStyle.syncToTheme()
    }
}
