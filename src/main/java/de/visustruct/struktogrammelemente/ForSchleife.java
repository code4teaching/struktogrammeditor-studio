package de.visustruct.struktogrammelemente;

import java.awt.Graphics2D;

import de.visustruct.control.DiagramKeywordText;
import de.visustruct.control.GlobalSettings;
import de.visustruct.control.Struktogramm;
import de.visustruct.other.JTextAreaEasy;
import de.visustruct.view.CodeErzeuger;

public class ForSchleife extends WhileSchleife { //erbt von WhileSchleife
   private static final String KOPF_TRENNER = "; ";

   public ForSchleife(Graphics2D g){
      super(g);
      
      setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typForSchleife));
   }

   @Override
   protected void randGroesseSetzen(){
      if (hatMehrteiligenKopf()){
         setObererRand(obererRandZusatz + gibTexthoehe(text[0]));
      }else{
         super.randGroesseSetzen();
      }
   }

   @Override
   protected int gibBreiteDerBreitestenTextzeile(){
      if (hatMehrteiligenKopf()){
         String display = DiagramKeywordText.lineForDisplay(Struktogramm.typForSchleife, 0, gibKopfText());
         if (objGesetzt(g)) {
            return DiagramKeywordText.measureLineWidth(g, display);
         }
         return Math.max(display.length() * 7, 4 * display.length());
      }

      return super.gibBreiteDerBreitestenTextzeile();
   }

   @Override
   protected void textZeichnen(){
      if (!hatMehrteiligenKopf()){
         super.textZeichnen();
         return;
      }

      String kopfText = gibKopfText();
      int texthoehe = gibTexthoehe(text[0]);

      String display = DiagramKeywordText.lineForDisplay(Struktogramm.typForSchleife, 0, kopfText);
      int x = gibX() + gibXVerschiebungFuerTextInMitte(0, display);
      DiagramKeywordText.drawKeywordAwareLine(g, getFarbeSchrift(), x, gibY() + texthoehe - 5, display);
   }

   private boolean hatMehrteiligenKopf(){
      return text != null && text.length > 1;
   }

   private String gibKopfText(){
      return String.join(KOPF_TRENNER, text);
   }
   
   
   @Override     //siehe DoUntilSchleife
   public void quellcodeGenerieren(int typ, int anzahlEingerueckt, int anzahlEinzuruecken, boolean alsKommentar, JTextAreaEasy textarea){
      String vorher = "";
      String nachher = "";


      if (typ == CodeErzeuger.typPython) {
         String pythonFor = CodeGenRules.pythonForLineFromJavaLikeLoopFields(text);
         if (pythonFor != null) {
            vorher = quellcodeMitKommentarVorspann("", "\n", pythonFor, typ, anzahlEingerueckt, alsKommentar);
         } else {
            vorher = quellcodeMitKommentarVorspann("for ", ":\n", typ, anzahlEingerueckt, alsKommentar);
         }
         nachher = "";
      } else if (typ == CodeErzeuger.typJavaScript && hatMehrteiligenKopf()) {
         vorher = quellcodeMitKommentarVorspann("for(", "){\n", CodeGenRules.javaScriptForLoopHeader(text), typ, anzahlEingerueckt, alsKommentar);
         nachher = "}\n";
      } else if (hatMehrteiligenKopf()) {
         vorher = quellcodeMitKommentarVorspann("for(", "){\n", CodeGenRules.cStyleForLoopHeader(text), typ, anzahlEingerueckt, alsKommentar);
         nachher = "}\n";
      } else {
         vorher = quellcodeMitKommentarVorspann("for(", "){\n", typ, anzahlEingerueckt, alsKommentar);
         nachher = "}\n";
      }

      textarea.hinzufuegen(wandleZuAusgabe(vorher,typ,anzahlEingerueckt,alsKommentar));
      liste.quellcodeAllerUnterelementeGenerieren(typ,anzahlEingerueckt+anzahlEinzuruecken,anzahlEinzuruecken,alsKommentar,textarea);
      if (!nachher.isEmpty()) {
         textarea.hinzufuegen(wandleZuAusgabe(nachher,typ,anzahlEingerueckt,alsKommentar));
      }

   }

   private String quellcodeMitKommentarVorspann(String linkerTeil, String rechterTeil, String codeText, int typ,
         int anzahlEingerueckt, boolean alsKommentar) {
      StringBuilder sb = new StringBuilder();
      if (alsKommentar) {
         sb.append(wandleZuAusgabe(co("kommentar") + co("text") + co("kommentarzu") + "\n", typ, anzahlEingerueckt, true));
      }
      sb.append(einruecken(linkerTeil + codeText + rechterTeil, anzahlEingerueckt));
      return sb.toString();
   }
   
}