package de.visustruct.struktogrammelemente;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;

import org.jdom2.Element;

import de.visustruct.control.CanvasStyle;
import de.visustruct.control.DiagramKeywordText;
import de.visustruct.control.Struktogramm;
import de.visustruct.control.XMLLeser;
import de.visustruct.other.JTextAreaEasy;
import de.visustruct.view.CodeErzeuger;

public abstract class StruktogrammElement { //abstrakte Klasse -> keine Objekte davon erzeugbar
	protected String[] text; //Textzeilen, die im Kopfbereich des jeweiligen StruktogrammElementes angzeigt werden
	protected Rectangle bereich; //Koordinaten, Breite und Hoehe des jeweiligen StruktogrammElemente
	protected boolean markiert; // aktive Kachel: wie SwiftUI blauer Rand (ohne Blaufüllung)
	/** Simulationsmodus: Schritt-Hervorhebung im Diagramm (unabhängig von {@link #markiert}). */
	protected boolean simulationSpotlight;
	protected Graphics2D g; //Graphics-Kontext des BufferedImage bild des Struktogramms
	protected static final int vorschauHoehe = 20; //Höhe des roten Vorschau-Rechteckes
	private int obererRand; //verändert sich je nach Anzahl der Textzeilen
	protected int obererRandZusatz; //wird pro von StruktogrammElement abgeleitete Klasse einmal gesetzt; beschreibt zusätzliche Pixelzahl zum für den oberen Rand
	private int xVergroesserung, yVergroesserung;
	/** Wenn false, kommen Schrift-/Hintergrundfarbe beim Zeichnen aus {@link CanvasStyle} (folgt Theme-Wechsel). */
	private boolean elementfarbenExplizit;
	private Color farbeSchrift;
	private Color farbeHintergrund;

	public StruktogrammElement(Graphics2D g){
		this.g = g;

		obererRandZusatz = 20;

		bereich = new Rectangle();
		markiert = false;
		simulationSpotlight = false;
		elementfarbenExplizit = false;
		setzeText("");
		
		xVergroesserung = 0;
		yVergroesserung = 0;
	}




	//wandelt codierten String für die Quelltextgenerierung in einen auszugebenen String um
	protected String wandleZuAusgabe(String codierung, int typ, int anzahlEinzuruecken, boolean alsKommentare){
		codierung = einruecken(codierung,anzahlEinzuruecken);

		if(alsKommentare){
			codierung = codierung.replace(co("kommentar"),CodeErzeuger.gibKommentarZeichen(true, typ))
			.replace(co("kommentarzu"),CodeErzeuger.gibKommentarZeichen(false, typ));
		}else{
			codierung = codierung.replace(co("kommentar"),"")
			.replace(co("kommentarzu"),"");
		}

		int x = codierung.indexOf(co("text")); //x ist die Position, wo das erste Zeichen des Textes erscheinen soll
		if(x > -1){
			x = x - codierung.substring(0,x).lastIndexOf("\n") -1; // x minus die Position des letzten Zeilenumbruchs im Bereich 0 bis x ist der Abstand (in Zeichen) des ersten Textzeichens vom linken Rand; alle weiteren Zeilen sollen um x Leerzeichen eingerückt werden, um einen linksbündigen Textblock zu erhalten
		}else{
			x = 0; //co("text") war nicht im Eingabestring
		}



		return codierung.replace(co("text"),textzeilenAusgeben(anzahlEinzuruecken, x))
		.replace(co("zwangkommentar"),CodeErzeuger.gibKommentarZeichen(true, typ))
		.replace(co("zwangkommentarzu"),CodeErzeuger.gibKommentarZeichen(false, typ));
	}

	/**
	 * Kopfzeilen mit Bedingung/Ausdruck in Klammern: Bei {@code alsKommentar} zuerst den Struktogramm-Text
	 * als Kommentarzeile(n), danach {@code linkerTeil + Text + rechterTeil} ohne Kommentar in den Klammern
	 * (vermeidet verschachteltes {@code if} plus auskommentierte Bedingung in einer Zeile).
	 */
	protected String quellcodeMitKommentarVorspann(String linkerTeil, String rechterTeil, int typ, int anzahlEingerueckt,
			boolean alsKommentar) {
		StringBuilder sb = new StringBuilder();
		if (alsKommentar) {
			sb.append(wandleZuAusgabe(co("kommentar") + co("text") + co("kommentarzu") + "\n", typ, anzahlEingerueckt, true));
		}
		sb.append(wandleZuAusgabe(linkerTeil + co("text") + rechterTeil, typ, anzahlEingerueckt, false));
		return sb.toString();
	}

