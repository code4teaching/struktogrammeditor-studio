package de.visustruct.control

import de.visustruct.struktogrammelemente.Fallauswahl
import de.visustruct.struktogrammelemente.Schleife
import de.visustruct.struktogrammelemente.StruktogrammElement
import de.visustruct.struktogrammelemente.StruktogrammElementListe
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.JDOMException
import org.jdom2.input.SAXBuilder
import java.awt.Color
import java.awt.Font
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

class XMLLeser {

    private var struktogramm: Struktogramm? = null

    fun ladeXML(pfad: String, struktogramm: Struktogramm) {
        ladeXML(pfad, null, struktogramm)
    }

    fun ladeXML(document: Document, struktogramm: Struktogramm) {
        ladeXML(null, document, struktogramm)
    }

    private fun erstelleElementeRek(elem: Element, tmp: StruktogrammElement) {
        val alleTextzeilen = elem.getChildren("text")
        val textzeilen = Array(alleTextzeilen.size) { i ->
            decodeS(alleTextzeilen[i].text)
        }
        tmp.setzeText(textzeilen)
        when (tmp) {
            is Schleife -> {
                listenelementeErstellen(elem.getChild("schleifeninhalt"), tmp.gibListe())
            }
            is Fallauswahl -> {
                val alleFaelle = elem.getChildren("fall")
                val fallauswahl = tmp
                fallauswahl.erstelleNeueListen(alleFaelle.size)
                for (i in alleFaelle.indices) {
                    fallauswahl.gibListe(i).setzeBeschreibung(decodeS(alleFaelle[i].getAttributeValue("fallname")))
                    listenelementeErstellen(alleFaelle[i], fallauswahl.gibListe(i))
                }
            }
        }
    }

    private fun wurzelStruktogrammElementErstellen(elem: Element): StruktogrammElementListe {
        val liste = StruktogrammElementListe(null)
        listenelementeErstellen(elem, liste)
        return liste
    }

    private fun listenelementeErstellen(elem: Element, liste: StruktogrammElementListe) {
        val alleUnterelemente = elem.getChildren("strelem")
        val str = struktogramm ?: return
        for (i in alleUnterelemente.indices) {
            val tmp = alleUnterelemente[i]
            val typ = tmp.getAttributeValue("typ").toInt()
            val neues = str.neuesStruktogrammElement(typ) ?: continue
            setAttribute(neues, tmp)
            liste.hinzufuegen(neues)
            erstelleElementeRek(tmp, neues)
        }
    }

    private fun setAttribute(struktogrammelement: StruktogrammElement, zugehoerigesKopfelement: Element) {
        var s = zugehoerigesKopfelement.getAttributeValue("zx")
        if (s != null) {
            struktogrammelement.setXVergroesserung(s.toInt())
        }
        s = zugehoerigesKopfelement.getAttributeValue("zy")
        if (s != null) {
            struktogrammelement.setYVergroesserung(s.toInt())
        }
        val tx = zugehoerigesKopfelement.getAttributeValue("textcolor")
        val bgc = zugehoerigesKopfelement.getAttributeValue("bgcolor")
        if (tx != null || bgc != null) {
            val schrift = if (tx != null) Color.decode(tx) else CanvasStyle.getElementText()
            val hg = if (bgc != null) Color.decode(bgc) else CanvasStyle.getElementFill()
            struktogrammelement.setzeFarbenAusXml(schrift, hg)
        }
    }

    private fun ladeXML(pfad: String?, document: Document?, struktogramm: Struktogramm) {
        try {
            val doc = document ?: SAXBuilder().build(File(pfad!!))
            val element = doc.rootElement
            this.struktogramm = struktogramm
            val fontFamily = element.getAttributeValue("fontfamily")
            val fontSize = element.getAttributeValue("fontsize")
            val fontStyle = element.getAttributeValue("fontstyle")
            val struktogrammBeschreibung = element.getAttributeValue("caption")
            if (fontFamily != null && fontSize != null && fontStyle != null) {
                struktogramm.setFontStr(
                    Font(decodeS(fontFamily), fontStyle.toInt(), fontSize.toInt()),
                )
            }
            if (struktogrammBeschreibung != null) {
                struktogramm.setStruktogrammBeschreibung(decodeS(struktogrammBeschreibung))
            }
            struktogramm.gibListe().alleEntfernen()
            listenelementeErstellen(element, struktogramm.gibListe())
            if (struktogramm.gibGraphics() != null) {
                struktogramm.zeichenbereichAktualisieren()
                struktogramm.zeichne()
            }
        } catch (e: JDOMException) {
            LOG.log(Level.SEVERE, "Struktogramm-XML konnte nicht geladen werden", e)
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, "Struktogramm-XML konnte nicht geladen werden", e)
        }
    }

    fun erstelleStruktogrammElementListe(document: Document?, struktogramm: Struktogramm): StruktogrammElementListe? {
        if (document == null) {
            return null
        }
        this.struktogramm = struktogramm
        return wurzelStruktogrammElementErstellen(document.rootElement)
    }

    companion object {
        private val LOG = Logger.getLogger(XMLLeser::class.java.name)

        @JvmStatic
        fun encodeS(s: String): String {
            if (s.isEmpty()) {
                return "-1;"
            }
            val ausgabe = StringBuilder()
            for (ch in s) {
                ausgabe.append(ch.code).append(';')
            }
            return ausgabe.toString()
        }

        private fun decodeS(codiert: String): String {
            val textzeileAlsZahlen = codiert.split(";")
            val sb = StringBuilder()
            for (part in textzeileAlsZahlen) {
                if (part.isEmpty()) {
                    continue
                }
                val zeichenNummer = part.toInt()
                if (zeichenNummer == -1) {
                    return ""
                }
                sb.append(zeichenNummer.toChar())
            }
            return sb.toString()
        }
    }
}
