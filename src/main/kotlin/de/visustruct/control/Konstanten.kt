package de.visustruct.control

/** Indizes für Settings → Theme (Swing-Umsetzung über [javax.swing.UIManager]). */
interface Konstanten {
    companion object {
        const val lookAndFeelOSStandard = 0
        const val lookAndFeelSwingStandard = 1
        const val lookAndFeelNimbus = 2
        const val lookAndFeelFlatLight = 4
        const val lookAndFeelFlatDark = 5
    }
}