	protected String co(String s){ //codiert s, damit es sehr unwahrscheinlich wird, dass der User zufällig den Schlüsselstring eingibt
		return "%%"+s+"SbGRXEJUz4ZbvaaN%%";
	}


	//gibt einen String zurück, in dem alle Textzeilen mit Zeilenumbrüchen eingerückt enthalten sind
	protected String textzeilenAusgeben(int anzahlEinzuruecken, int xPosErsteZeile){
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < text.length; i++) {
			if (i > 0) {
				b.append(einruecken(text[i], xPosErsteZeile));
			} else {
				b.append(text[i]);
			}
			if (i < text.length - 1) {
				b.append('\n');
			}
		}
		return b.toString();
	}






	/*wird von den abgeleiteten Klassen überschrieben;
     typ gibt an, welche Sprache erzeugt werden soll, anzahlEingerueckt, wie weit bisher eingerückt worden ist,
     anzahlEinzuruecken, wie weit pro Einrückung eingerückt werden soll,
     alsKommentar, ob die Textzeilen und Fallnamen auskommentiert erscheinen sollen und
     textarea ist die JTextAreaEasy, in die der Code eingefügt werden soll*/
	public void quellcodeGenerieren(int typ, int anzahlEingerueckt, int anzahlEinzuruecken, boolean alsKommentar, JTextAreaEasy textarea){

	}

	protected String einruecken(String codeZeile, int anzahlStellen){
		if (anzahlStellen <= 0) {
			return codeZeile;
		}
		return " ".repeat(anzahlStellen) + codeZeile;
	}




	public void schreibeXMLDaten(Element elem){
		Element neues = new Element("strelem")//strelem-Tag mit dem Attribut typ, welches die Typnummer für das StruktogrammElement angibt, wird eingefügt
			.setAttribute("typ",""+Struktogramm.strElementZuTypnummer(this))
			.setAttribute("zx",""+xVergroesserung)
			.setAttribute("zy",""+yVergroesserung);
		if (elementfarbenExplizit) {
			neues.setAttribute("textcolor", "" + farbeSchrift.getRGB());
			neues.setAttribute("bgcolor", "" + farbeHintergrund.getRGB());
		}

		for (int i=0; i < text.length; i++){
			neues.addContent(new Element("text").addContent(XMLLeser.encodeS(text[i])));//in den strelem-Tag wird pro Textzeile ein text-Tag eingefügt, mit der Textzeile als Inhalt, die Textzeile ist dabei codiert, weil es beim laden später Probleme u.a. mit Umlauten gibt
		}

		zusaetzlicheXMLDatenSchreiben(neues);

		elem.addContent(neues); //strelem-Tag wird in den übergeordneten Tag eingefügt
	}


	//wird von Schleife und von Fallauswahl überschrieben
	protected void zusaetzlicheXMLDatenSchreiben(Element aktuelles){

	}




	public boolean istUnterelement(StruktogrammElement eventuellesUnterelement){
		return false;//eventuellesUnterelement ist nicht Unterelement von this
	}



	//wird in Fallauswahl überschrieben
	public String[] gibFaelle(){
		String[] faelle = new String[1];
		faelle[0] = "";
		return faelle;
	}


	public void setzeFaelle(String[] faelle){
		//nichts, wird nur in Verzweigung und Fallauswahl gebraucht
	}







	public int gibAnzahlListen(){
		return 0;
	}



	protected void setzeGraphics(Graphics2D g){
		this.g = g;
		randGroesseSetzen();
	}


	public void setzeMarkiert(boolean markiert){
		this.markiert = markiert;
	}

	public void setzeSimulationSpotlight(boolean simulationSpotlight) {
		this.simulationSpotlight = simulationSpotlight;
	}

	public boolean istSimulationSpotlight() {
		return simulationSpotlight;
	}

	public boolean istMarkiert(){
		return markiert;
	}

	protected boolean objGesetzt(Object obj){
		return obj != null;
	}



	public boolean neuesElementMussOberhalbPlatziertWerden(int y){
		return y < gibY() + gibHoehe()/2;
	}


	//gibt den Bereich zurück, der die Größe und Position des roten Vorschaurechteckes angibt, wenn die Maus an der Position vorschauPoint ist
	public Rectangle gibVorschauRect(Point vorschauPoint){
		int anYPos;

		if (neuesElementMussOberhalbPlatziertWerden(vorschauPoint.y)){
			//der Point ist auf der obere Hälfte -> Vorschau über diesem Element zeichnen
			anYPos = gibY() - vorschauHoehe / 2;
		}else{
			//Vorschau muss unter diesem Element gezeichnet werden
			anYPos = gibY() + gibHoehe() - vorschauHoehe / 2;
		}


		return new Rectangle(gibX(),anYPos,gibBreite(),vorschauHoehe);
	}


	public Rectangle gibRectangle(){
		return bereich;
	}

	protected int gibMindestbreite(){
		return gibBreiteDerBreitestenTextzeile() + xVergroesserung + 80;
	}
	
	
	protected int getXVergroesserung(){
		return xVergroesserung;
	}
	
	protected int getYVergroesserung(){
		return yVergroesserung;
	}


	public void setXVergroesserung(int xVergroesserung) {
		this.xVergroesserung = xVergroesserung;
	}




	public void setYVergroesserung(int yVergroesserung) {
		this.yVergroesserung = yVergroesserung;
	}




	protected int gibBreiteDerBreitestenTextzeile(){
		int typ = Struktogramm.strElementZuTypnummer(this);
		int groessteBreite = 0;
		for (int i = 0; i < text.length; i++) {
			String display = DiagramKeywordText.lineForDisplay(typ, i, text[i]);
			int breite;
			if (objGesetzt(g)) {
				breite = DiagramKeywordText.measureLineWidth(g, display);
			} else {
				breite = Math.max(display.length() * 7, 4 * display.length());
			}
			if (breite > groessteBreite) {
				groessteBreite = breite;
			}
		}
		return groessteBreite;
	}


	protected Object gibElementAnPos(int x, int y, boolean nurListe){//Elemente mit UnterElementen überschreiben diese Methode

		if (nurListe){
			return null;//Ich bin keine StruktogrammElementListe
		}

		if (bereich.contains(x,y)){
			return this;//der angegebene Punkt ist in meinem Bereich
		}else{
			return null;//der angegebene Punkt ist nicht in meinem Bereich
		}
	}


	public StruktogrammElementListe gibListeDieDasElementHat(StruktogrammElement element){
		return null; //ich habe keine Liste
	}


	public abstract Rectangle zeichenbereichAktualisieren(int x, int y);//wird in den Tochterklassen überschrieben
	

	public void setzeText(String[] text){
		this.text = text;
		randGroesseSetzen();
	}

	protected void randGroesseSetzen(){
		setObererRand(obererRandZusatz + text.length * gibTexthoehe(text[0]));
	}

	protected void setzeText(String textEineZeile){
		text = new String[1];
		text[0] = textEineZeile;
		randGroesseSetzen();
	}

	public String[] gibText(){
		return text;
	}


	public void zeichne(){ //wird überschrieben, aber mit super.zeichne(); aufgerufen
		eigenenBereichZeichnen();
		textZeichnen();
	}


	protected void textZeichnen(){
		int texthoehe = gibTexthoehe(text[0]);
		int yVerschiebungAktuell = texthoehe - 5;

		int typ = Struktogramm.strElementZuTypnummer(this);
		for (int i = 0; i < text.length; i++) {
			String display = DiagramKeywordText.lineForDisplay(typ, i, text[i]);
			int x = gibX() + gibXVerschiebungFuerTextInMitte(i, display);
			DiagramKeywordText.drawKeywordAwareLine(g, getFarbeSchrift(), x, gibY() + yVerschiebungAktuell, display);
			yVerschiebungAktuell += texthoehe;
		}
	}


	protected void eigenenBereichZeichnen(){
		Stroke alt = g.getStroke();
		try {
			g.setStroke(new BasicStroke(CanvasStyle.DIAGRAM_LINE_WIDTH));
			Color fill = getFarbeHintergrund();
			if (simulationSpotlight) {
				fill = CanvasStyle.getSimulationStepHighlightFill();
			}
			g.setColor(fill);
			g.fillRect(gibX(), gibY(), gibBreite(), gibHoehe());
			g.setColor(CanvasStyle.getElementBorder());
			g.drawRect(gibX(), gibY(), gibBreite(), gibHoehe());
			if (simulationSpotlight) {
				g.setStroke(new BasicStroke(CanvasStyle.SELECTION_LINE_WIDTH));
				g.setColor(CanvasStyle.getSelectionStroke());
				g.drawRect(gibX(), gibY(), gibBreite(), gibHoehe());
			} else if (markiert) {
				g.setStroke(new BasicStroke(CanvasStyle.SELECTION_LINE_WIDTH));
				g.setColor(CanvasStyle.getSelectionStroke());
				g.drawRect(gibX(), gibY(), gibBreite(), gibHoehe());
			}
		} finally {
			g.setStroke(alt);
		}
	}

	protected int gibX(){
		return bereich.x;
	}

	protected int gibY(){
		return bereich.y;
	}

	protected int gibBreite(){
		return bereich.width;
	}

	protected int gibHoehe(){
		return bereich.height;
	}


	public void setzeBreite(int neueBreite){
		bereich.width = neueBreite;
	}

	public void setzeHoehe(int neueHoehe){
		bereich.height = neueHoehe;
	}


	protected int gibTextbreite(String s){
		return (g != null) ? (int) g.getFontMetrics().getStringBounds(s, g).getBounds().getWidth() : s.length() * 4;//http://www.tutorials.de/java/288641-textlaenge-pixel.html
	}

	protected int gibTexthoehe(String s){
		if (objGesetzt(g)){ //sonst gib es einen Fehler beim erzeugen des Struktogramm, weil dort eine StruktogrammElementListe mit null als Graphics2D erzeugt wird und darin dann ein LeerElement
			return (int) g.getFontMetrics().getStringBounds(s, g).getBounds().getHeight();//http://www.tutorials.de/java/288641-textlaenge-pixel.html
		}else{
			return 20;
		}
	}


	protected int gibXVerschiebungFuerTextInMitte(int lineIndex, String displayLine){
		if (!objGesetzt(g)) {
			return (int) ((gibBreite() - displayLine.length() * 7) / 2);
		}
		return (int) ((gibBreite() - DiagramKeywordText.measureLineWidth(g, displayLine)) / 2);
	}

	/** Zentriert eine Anzeigezeile (Rohzeile Zeile 0: führendes Keyword wie in Swift). */
	protected int gibXVerschiebungFuerTextInMitte(String rawEineZeile){
		int typ = Struktogramm.strElementZuTypnummer(this);
		String display = DiagramKeywordText.lineForDisplay(typ, 0, rawEineZeile);
		return gibXVerschiebungFuerTextInMitte(0, display);
	}

	//gibt die x-Verschiebung zurück, damit der Text s mittig in einem Bereich der Breite breiteUntergrund dargestellt wird
	protected int gibXVerschiebungFuerMittig(String s, int breiteUntergrund){
		return (int)((breiteUntergrund - gibTextbreite(s)) / 2);//breiteUntergrund - gibTextbreite(s) ist, wie weit der Untergrund übersteht gegenüber der Textbreite; durch 2 dividieren des Überstehenden und eine Hälfe nach links packen, dann ist der Text in der Mitte
	}
	
	/*private int gibYVerschiebungFuerMittig(String s, int hoeheUntergrund){
		return (int)((hoeheUntergrund - gibTexthoehe(s)) / 2);//breiteUntergrund - gibTextbreite(s) ist, wie weit der Untergrund übersteht gegenüber der Textbreite; durch 2 dividieren des Überstehenden und eine Hälfe nach links packen, dann ist der Text in der Mitte
	}*/


	public void setzeXPos(int x){
		bereich.x = x;
	}
	
	public void zoomX(int erhoeheXUm){
		if(xVergroesserung + erhoeheXUm >= 0){
			xVergroesserung += erhoeheXUm;
		}
	}
	
	public void zoomY(int erhoeheYUm){
		if(yVergroesserung + erhoeheYUm >= 0){
			yVergroesserung += erhoeheYUm;
		}
	}
	
	public void zoomsZuruecksetzen(){
		xVergroesserung = 0;
		yVergroesserung = 0;
	}


	public void setObererRand(int obererRand) {
		this.obererRand = obererRand;
	}


	public int getObererRand() {
		return obererRand;
	}


	public Color getFarbeSchrift() {
		return elementfarbenExplizit ? farbeSchrift : CanvasStyle.getElementText();
	}

	public void setFarbeSchrift(Color farbeSchrift) {
		this.farbeSchrift = farbeSchrift;
		elementfarbenExplizit = true;
	}

	public Color getFarbeHintergrund() {
		return elementfarbenExplizit ? farbeHintergrund : CanvasStyle.getElementFill();
	}

	public void setFarbeHintergrund(Color farbeHintergrund) {
		this.farbeHintergrund = farbeHintergrund;
		elementfarbenExplizit = true;
	}

	/** Beim XML-Laden, wenn mindestens eine Farbe im Tag steht. */
	public void setzeFarbenAusXml(Color schrift, Color hintergrund) {
		this.farbeSchrift = schrift;
		this.farbeHintergrund = hintergrund;
		elementfarbenExplizit = true;
	}

	public boolean sindElementfarbenExplizit() {
		return elementfarbenExplizit;
	}

}