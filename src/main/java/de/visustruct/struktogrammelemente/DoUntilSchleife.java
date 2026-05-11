package de.visustruct.struktogrammelemente;

import java.awt.Font;
import java.awt.Graphics2D;

import de.visustruct.control.CanvasStyle;
import de.visustruct.control.DiagramKeywordText;
import de.visustruct.control.GlobalSettings;
import de.visustruct.control.Struktogramm;
import de.visustruct.other.JTextAreaEasy;
import de.visustruct.view.CodeErzeuger;

public class DoUntilSchleife extends Schleife {//erbt von Schleife

   public DoUntilSchleife(Graphics2D g){
      super(g);
      
      setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typDoUntilSchleife));
   }
   
	/**
	 * Mehr linker Einzug als bei anderen Schleifen: Platz für fettes „do“ ohne Überlappung
	 * mit der senkrechten Rumpfkante (2px-Strich).
	 */
	@Override
	protected int gibSchleifenLinkenRandInnen() {
		return 44;
	}

	/** Abstand vom Kastenstrich zur Außenkante; verhindert Überlappung mit dem 2px-Rahmen. */
	private static final int DO_LABEL_PAD_AUSSEN = 6;
	/** Freiraum zwischen rechtem Rand von „do“ und der Rumpf-Linie (inkl. halber Strichbreite). */
	private static final int DO_LABEL_PAD_VOR_RUMPF = 10;

	@Override
	public void zeichne() {
		super.zeichne();
		zeichneDoObenLinks();
	}

	/**
	 * „do“ im linken Streifen zwischen Außenkante und Rumpf — mit Abstand zu Rahmen und senkrechter Rumpfkante.
	 */
	private void zeichneDoObenLinks() {
		if (!objGesetzt(g)) {
			return;
		}
		int stroke = (int) Math.ceil(CanvasStyle.DIAGRAM_LINE_WIDTH);
		int gutterLeft = gibX() + stroke + DO_LABEL_PAD_AUSSEN;
		int innerBodyLeft = gibX() + gibSchleifenLinkenRandInnen();
		int halfStroke = (int) Math.ceil(CanvasStyle.DIAGRAM_LINE_WIDTH / 2f);
		int gutterRight = innerBodyLeft - halfStroke - DO_LABEL_PAD_VOR_RUMPF;
		if (gutterRight <= gutterLeft) {
			gutterRight = gutterLeft + 1;
		}
		Font saved = g.getFont();
		try {
			g.setFont(saved.deriveFont(Font.BOLD));
			int doWide = DiagramKeywordText.measureLineWidth(g, "do");
			int x = gutterLeft;
			if (doWide < gutterRight - gutterLeft) {
				x = gutterLeft + (gutterRight - gutterLeft - doWide) / 2;
			}
			int ascent = g.getFontMetrics().getAscent();
			int y = gibY() + stroke + DO_LABEL_PAD_AUSSEN + ascent;
			DiagramKeywordText.drawKeywordAwareLine(g, getFarbeSchrift(), x, y, "do");
		} finally {
			g.setFont(saved);
		}
	}

   
   
   @Override
   protected void textZeichnen(){
      int texthoehe = gibTexthoehe(text[0]);
      int yVerschiebungAktuell = gibHoehe() -15; //Position der untersten Zeile: 15 Pixel über dem unteren Rand

      int typ = Struktogramm.typDoUntilSchleife;
      // Textzeilen von unten nach oben; pro Zeile gleicher Index wie im Text-Array (für while/until-Normalisierung).
      for (int i = text.length - 1; i >= 0; i--) {
         String display = DiagramKeywordText.lineForDisplay(typ, i, text[i]);
         int x = gibX() + gibXVerschiebungFuerTextInMitte(i, display);
         DiagramKeywordText.drawKeywordAwareLine(g, getFarbeSchrift(), x, gibY() + yVerschiebungAktuell, display);
         yVerschiebungAktuell -= texthoehe;
      }
   }
   
   
   @Override
   public boolean neuesElementMussOberhalbPlatziertWerden(int y){
      return y < gibY() + gibHoehe() - getUntererRand()/2;//hier wird anhand der Position der Maus im Kopfteil (der unten ist) entschieden, weil man beim ganzer Betrachtung nicht unterhalb einfügen kann
   }
   
   
   @Override
   protected void randGroesseSetzen(){
      setUntererRand(obererRandZusatz + text.length * gibTexthoehe(text[0]));//der untere Rand ist der Zusatzrand plus die Höhe aller Textzeilen (obererRandZusatz heißt nur oberer..., weil DoUntilSchleife von Schleife erbt und dort von einem oberen Rand ausgegangen wird)
   }
   
   
   
   

   @Override
   public void quellcodeGenerieren(int typ, int anzahlEingerueckt, int anzahlEinzuruecken, boolean alsKommentar, JTextAreaEasy textarea){
     String vorher = "";
      String nachher = "";


      if (typ == CodeErzeuger.typPython) {
         vorher = "while True:\n";
         int bodyStart = anzahlEingerueckt + anzahlEinzuruecken;
         nachher = quellcodeMitKommentarVorspann("if not (", "):\n", typ, bodyStart, alsKommentar)
               + wandleZuAusgabe("break\n", typ, bodyStart + anzahlEinzuruecken, alsKommentar);
      } else {
         vorher = "do{\n";
         nachher = quellcodeMitKommentarVorspann("}while(", ");\n", typ, anzahlEingerueckt, alsKommentar);
      }

      textarea.hinzufuegen(wandleZuAusgabe(vorher, typ, anzahlEingerueckt, alsKommentar));
      liste.quellcodeAllerUnterelementeGenerieren(typ, anzahlEingerueckt + anzahlEinzuruecken, anzahlEinzuruecken, alsKommentar, textarea);
      if (typ == CodeErzeuger.typPython) {
         textarea.hinzufuegen(wandleZuAusgabe(nachher, typ, 0, alsKommentar));
      } else {
         textarea.hinzufuegen(wandleZuAusgabe(nachher, typ, anzahlEingerueckt, alsKommentar));
      }
   }
   
   
   
   @Override
	public int getObererRand(){
		return 0;
	}
   
   @Override
	public int getUntererRand(){
		return super.getUntererRand() + getYVergroesserung();
	}
   
}