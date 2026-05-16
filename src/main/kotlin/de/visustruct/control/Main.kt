package de.visustruct.control

import de.visustruct.i18n.I18n
import java.awt.Desktop
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities

object Main {

    /**
     * Pfade aus dem macOS-„Datei öffnen“-Ereignis, die eintreffen bevor [Controlling] fertig ist.
     */
    private val pendingMacOpenPaths = CopyOnWriteArrayList<String>()

    @JvmStatic
    fun main(args: Array<String>) {
        val os = System.getProperty("os.name", "").lowercase()
        val isMac = "mac" in os
        if (isMac) {
            System.setProperty("sun.java2d.metal", "false")
            System.setProperty("apple.laf.useScreenMenuBar", "false")
            System.setProperty("apple.awt.application.name", GlobalSettings.APP_DISPLAY_NAME)
        }

        GlobalSettings.init()
        I18n.syncWithSettings()
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", GlobalSettings.guiTitel)

        val startArgs = args
        val controllingHolder = arrayOfNulls<Controlling>(1)

        SwingUtilities.invokeLater {
            if (isMac && Desktop.isDesktopSupported()) {
                val d = Desktop.getDesktop()
                if (d.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                    d.setOpenFileHandler { e ->
                        for (f in e.files) {
                            if (f == null) continue
                            val path = f.absolutePath
                            SwingUtilities.invokeLater { deliverMacOpenFile(path, controllingHolder) }
                        }
                    }
                }
            }

            controllingHolder[0] = Controlling(startArgs)
            for (p in ArrayList(pendingMacOpenPaths)) {
                controllingHolder[0]!!.oeffneStruktogrammDateiAusFinder(p)
            }
            pendingMacOpenPaths.clear()
        }
    }

    private fun deliverMacOpenFile(path: String, holder: Array<Controlling?>) {
        if (holder[0] != null) {
            holder[0]!!.oeffneStruktogrammDateiAusFinder(path)
        } else {
            pendingMacOpenPaths.add(path)
        }
    }
}
