package de.visustruct.view

import de.visustruct.control.Struktogramm
import de.visustruct.i18n.I18n
import de.visustruct.struktogrammelemente.Fallauswahl
import de.visustruct.struktogrammelemente.StruktogrammElement
import de.visustruct.struktogrammelemente.Verzweigung
import org.jdom2.Document
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

// http://download.oracle.com/javase/tutorial/uiswing/components/menu.html
// http://download.oracle.com/javase/tutorial/uiswing/examples/components/MenuDemoProject/src/components/MenuDemo.java

class StruktogrammPopup(
    private val element: StruktogrammElement,
    private val struktogramm: Struktogramm,
    private val popupKlickX: Int,
    private val popupKlickY: Int,
) : JPopupMenu(), PopupMenuListener {

    init {
        addPopupMenuListener(this)

        val typ = Struktogramm.strElementZuTypnummer(element)

        // Platzhalter „ø“: nur Einfügen — kein Bearbeiten/Kopieren/Löschen/Zoom für ein LeerElement.
        if (typ == Struktogramm.typLeerElement) {
            einfuegen(I18n.tr("popup.paste"), 14)
        } else {
            var untermenue = unterMenueEinfuegen(I18n.tr("popup.zoom"))
            untermenue.einfuegen(I18n.tr("popup.zoom.larger"), 8, -1)
            untermenue.einfuegen(I18n.tr("popup.zoom.smaller"), 9, -1)
            untermenue.add(JSeparator())
            untermenue.einfuegen(I18n.tr("popup.zoom.wider"), 10, -1)
            untermenue.einfuegen(I18n.tr("popup.zoom.narrower"), 11, -1)
            untermenue.add(JSeparator())
            untermenue.einfuegen(I18n.tr("popup.zoom.taller"), 12, -1)
            untermenue.einfuegen(I18n.tr("popup.zoom.shorter"), 13, -1)
            add(JSeparator())

            einfuegen(I18n.tr("popup.editText"), 0)
            einfuegen(I18n.tr("popup.copy"), 7)
            einfuegen(I18n.tr("popup.paste"), 14)
            einfuegen(I18n.tr("popup.delete"), 1)

            when (typ) {
                Struktogramm.typVerzweigung -> {
                    add(JSeparator())
                    einfuegen(I18n.tr("popup.swapBranches"), 2)
                }

                Struktogramm.typFallauswahl -> {
                    add(JSeparator())

                    einfuegen(I18n.tr("popup.insertNewCase"), 3)

                    val faelle = element.gibFaelle()

                    for (i in faelle.indices) {
                        untermenue = unterMenueEinfuegen(I18n.trf("popup.caseSubmenu", faelle[i]))

                        if (i > 0) {
                            untermenue.einfuegen(I18n.trf("popup.moveCaseLeft", faelle[i]), 4, i)
                        }

                        if (i < faelle.size - 1) {
                            untermenue.einfuegen(I18n.trf("popup.moveCaseRight", faelle[i]), 5, i)
                            untermenue.einfuegen(I18n.trf("popup.removeCase", faelle[i]), 6, i)
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    private fun einfuegen(text: String, id: Int) {
        add(StrPopupItem(text, id))
    }

    private fun unterMenueEinfuegen(text: String): StrPopupUntermenue {
        val tmp = StrPopupUntermenue(element, struktogramm, text)
        add(tmp)
        return tmp
    }

    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        struktogramm.setzePopupmenuSichtbar(true)
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        struktogramm.setzePopupmenuSichtbar(false)
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
        struktogramm.setzePopupmenuSichtbar(false)
    }

    private inner class StrPopupUntermenue(
        @Suppress("UNUSED_PARAMETER") element: StruktogrammElement,
        @Suppress("UNUSED_PARAMETER") struktogramm: Struktogramm,
        text: String,
    ) : JMenu(text) {

        fun einfuegen(text: String, id: Int, fallnummer: Int) {
            add(StrPopupItem(text, id, fallnummer))
        }
    }

    private inner class StrPopupItem : JMenuItem, ActionListener {

        private val id: Int
        private val fallnummer: Int

        constructor(text: String, id: Int) : this(text, id, -1)

        constructor(text: String, id: Int, fallnummer: Int) : super(text) {
            this.id = id
            this.fallnummer = fallnummer
            addActionListener(this)
        }

        override fun actionPerformed(e: ActionEvent) {
            var mussSpeicherpunktSetzen = true

            when (id) {
                0 -> {
                    struktogramm.elementBefuellen(element)
                    mussSpeicherpunktSetzen = false
                }

                1 -> {
                    struktogramm.elementLoeschen(element, true)
                    mussSpeicherpunktSetzen = false
                }

                2 -> (element as Verzweigung).seitenVertauschen()

                3 -> (element as Fallauswahl).erstelleNeueSpalte()

                4 -> (element as Fallauswahl).spalteVerschieben(true, fallnummer)

                5 -> (element as Fallauswahl).spalteVerschieben(false, fallnummer)

                6 -> {
                    val fallname = (element as Fallauswahl).gibFaelle()[fallnummer]
                    val entfOpts = arrayOf(I18n.tr("dialog.common.yes"), I18n.tr("dialog.common.no"))
                    if (
                        JOptionPane.showOptionDialog(
                            struktogramm,
                            I18n.trf("popup.removeCaseConfirm", fallname),
                            I18n.tr("popup.removeCaseTitle"),
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            entfOpts,
                            entfOpts[1],
                        ) == 0
                    ) {
                        (element as Fallauswahl).entferneSpalte(fallnummer)
                    } else {
                        mussSpeicherpunktSetzen = false
                    }
                }

                7 -> {
                    struktogramm.gibTabbedPane()!!.gibGUI().gibAuswahlPanel()
                        .setzeKopiertesStrElement(struktogramm.xmlErstellen(element))
                    mussSpeicherpunktSetzen = false
                }

                14 -> {
                    val kopie: Document? =
                        struktogramm.gibTabbedPane()!!.gibGUI().gibAuswahlPanel().gibKopiertesStrElement()
                    if (kopie == null) {
                        JOptionPane.showMessageDialog(
                            struktogramm,
                            I18n.tr("popup.pasteEmpty"),
                            I18n.tr("popup.pasteTitle"),
                            JOptionPane.INFORMATION_MESSAGE,
                        )
                        mussSpeicherpunktSetzen = false
                    } else {
                        struktogramm.elementAusKopierFeldEinfuegenAnKoordinaten(popupKlickX, popupKlickY)
                        mussSpeicherpunktSetzen = false
                    }
                }

                8 -> struktogramm.zoom(1, 1, element)
                9 -> struktogramm.zoom(-1, -1, element)
                10 -> struktogramm.zoom(1, 0, element)
                11 -> struktogramm.zoom(-1, 0, element)
                12 -> struktogramm.zoom(0, 1, element)
                13 -> struktogramm.zoom(0, -1, element)
                else -> Unit
            }

            struktogramm.zeichenbereichAktualisieren()
            struktogramm.zeichne()

            if (mussSpeicherpunktSetzen) {
                struktogramm.rueckgaengigPunktSetzen()
            }
        }
    }

    companion object {
        private const val serialVersionUID = -6394669590636692950L
    }
}
