package de.visustruct.other

import java.awt.Container
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JScrollPane

class JListEasy : JList<String> {

    private val model: DefaultListModel<String>
    private var scrollpane: JScrollPane? = null

    constructor() : super(DefaultListModel()) {
        @Suppress("UNCHECKED_CAST")
        model = super.getModel() as DefaultListModel<String>
    }

    constructor(xpos: Int, ypos: Int, breite: Int, hoehe: Int) : this() {
        setBounds(xpos, ypos, breite, hoehe)
    }

    fun setzeContainer(cp: Container) {
        scrollpane = JScrollPane(this).also {
            it.bounds = bounds
            cp.add(it)
        }
    }

    fun gibIndex(): Int = selectedIndex

    fun setzeIndex(neuerIndex: Int) {
        selectedIndex = neuerIndex
    }

    fun setzeText(s: String, index: Int) {
        if (index in 0 until gibAnzahl()) {
            model[index] = s
        }
    }

    fun gibMarkiertenInhalt(): String =
        if (gibIndex() >= 0) "${model.getElementAt(gibIndex())}" else ""

    fun gibInhalt(indexDesInhalt: Int): String =
        if (indexIstVorhanden(indexDesInhalt)) "${model.getElementAt(indexDesInhalt)}" else ""

    fun fuegeHinzu(s: String) {
        model.add(gibAnzahl(), s)
    }

    fun fuegeHinzuAmAnfang(s: String) {
        model.add(0, s)
    }

    fun fuegeHinzuAnStelle(s: String, stelle: Int) {
        model.add(minOf(stelle, gibAnzahl()), s)
    }

    fun entferne(indexDesEintrages: Int) {
        if (indexIstVorhanden(indexDesEintrages)) {
            model.remove(indexDesEintrages)
        }
    }

    fun entferneLetztenEintrag() {
        entferne(gibAnzahl() - 1)
    }

    fun entferneAlle() {
        for (i in gibAnzahl() - 1 downTo 0) {
            entferne(0)
        }
    }

    private fun indexIstVorhanden(indexDesEintrages: Int): Boolean =
        indexDesEintrages in 0 until gibAnzahl()

    fun gibAnzahl(): Int = model.size

    companion object {
        private const val serialVersionUID = -5302981897521724160L
    }
}
