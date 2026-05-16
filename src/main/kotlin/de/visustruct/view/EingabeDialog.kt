package de.visustruct.view

import de.visustruct.i18n.I18n
import de.visustruct.other.JListEasy
import de.visustruct.other.JTextAreaEasy
import de.visustruct.struktogrammelemente.Fallauswahl
import de.visustruct.struktogrammelemente.StruktogrammElement
import de.visustruct.struktogrammelemente.Verzweigung
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.WindowConstants

class EingabeDialog(
    owner: JFrame?,
    title: String?,
    modal: Boolean,
    private val element: StruktogrammElement,
) : JDialog(owner, title, modal) {

    private lateinit var textarea: JTextAreaEasy
    private var list: JListEasy? = null
    private lateinit var rueckgabeInhalt: Array<String>
    private var okWurdeGedrueckt = false
    private var schriftfarbeNeu: Color
    private var hintergrundfarbeNeu: Color
    private val initialSchriftfarbe: Color
    private val initialHintergrundfarbe: Color
    private val initialFarbenExplizit: Boolean
    private val buttonSchriftfarbe: JButton
    private val buttonHintergrundfarbe: JButton

    init {
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        initialSchriftfarbe = element.getFarbeSchrift()
        initialHintergrundfarbe = element.getFarbeHintergrund()
        initialFarbenExplizit = element.sindElementfarbenExplizit()
        schriftfarbeNeu = initialSchriftfarbe
        hintergrundfarbeNeu = initialHintergrundfarbe

        layout = GridBagLayout()

        var c = GridBagConstraints()
        c.gridwidth = 1
        c.gridheight = 1
        c.weightx = 1.0
        c.weighty = 1.0
        c.ipadx = 1
        c.ipady = 1
        c.anchor = GridBagConstraints.WEST
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = Insets(0, 0, 4, 0)

        var button: JButton

        c.gridwidth = 3
        c.gridx = 0
        c.gridy = 0
        c.weightx = 1.0
        add(JLabel(I18n.tr("dialog.elementEdit.headerText")), c)

        c.gridx = 0
        c.gridy = 1
        c.weightx = 100.0
        c.weighty = 150.0
        c.fill = GridBagConstraints.BOTH
        textarea = JTextAreaEasy()
        add(JScrollPane(textarea), c)

        c.weighty = 1.0
        c.fill = GridBagConstraints.HORIZONTAL

        val vorherigerInhalt = element.gibText()
        for (i in vorherigerInhalt.indices) {
            textarea.hinzufuegen(vorherigerInhalt[i])
            if (i != vorherigerInhalt.size - 1) {
                textarea.zeilenumbruch()
            }
        }

        rueckgabeInhalt = vorherigerInhalt

        val anzahlListen = element.gibAnzahlListen()
        if (anzahlListen > 0) {
            c.gridx = 0
            c.gridy = 2
            c.weightx = 1.0
            val label =
                if (element is Verzweigung) {
                    JLabel(I18n.tr("dialog.elementEdit.branchLabels"))
                } else {
                    JLabel(I18n.tr("dialog.elementEdit.caseLabels"))
                }
            add(label, c)

            c.gridx = 0
            c.gridy = 3
            c.weightx = 100.0
            c.weighty = 60.0
            c.fill = GridBagConstraints.BOTH
            val listEasy = JListEasy()
            list = listEasy
            add(JScrollPane(listEasy), c)

            c.weighty = 1.0
            c.fill = GridBagConstraints.HORIZONTAL

            c.gridwidth = 1
            c.fill = GridBagConstraints.NONE
            c.gridx = 2
            c.gridy = 4
            c.weightx = 100.0
            val caseButtons = JPanel()
            caseButtons.layout = BoxLayout(caseButtons, BoxLayout.Y_AXIS)
            val rename = JButton(I18n.tr("dialog.caseLabel.renameButton"))
            rename.addActionListener { buttonFallname_ActionPerformed(it) }
            caseButtons.add(rename)

            if (element is Fallauswahl && element !is Verzweigung) {
                val insert = JButton(I18n.tr("popup.insertNewCase"))
                insert.addActionListener { buttonNeuerFall_ActionPerformed(it) }
                caseButtons.add(insert)
            }
            add(caseButtons, c)

            refreshCaseList()
        }

        c.gridwidth = 1
        c.fill = GridBagConstraints.NONE
        c.gridx = 0
        c.gridy = 4
        c.weightx = 1.0
        button = JButton(I18n.tr("dialog.elementEdit.textColor"))
        buttonSchriftfarbe = button
        button.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                buttonSchriftfarbeGeklickt()
            }
        })
        add(button, c)

        c.gridx = 1
        c.gridy = 4
        c.weightx = 100.0
        button = JButton(I18n.tr("dialog.elementEdit.fillColor"))
        buttonHintergrundfarbe = button
        button.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                buttonHintergrundfarbeGeklickt()
            }
        })
        add(button, c)

        c.gridx = 0
        c.gridy = 5
        c.weightx = 1.0
        c.fill = GridBagConstraints.HORIZONTAL
        button = JButton(I18n.tr("dialog.common.ok"))
        button.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                buttonOK_ActionPerformed(e!!)
            }
        })
        add(button, c)

        c.fill = GridBagConstraints.NONE
        c.anchor = GridBagConstraints.EAST
        c.gridx = 2
        c.gridy = 5
        c.weightx = 100.0
        button = JButton(I18n.tr("dialog.common.cancel"))
        button.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                buttonAbbrechen_ActionPerformed(e!!)
            }
        })
        add(button, c)

        aktualisiereButtonfarben()

        setSize(400, 500)
        setLocationRelativeTo(owner)
        isVisible = true
    }

    fun buttonOK_ActionPerformed(evt: ActionEvent) {
        if (element is Fallauswahl) {
            val lis = list!!
            val fallBezeichnungen = Array(lis.gibAnzahl()) { i -> lis.gibInhalt(i) }
            element.setzeFaelle(fallBezeichnungen)
        }

        val farbenGeaendert =
            schriftfarbeNeu.rgb != initialSchriftfarbe.rgb ||
                hintergrundfarbeNeu.rgb != initialHintergrundfarbe.rgb
        if (initialFarbenExplizit || farbenGeaendert) {
            element.setFarbeSchrift(schriftfarbeNeu)
            element.setFarbeHintergrund(hintergrundfarbeNeu)
        }

        rueckgabeInhalt = textarea.gibTextzeilenArray()
        okWurdeGedrueckt = true
        isVisible = false
    }

    fun buttonAbbrechen_ActionPerformed(evt: ActionEvent) {
        isVisible = false
    }

    fun buttonFallname_ActionPerformed(evt: ActionEvent) {
        val lis = list!!
        if (lis.gibIndex() >= 0) {
            var fallname =
                JOptionPane.showInputDialog(
                    this,
                    I18n.tr("dialog.caseLabel.newMessage"),
                    I18n.tr("dialog.caseLabel.newTitle"),
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    lis.gibMarkiertenInhalt()
                ) as String?

            if (fallname != null) {
                if (fallname == "") {
                    fallname = " "
                }
                lis.setzeText(fallname, lis.gibIndex())
            }
        } else {
            JOptionPane.showMessageDialog(
                this,
                I18n.tr("dialog.caseSelectFirst.message"),
                I18n.tr("dialog.caseSelectFirst.title"),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    fun gibTextArray(): Array<String>? = if (okWurdeGedrueckt) rueckgabeInhalt else null

    fun getSchriftfarbeNeu(): Color = schriftfarbeNeu

    fun getHintergrundfarbeNeu(): Color = hintergrundfarbeNeu

    private fun buttonSchriftfarbeGeklickt() {
        schriftfarbeNeu = ColorDialog.showColorChooser(this, schriftfarbeNeu) ?: schriftfarbeNeu
        aktualisiereButtonfarben()
    }

    private fun buttonHintergrundfarbeGeklickt() {
        hintergrundfarbeNeu = ColorDialog.showColorChooser(this, hintergrundfarbeNeu) ?: hintergrundfarbeNeu
        aktualisiereButtonfarben()
    }

    private fun buttonNeuerFall_ActionPerformed(evt: ActionEvent) {
        if (element !is Fallauswahl || element is Verzweigung) {
            return
        }
        element.erstelleNeueSpalte()
        refreshCaseList()
        val lis = list
        if (lis != null && lis.gibAnzahl() >= 2) {
            lis.setzeIndex(lis.gibAnzahl() - 2)
        }
    }

    private fun refreshCaseList() {
        val lis = list ?: return
        lis.entferneAlle()
        val inhalt = element.gibFaelle()
        for (s in inhalt) {
            lis.fuegeHinzu(s)
        }
    }

    private fun aktualisiereButtonfarben() {
        buttonSchriftfarbe.foreground = schriftfarbeNeu
        buttonHintergrundfarbe.background = hintergrundfarbeNeu
    }

    private companion object {
        private const val serialVersionUID: Long = -7385908673937166978L
    }
}
