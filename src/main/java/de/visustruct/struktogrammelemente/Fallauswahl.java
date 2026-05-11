package de.visustruct.struktogrammelemente;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

import org.jdom2.Element;

import de.visustruct.control.CanvasStyle;
import de.visustruct.control.DiagramKeywordText;
import de.visustruct.control.GlobalSettings;
import de.visustruct.control.Struktogramm;
import de.visustruct.control.XMLLeser;
import de.visustruct.i18n.I18n;
import de.visustruct.view.CodeErzeuger;
import de.visustruct.other.JTextAreaEasy;

public class Fallauswahl extends StruktogrammElement { //erbt von StruktogrammElement
	//private Struktogramm str; //wird in hier gebraucht, um auf die Einstellung zugreifen zu können, ob die letzten Elemente bei Bedarf gestreckt werden sollen
	protected int xVerschiebungFuerTrennlinie; //legt fest, wie weit vom linken Rand der Fallauswahl die Trennlinie zwischen Vorletztem- und Sonst-Fall sein soll
	protected int yVerschiebungFuerTrennLinie; //legt fest, wie hoch die oben genannte Trennlinie sein soll
	protected ArrayList<StruktogrammElementListe> listen; //Liste von StruktogrammElementListen, für die einzelnen Fälle; als Generische ArrayList: http://www.theserverside.de/java-generics-generische-methoden-klassen-und-interfaces/

	public Fallauswahl(Graphics2D g){
		this(g,3);//anderen Konstruktor aufrufen
	}


