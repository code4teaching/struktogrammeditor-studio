package de.visustruct.other

import java.awt.Container
import java.awt.Font
import javax.swing.JScrollPane
import javax.swing.JTextArea

class JTextAreaEasy : JTextArea {

    private var scrollpane: JScrollPane? = null
    private var quellcodeBatch: StringBuilder? = null

    constructor() : super() {
        lineWrap = false
        wrapStyleWord = false
    }

    constructor(xpos: Int, ypos: Int, breite: Int, hoehe: Int) : super() {
        lineWrap = false
        wrapStyleWord = false
        setBounds(xpos, ypos, breite, hoehe)
    }

    fun setzeContainer(cp: Container) {
        scrollpane = JScrollPane(this).also {
            it.bounds = bounds
            cp.add(it)
        }
    }

    fun hinzufuegen(s: String) {
        val batch = quellcodeBatch
        if (batch != null) {
            batch.append(s)
        } else {
            append(s)
        }
    }

    fun beginQuellcodeBatch() {
        quellcodeBatch = StringBuilder(16384)
    }

    fun endQuellcodeBatch() {
        quellcodeBatch?.let {
            text = it.toString()
            quellcodeBatch = null
        }
    }

    fun einfuegenAnStelle(s: String, stelle: Int) {
        insert(s, stelle)
    }

    fun zeilenumbruch() {
        hinzufuegen("\n")
    }

    fun zeilenumbruchAnStelle(stelle: Int) {
        einfuegenAnStelle("\n", stelle)
    }

    fun leeren() {
        quellcodeBatch = null
        text = ""
    }

    fun setzeNurLesen(nurLesen: Boolean) {
        isEditable = !nurLesen
    }

    fun gibText(): String = text

    fun setzeText(text: String) {
        this.text = text
    }

    fun gibTextzeilenArray(): Array<String> = text.split("\n").toTypedArray()

    fun setzeFont(font: Font) {
        this.font = font
    }

    companion object {
        private const val serialVersionUID = -4009874987394271922L
    }
}
