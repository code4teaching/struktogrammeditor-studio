package de.visustruct.struktogrammelemente

import de.visustruct.control.CanvasStyle
import de.visustruct.control.DiagramKeywordText
import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.control.XMLLeser
import de.visustruct.i18n.I18n
import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import org.jdom2.Element
import java.awt.Graphics2D
import java.awt.Rectangle
import java.util.ArrayList

open class Fallauswahl @JvmOverloads constructor(
    g: Graphics2D?,
    anzahlListen: Int = 3,
) : StruktogrammElement(g) {

    protected var xVerschiebungFuerTrennlinie: Int = 0
    protected var yVerschiebungFuerTrennLinie: Int = 0

    @JvmField
    protected val listen = ArrayList<StruktogrammElementListe>()

    init {
        erstelleNeueListen(anzahlListen)
        yVerschiebungFuerTrennLinie = -20
        listen[listen.size - 1].setzeBeschreibung(I18n.tr("structure.multiway.defaultCaseLabel"))
        obererRandZusatz = 40
        setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typFallauswahl))
    }

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        val (vorher, nachher) = if (typ == CodeErzeuger.typPython) {
            quellcodeMitKommentarVorspann("match ", ":\n", typ, anzahlEingerueckt, alsKommentar) to ""
        } else {
            quellcodeMitKommentarVorspann("switch(", "){\n", typ, anzahlEingerueckt, alsKommentar) to "}\n"
        }

        textarea.hinzufuegen(wandleZuAusgabe(vorher, typ, anzahlEingerueckt, alsKommentar))

        for (i in listen.indices) {
            val fall: String
            val fallEnde: String
            if (typ == CodeErzeuger.typPython) {
                fall = if (i < listen.size - 1) {
                    "case " + CodeGenRules.caseLabelToken(listen[i].gibBeschreibung(), typ) + ":\n"
                } else {
                    "case _:" + CodeGenRules.forcedCaseComment(listen[i].gibBeschreibung(), typ) + "\n"
                }
                fallEnde = ""
            } else {
                fall = if (i < listen.size - 1) {
                    "case " + CodeGenRules.caseLabelToken(listen[i].gibBeschreibung(), typ) + ":\n"
                } else {
                    "default: " + CodeGenRules.forcedCaseComment(listen[i].gibBeschreibung(), typ) + "\n"
                }
                fallEnde = einruecken("break;\n", anzahlEinzuruecken)
            }

            textarea.hinzufuegen(
                wandleZuAusgabe(fall, typ, anzahlEingerueckt + anzahlEinzuruecken, alsKommentar),
            )
            listen[i].quellcodeAllerUnterelementeGenerieren(
                typ,
                anzahlEingerueckt + anzahlEinzuruecken * 2,
                anzahlEinzuruecken,
                alsKommentar,
                textarea,
            )
            if (fallEnde.isNotEmpty()) {
                textarea.hinzufuegen(
                    wandleZuAusgabe(fallEnde, typ, anzahlEingerueckt + anzahlEinzuruecken, alsKommentar),
                )
            }
        }

        if (nachher.isNotEmpty()) {
            textarea.hinzufuegen(wandleZuAusgabe(nachher, typ, anzahlEingerueckt, alsKommentar))
        }
    }

    fun erstelleNeueListen(anzahlListen: Int) {
        listen.clear()
        for (i in 0 until anzahlListen) {
            listen.add(StruktogrammElementListe(g))
            listen[i].setzeBeschreibung("${i + 1}")
        }
    }

    open fun erstelleNeueSpalte() {
        val listennummer = listen.size - 1
        listen.add(listennummer, StruktogrammElementListe(g))
        listen[listennummer].setzeBeschreibung("${listennummer + 1}")
    }

    fun spalteVerschieben(nachLinks: Boolean, spaltenIndex: Int) {
        if (nachLinks) {
            if (spaltenIndex > 0) {
                listenTauschen(spaltenIndex, spaltenIndex - 1)
            }
        } else {
            if (spaltenIndex <= listen.size - 2) {
                listenTauschen(spaltenIndex, spaltenIndex + 1)
            }
        }
    }

    protected fun listenTauschen(index1: Int, index2: Int) {
        val tmp = listen[index1]
        listen[index1] = listen[index2]
        listen[index2] = tmp
    }

    fun entferneSpalte(index: Int) {
        if (index >= 0 && index < listen.size && listen.size > 2) {
            listen.removeAt(index)
        }
    }

    override fun zusaetzlicheXMLDatenSchreiben(aktuelles: Element) {
        for (i in listen.indices) {
            val unterelement = Element("fall")
                .setAttribute("fallname", XMLLeser.encodeS(listen[i].gibBeschreibung()))
            listen[i].schreibeXMLDatenAllerUnterElemente(unterelement)
            aktuelles.addContent(unterelement)
        }
    }

    override fun istUnterelement(eventuellesUnterelement: StruktogrammElement): Boolean {
        for (i in listen.indices) {
            if (listen[i].istUnterelement(eventuellesUnterelement)) {
                return true
            }
        }
        return false
    }

    override fun gibFaelle(): Array<String> =
        Array(listen.size) { listen[it].gibBeschreibung() }

    override fun setzeFaelle(faelle: Array<String>) {
        for (i in listen.indices) {
            listen[i].setzeBeschreibung(faelle[i])
        }
    }

    override fun gibAnzahlListen(): Int = listen.size

    fun gibListe(index: Int): StruktogrammElementListe = listen[index]

    override fun neuesElementMussOberhalbPlatziertWerden(y: Int): Boolean =
        y < gibY() + getObererRand() / 2

    override fun setzeGraphics(g: Graphics2D?) {
        super.setzeGraphics(g)
        for (i in listen.indices) {
            g?.let { listen[i].graphicsAllerUnterlementeSetzen(it) }
        }
    }

    override fun setzeBreite(neueBreite: Int) {
        var gesamtbreiteDerListen = 0
        var neueSpaltenbreite: Int

        for (i in listen.indices) {
            listen[i].xPosAllerUnterelementeSetzen(gibX() + gesamtbreiteDerListen)

            if (i <= listen.size - 2) {
                neueSpaltenbreite = neueBreite * listen[i].gibBreite() / gibBreite()
                listen[i].breiteDerUnterelementeSetzen(neueSpaltenbreite)
            } else {
                neueSpaltenbreite = neueBreite - gesamtbreiteDerListen
                listen[i].breiteDerUnterelementeSetzen(neueSpaltenbreite)
                xVerschiebungFuerTrennlinie = neueBreite - neueSpaltenbreite
            }

            gesamtbreiteDerListen += neueSpaltenbreite
        }

        bereich.width = neueBreite
    }

    override fun setzeHoehe(neueHoehe: Int) {
        for (i in listen.indices) {
            listen[i].gesamtHoeheSetzen(neueHoehe - getObererRand())
        }
        bereich.height = neueHoehe
    }

    override fun gibElementAnPos(x: Int, y: Int, nurListe: Boolean): Any? {
        if (!bereich.contains(x, y)) {
            return null
        }

        for (i in listen.indices) {
            val tmp = listen[i].gibElementAnPos(x, y, nurListe)
            if (objGesetzt(tmp)) {
                return tmp
            }
        }

        return if (!nurListe) this else null
    }

    override fun gibListeDieDasElementHat(element: StruktogrammElement): StruktogrammElementListe? {
        for (i in listen.indices) {
            val tmp = listen[i].gibListeDieDasElementHat(element)
            if (tmp != null) {
                return tmp
            }
        }
        return null
    }

    private fun gibPassendeYKoordFuerLinie(x: Int): Int {
        val m = (gibY() - (gibY() + getObererRand() + yVerschiebungFuerTrennLinie)).toDouble() /
            (gibX() - (gibX() + xVerschiebungFuerTrennlinie)).toDouble()
        val b = gibY() - m * gibX()
        return (m * x + b).toInt()
    }

    override fun zeichne() {
        eigenenBereichZeichnen()

        for (i in listen.indices) {
            if (!listen[i].isEmpty()) {
                listen[i].alleZeichnen()
            }
        }

        g!!.setColor(CanvasStyle.getElementBorder())
        g!!.drawLine(
            gibX(),
            gibY(),
            gibX() + xVerschiebungFuerTrennlinie,
            gibY() + getObererRand() + yVerschiebungFuerTrennLinie,
        )
        g!!.drawLine(
            gibX() + xVerschiebungFuerTrennlinie,
            gibY() + getObererRand() + yVerschiebungFuerTrennLinie,
            gibX() + gibBreite(),
            gibY(),
        )

        for (i in listen.indices) {
            val tmp = listen[i]
            var x = tmp.gibRechterRand()

            if (i != listen.size - 1) {
                g!!.setColor(CanvasStyle.getElementBorder())
                g!!.drawLine(x, gibY() + gibHoehe(), x, gibPassendeYKoordFuerLinie(x))
            }

            x = if (this is Verzweigung) {
                if (i == 0) {
                    gibX() + 5
                } else {
                    tmp.gibRechterRand() - 5 - DiagramKeywordText.measureLineWidth(g, tmp.gibBeschreibung())
                }
            } else {
                val colW = tmp.gibRechterRand() - tmp.gibX()
                val lab = tmp.gibBeschreibung()
                tmp.gibX() + ((colW - DiagramKeywordText.measureLineWidth(g, lab)) / 2)
            }

            DiagramKeywordText.drawKeywordAwareLine(
                g,
                getFarbeSchrift(),
                x,
                gibY() + getObererRand() - 5,
                tmp.gibBeschreibung(),
            )
        }

        textZeichnen()
    }

    override fun zeichenbereichAktualisieren(x: Int, y: Int): Rectangle {
        var gesamtbreiteDerListen = 0
        var groessteHoeheDerListen = 0

        for (i in listen.indices) {
            val rectListe = listen[i].zeichenbereichAllerElementeAktualisieren(
                x + gesamtbreiteDerListen,
                y + getObererRand(),
            )
            gesamtbreiteDerListen += rectListe.width

            if (rectListe.height > groessteHoeheDerListen) {
                groessteHoeheDerListen = rectListe.height
            }
        }

        if (this is Verzweigung && listen.size == 2) {
            gesamtbreiteDerListen = verzweigungJaNeinMindestbreiten(x, gesamtbreiteDerListen)
        }

        xVerschiebungFuerTrennlinie =
            gesamtbreiteDerListen - listen[listen.size - 1].gibBreite()

        if (gesamtbreiteDerListen < gibMindestbreite()) {
            gesamtbreiteDerListen = gibMindestbreite()
        }

        if (GlobalSettings.gibLetzteElementeStrecken()) {
            setzeHoehe(getObererRand() + groessteHoeheDerListen)
        }

        bereich.setBounds(x, y, gesamtbreiteDerListen, getObererRand() + groessteHoeheDerListen)
        return bereich
    }

    private fun verzweigungJaNeinMindestbreiten(fallAuswahlX: Int, gesamtBreite: Int): Int {
        val links = listen[0]
        val rechts = listen[listen.size - 1]
        var wLinks = links.gibBreite()
        val wRechts = rechts.gibBreite()
        val randLinks = 28
        val randRechts = 20
        val minLinks = links.breiteFuerBeschriftungMitRand(randLinks)
        val minRechts = rechts.breiteFuerBeschriftungMitRand(randRechts)
        if (wLinks >= minLinks) {
            return gesamtBreite
        }
        val fehlend = minLinks - wLinks
        var wRechtsNeu = wRechts - fehlend
        if (wRechtsNeu < minRechts) {
            wLinks = minLinks
            wRechtsNeu = minRechts
        } else {
            wLinks = minLinks
        }
        links.breiteDerUnterelementeSetzen(wLinks)
        rechts.breiteDerUnterelementeSetzen(wRechtsNeu)
        links.xPosAllerUnterelementeSetzen(fallAuswahlX)
        rechts.xPosAllerUnterelementeSetzen(fallAuswahlX + wLinks)
        return wLinks + wRechtsNeu
    }

    override fun setzeXPos(x: Int) {
        bereich.x = x
        var xVerschiebung = 0
        for (i in listen.indices) {
            listen[i].xPosAllerUnterelementeSetzen(x + xVerschiebung)
            xVerschiebung += listen[i].gibBreite()
        }
    }

    override fun zoomsZuruecksetzen() {
        super.zoomsZuruecksetzen()
        for (i in listen.indices) {
            listen[i].zoomsAllerElementeZuruecksetzen()
        }
    }

    override fun getObererRand(): Int = super.getObererRand() + getYVergroesserung()
}