	public Fallauswahl(Graphics2D g, int anzahlListen){
		super(g);

		//this.str = str;

		erstelleNeueListen(anzahlListen);

		yVerschiebungFuerTrennLinie = -20; //Trennlinie zwischen Vorletztem und Sonst-Fall geht 20 Pixel in den Kopfteil hinein

		listen.get(listen.size() - 1).setzeBeschreibung(I18n.tr("structure.multiway.defaultCaseLabel"));

		obererRandZusatz = 40; //der Kopfteil soll 40 Pixel plus die Höhe des Textes sein

		setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typFallauswahl));
	}





	@Override
	public void quellcodeGenerieren(int typ, int anzahlEingerueckt, int anzahlEinzuruecken, boolean alsKommentar, JTextAreaEasy textarea){
		String vorher = "";
		String nachher = "";
		String fall = "";
		String fallEnde = "";


		if (typ == CodeErzeuger.typPython) {
			vorher = quellcodeMitKommentarVorspann("match ", ":\n", typ, anzahlEingerueckt, alsKommentar);
			nachher = "";
		} else {
			vorher = quellcodeMitKommentarVorspann("switch(", "){\n", typ, anzahlEingerueckt, alsKommentar);
			nachher = "}\n";
		}

		textarea.hinzufuegen(wandleZuAusgabe(vorher, typ, anzahlEingerueckt, alsKommentar));

		for (int i = 0; i < listen.size(); i++) {
			if (typ == CodeErzeuger.typPython) {
				if (i < listen.size() - 1) {
					fall = "case " + CodeGenRules.caseLabelToken(listen.get(i).gibBeschreibung(), typ) + ":\n";
				} else {
					fall = "case _:" + CodeGenRules.forcedCaseComment(listen.get(i).gibBeschreibung(), typ) + "\n";
				}
				fallEnde = "";
			} else {
				if (i < listen.size() - 1) {
					fall = "case " + CodeGenRules.caseLabelToken(listen.get(i).gibBeschreibung(), typ) + ":\n";
				} else {
					fall = "default: " + CodeGenRules.forcedCaseComment(listen.get(i).gibBeschreibung(), typ) + "\n";
				}
				fallEnde = einruecken("break;\n", anzahlEinzuruecken);
			}

			textarea.hinzufuegen(wandleZuAusgabe(fall, typ, anzahlEingerueckt + anzahlEinzuruecken, alsKommentar));
			listen.get(i).quellcodeAllerUnterelementeGenerieren(typ, anzahlEingerueckt + anzahlEinzuruecken * 2, anzahlEinzuruecken,
					alsKommentar, textarea);
			if (!fallEnde.isEmpty()) {
				textarea.hinzufuegen(wandleZuAusgabe(fallEnde, typ, anzahlEingerueckt + anzahlEinzuruecken, alsKommentar));
			}
		}

		if (!nachher.isEmpty()) {
			textarea.hinzufuegen(wandleZuAusgabe(nachher, typ, anzahlEingerueckt, alsKommentar));
		}

	}



	//alle Listen werden gelöscht und Neue werden erzeugt
	public void erstelleNeueListen(int anzahlListen){
		listen = new ArrayList<StruktogrammElementListe>();

		for(int i=0; i < anzahlListen; i++){
			listen.add(new StruktogrammElementListe(g));
			listen.get(i).setzeBeschreibung(""+(i+1));
		}
	}



	//neue Spalte links neben der Sonst-Spalte wird erstellt
	public void erstelleNeueSpalte(){
		int listennummer = listen.size() -1;
		listen.add(listennummer, new StruktogrammElementListe(g));

		listen.get(listennummer).setzeBeschreibung(""+(listennummer+1));
	}


	//die Spalte mit der Nummer spaltenIndex wird nach links oder nach rechts verschoben, je nach Parameter-Wert
	public void spalteVerschieben(boolean nachLinks, int spaltenIndex){
		if (nachLinks){
			if (spaltenIndex > 0)
				listenTauschen(spaltenIndex, spaltenIndex -1);
		}else{
			if (spaltenIndex <= listen.size() -2)
				listenTauschen(spaltenIndex, spaltenIndex +1);
		}
	}


	//Zwei Spalten (Listen) werden getauscht
	protected void listenTauschen(int index1, int index2){
		StruktogrammElementListe tmp = listen.get(index1);
		listen.set(index1,listen.get(index2));
		listen.set(index2,tmp);
	}


	public void entferneSpalte(int index){
		if ((index >= 0) && (index < listen.size()) && (listen.size() > 2)){//wenn mehr als 2 Spalten da sind, darf eine gelöscht werden
			listen.remove(index);
		}
	}



	//zusätzliche XML Daten sind die Daten zu den Fall-Listen
	@Override
	protected void zusaetzlicheXMLDatenSchreiben(Element aktuelles){
		Element unterelement;

		for (int i=0; i < listen.size(); i++){
			unterelement = new Element("fall").setAttribute("fallname",XMLLeser.encodeS(listen.get(i).gibBeschreibung()));//für jeden Fall wird ein neuer fall-Tag generiert mit dem Fallnamen als Attribut (codiert)

			listen.get(i).schreibeXMLDatenAllerUnterElemente(unterelement);//in den neuen fall-Tag werden die xml-Daten der Unterelemente geschrieben
			aktuelles.addContent(unterelement);//fall-Tag wird in den strelem-Tag geschrieben, der zu dieser Fallauswahl gehört
		}

	}




	//prüft, ob eventuellesUnterelement irgendwo innerhalb einer der Fall-Listen steht
	@Override
	public boolean istUnterelement(StruktogrammElement eventuellesUnterelement){
		for (int i=0; i < listen.size(); i++){//Fall-Listen durchgehen
			if(listen.get(i).istUnterelement(eventuellesUnterelement)){//einzelne Listen fragen
				return true;
			}
		}

		return false;
	}



	//gibt die Fallbeschreibungen als String-Array zurück
	@Override
	public String[] gibFaelle(){
		String[] faelle = new String[listen.size()];
		for (int i=0; i < listen.size(); i++){
			faelle[i] = listen.get(i).gibBeschreibung();
		}

		return faelle;
	}


	//setzt die Fallbeschreibungen
	@Override
	public void setzeFaelle(String[] faelle){
		for (int i=0; i < listen.size(); i++){
			listen.get(i).setzeBeschreibung(faelle[i]);
		}
	}



	@Override
	public int gibAnzahlListen(){
		return listen.size();
	}



	public StruktogrammElementListe gibListe(int index){
		return listen.get(index);
	}





	@Override
	public boolean neuesElementMussOberhalbPlatziertWerden(int y){
		return y < gibY() + getObererRand()/2;//ist die Maus im oberen Teil des Kopfteils, so soll ein neues StruktogrammElement oberhalb eingefügt werden, sonst unterhalb
	}


	@Override
	protected void setzeGraphics(Graphics2D g){
		super.setzeGraphics(g);

		for (int i=0; i < listen.size(); i++){
			listen.get(i).graphicsAllerUnterlementeSetzen(g);
		}
	}


	@Override
	public void setzeBreite(int neueBreite){//FallAuswahl wird horizontal getreckt, einzelne Fall-Listen müssen mitgestreckt werden

		int gesamtbreiteDerListen = 0;
		int neueSpaltenbreite = 0;

		for (int i=0; i < listen.size(); i++){

			listen.get(i).xPosAllerUnterelementeSetzen(gibX() + gesamtbreiteDerListen);//x-Position der Unterelemente neu setzen

			if (i <= listen.size() -2){

				//Rechnung: neueGesamtbreite / alteGesamtbreite = neueSpaltenbreite / alteSpaltenbreite //-> Verhältnisse sollen gleich bleiben
				//<=> neueSpaltenbreite = neueGesamtbreite * alteSpaltenbreite / alte Gesamtbreite
				neueSpaltenbreite = neueBreite * listen.get(i).gibBreite() / gibBreite();

				listen.get(i).breiteDerUnterelementeSetzen(neueSpaltenbreite);

			}else{//das rechteste Element
				neueSpaltenbreite = neueBreite - gesamtbreiteDerListen;

				listen.get(i).breiteDerUnterelementeSetzen(neueSpaltenbreite);
				xVerschiebungFuerTrennlinie = neueBreite - neueSpaltenbreite;
			}


			gesamtbreiteDerListen += neueSpaltenbreite;

		}


		bereich.width = neueBreite;

	}



	@Override
	public void setzeHoehe(int neueHoehe){

		for(int i=0; i < listen.size(); i++){
			listen.get(i).gesamtHoeheSetzen(neueHoehe -getObererRand());
		}

		bereich.height = neueHoehe;
	}




	@Override
	protected Object gibElementAnPos(int x, int y, boolean nurListe){
		Object tmp;

		if (bereich.contains(x,y)){ //der angegebene Punkt ist in meinem Bereich


			for (int i=0; i < listen.size(); i++){

				tmp = listen.get(i).gibElementAnPos(x,y,nurListe);
				if (objGesetzt(tmp)){
					return tmp; //eine Liste oder ein StruktogrammElement wurde gefunden, welches den Punkt enthält
				}

			}

			if (!nurListe){
				return this; //der Punkt ist nicht in einem der Fall-Listen, also, wenn nicht nur nach Listen gefragt ist, diese Fallauswahl zurückgeben
			}
		}

		return null; //der Punkt ist nicht auf dieser Fallauswahl, oder er ist auf dem Kopfteil und nurListe ist true
	}




	@Override
	public StruktogrammElementListe gibListeDieDasElementHat(StruktogrammElement element){
		StruktogrammElementListe tmp;

		for (int i=0; i < listen.size(); i++){//Fall-Listen werden gefragt, ob sie, oder deren Unterlisten, das Element haben
			tmp = listen.get(i).gibListeDieDasElementHat(element);
			if (tmp != null){
				return tmp;
			}
		}

		return null;//keine Unterliste dieser Fallauswahl hat das gesuchte Element
	}




	private int gibPassendeYKoordFuerLinie(int x){
		//herausfinden, bei welcher y-Koordinate die senkrechten Linien der Fallauswahl oben enden müssen
		//y = m * x + b
		//m = (y2 - y1)/(x2-x1)
		double m = (double)(gibY() - (gibY()+getObererRand()+yVerschiebungFuerTrennLinie)) / (double)(gibX() - (gibX() + xVerschiebungFuerTrennlinie));

		//b = y - m * x
		double b = gibY() - m * gibX();

		//y = m * x + b
		return (int)(m * x + b);
	}


	@Override
	public void zeichne(){
		eigenenBereichZeichnen();//Umrandung und Markierungs-Hintergrund zeichnen

		//Fall-Listen zeichnen
		for (int i=0; i < listen.size(); i++){
			if (!listen.get(i).isEmpty()){
				listen.get(i).alleZeichnen();
			}
		}


		//die beiden Schrägen Linien zeichnen
		g.setColor(CanvasStyle.getElementBorder());
		g.drawLine(gibX(),gibY(),gibX() + xVerschiebungFuerTrennlinie, gibY() +getObererRand() +yVerschiebungFuerTrennLinie);
		g.drawLine(gibX() + xVerschiebungFuerTrennlinie, gibY() +getObererRand() +yVerschiebungFuerTrennLinie, gibX() + gibBreite(), gibY());


		//senkrechte Striche im Kopfteil der Fallauswahl und Fallbeschriftungen zeichnen
		int x;
		StruktogrammElementListe tmp;

		for (int i=0; i < listen.size(); i++){
			tmp = listen.get(i);

			x = tmp.gibRechterRand();

			if (i != listen.size() -1){//senkrechte Striche zeichnen, die Sonstliste (die Letzte) braucht keinen senkrechten Strich
				g.setColor(CanvasStyle.getElementBorder());
				g.drawLine(x, gibY()+gibHoehe(), x, gibPassendeYKoordFuerLinie(x));
			}

			//Fallbeschreibungen zeichnen
			if (this instanceof Verzweigung){//ein bischen rumtricksen... bei der Verzweigung soll Ja und Nein ja ganz an den Rändern stehen
				if (i==0){
					x = gibX() + 5; //5 Pixel von linken Rand entfernt
				}else{
					x = tmp.gibRechterRand() - 5 - DiagramKeywordText.measureLineWidth(g, tmp.gibBeschreibung()); //5 Pixel vom rechten Rand entfernt
				}
			}else{ //bei der Fallauswahl sollen die Überschriften mittig über den Zeilen stehen
				int colW = tmp.gibRechterRand() - tmp.gibX();
				String lab = tmp.gibBeschreibung();
				x = tmp.gibX() + (int) ((colW - DiagramKeywordText.measureLineWidth(g, lab)) / 2);
			}

			DiagramKeywordText.drawKeywordAwareLine(g, getFarbeSchrift(), x, gibY() + getObererRand() - 5, tmp.gibBeschreibung());
		}


		textZeichnen(); //Textzeilen zeichnen

	}




	@Override
	public Rectangle zeichenbereichAktualisieren(int x, int y){

		int gesamtbreiteDerListen = 0;
		int groessteHoeheDerListen = 0;

		for (int i=0; i < listen.size(); i++){

			Rectangle rectListe = listen.get(i).zeichenbereichAllerElementeAktualisieren(x + gesamtbreiteDerListen, y + getObererRand());//Zeichenbereich der einzelnen Listen aktualisieren, die x-Position ist immer der rechte Rand der vorherigen Liste
			gesamtbreiteDerListen += rectListe.width;

			//Höhe der größten Liste ermitteln
			if (rectListe.height > groessteHoeheDerListen){
				groessteHoeheDerListen = rectListe.height;
			}
		}



		xVerschiebungFuerTrennlinie = gesamtbreiteDerListen - listen.get(listen.size()-1).gibBreite(); //xVerschiebung der Trennlinie ist Breite der Listen minus Breite der Sonst-Liste




		if (gesamtbreiteDerListen < gibMindestbreite()){
			gesamtbreiteDerListen = gibMindestbreite();
		}


		if(GlobalSettings.gibLetzteElementeStrecken()){//wenn der User diese Einstellung gewählt hat...
			setzeHoehe(getObererRand() + groessteHoeheDerListen);//...Höhe setzen und somit das letzte Element jeder Liste strecken
		}


		bereich.setBounds(x, y, gesamtbreiteDerListen, getObererRand() + groessteHoeheDerListen);//eigenen Bereich festhalten
		

		return bereich;
	}




	@Override
	public void setzeXPos(int x){
		bereich.x = x;

		int xVerschiebung = 0;

		for (int i=0; i < listen.size(); i++){
			listen.get(i).xPosAllerUnterelementeSetzen(x + xVerschiebung);
			xVerschiebung += listen.get(i).gibBreite();
		}
	}
	
	
	@Override
	public void zoomsZuruecksetzen(){
		super.zoomsZuruecksetzen();
		
		for(int i=0; i < listen.size(); i++){
			listen.get(i).zoomsAllerElementeZuruecksetzen();
		}
	}
	
	
	@Override
	public int getObererRand(){
		return super.getObererRand() + getYVergroesserung();
	}

}