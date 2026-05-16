package de.visustruct.control

import java.awt.Desktop
import java.awt.Taskbar
import java.awt.desktop.AboutEvent
import java.awt.desktop.AboutHandler
import java.awt.desktop.QuitEvent
import java.awt.desktop.QuitHandler
import java.awt.desktop.QuitResponse
import javax.swing.ImageIcon

class MacHandler(private val controlling: Controlling) : AboutHandler, QuitHandler {

    init {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", GlobalSettings.guiTitel)

        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(this)
            }
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitHandler(this)
            }
        }

        if (Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.iconImage = ImageIcon(
                    javaClass.getResource(GlobalSettings.logoName),
                ).image
            }
        }
    }

    override fun handleQuitRequestWith(quitEvent: QuitEvent, response: QuitResponse) {
        if (!controlling.programmBeendenGeklickt()) {
            response.cancelQuit()
        }
    }

    override fun handleAbout(event: AboutEvent) {
        controlling.showInfo()
    }
}
