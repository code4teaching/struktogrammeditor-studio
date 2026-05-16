package de.visustruct.view;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdom2.Document;

import de.visustruct.control.Struktogramm;
import de.visustruct.i18n.I18n;
import de.visustruct.struktogrammelemente.Fallauswahl;
import de.visustruct.struktogrammelemente.StruktogrammElement;
import de.visustruct.struktogrammelemente.Verzweigung;

//http://download.oracle.com/javase/tutorial/uiswing/components/menu.html
//http://download.oracle.com/javase/tutorial/uiswing/examples/components/MenuDemoProject/src/components/MenuDemo.java

public class StruktogrammPopup extends JPopupMenu implements PopupMenuListener{

	private static final long serialVersionUID = -6394669590636692950L;
	private int typ;
	private StruktogrammElement element; //das Element, bei dem das PopupMenü geöffnet wurde
	private Struktogramm struktogramm;
	private final int popupKlickX;
	private final int popupKlickY;

	public StruktogrammPopup(StruktogrammElement element, Struktogramm struktogramm, int klickX, int klickY){
		super();

		addPopupMenuListener(this);

		this.element = element;
		this.struktogramm = struktogramm;
		this.popupKlickX = klickX;
		this.popupKlickY = klickY;

		typ = Struktogramm.strElementZuTypnummer(element);

		// Platzhalter „ø“: nur Einfügen — kein Bearbeiten/Kopieren/Löschen/Zoom für ein LeerElement.
		if (typ == Struktogramm.typLeerElement) {
			einfuegen(I18n.tr("popup.paste"), 14);
			return;
		}

		StrPopupUntermenue untermenue = unterMenueEinfuegen(I18n.tr("popup.zoom"));
		untermenue.einfuegen(I18n.tr("popup.zoom.larger"), 8, -1);
		untermenue.einfuegen(I18n.tr("popup.zoom.smaller"), 9, -1);
		untermenue.add(new JSeparator());
		untermenue.einfuegen(I18n.tr("popup.zoom.wider"), 10, -1);
		untermenue.einfuegen(I18n.tr("popup.zoom.narrower"), 11, -1);
		untermenue.add(new JSeparator());
		untermenue.einfuegen(I18n.tr("popup.zoom.taller"), 12, -1);
		untermenue.einfuegen(I18n.tr("popup.zoom.shorter"), 13, -1);
		add(new JSeparator());

		einfuegen(I18n.tr("popup.editText"), 0);
		einfuegen(I18n.tr("popup.copy"), 7);
		einfuegen(I18n.tr("popup.paste"), 14);
		einfuegen(I18n.tr("popup.delete"), 1);

		switch(typ){
		case Struktogramm.typVerzweigung: //Bei Verzweigung diesen Menüpunkt hinzufügen:
			add(new JSeparator());
			einfuegen(I18n.tr("popup.swapBranches"), 2);
			break;

		case Struktogramm.typFallauswahl: //Bei Fallauswahl diese Menüpunkte hinzufügen:
			add(new JSeparator());

			einfuegen(I18n.tr("popup.insertNewCase"), 3);

			String[] faelle = element.gibFaelle();

			for (int i=0; i < faelle.length; i++){

				untermenue = unterMenueEinfuegen(I18n.trf("popup.caseSubmenu", faelle[i]));

				if (i > 0) {
					untermenue.einfuegen(I18n.trf("popup.moveCaseLeft", faelle[i]), 4, i);
				}

				if (i < faelle.length -1){
					untermenue.einfuegen(I18n.trf("popup.moveCaseRight", faelle[i]), 5, i);
					untermenue.einfuegen(I18n.trf("popup.removeCase", faelle[i]), 6, i);
				}

			}
			break;
		}

	}


	private void einfuegen(String text,int id){
		add(new StrPopupItem(text,id));
	}


	private StrPopupUntermenue unterMenueEinfuegen(String text){
		StrPopupUntermenue tmp = new StrPopupUntermenue(element,struktogramm,text);
		add(tmp);
		return tmp;
	}




	public void popupMenuWillBecomeVisible(PopupMenuEvent e){
		struktogramm.setzePopupmenuSichtbar(true);
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e){
		struktogramm.setzePopupmenuSichtbar(false);
	}

	public void popupMenuCanceled(PopupMenuEvent e){
		struktogramm.setzePopupmenuSichtbar(false);
	}









	private class StrPopupUntermenue extends JMenu{

		private static final long serialVersionUID = 1L;
		//StruktogrammElement element;
		//Struktogramm struktogramm;

		public StrPopupUntermenue(StruktogrammElement element, Struktogramm struktogramm, String text){
			super(text);
			//this.element = element;
			//this.struktogramm = struktogramm;
		}

		private void einfuegen(String text,int id, int fallnummer){
			add(new StrPopupItem(text,id,fallnummer));
		}
	}



	private class StrPopupItem extends JMenuItem implements ActionListener{

		private static final long serialVersionUID = 1L;
		private int id;
		private int fallnummer;

		public StrPopupItem(String text, int id){
			this(text,id,-1);
		}

		public StrPopupItem(String text, int id, int fallnummer){
			super(text);
			this.id = id;
			this.fallnummer = fallnummer;
			addActionListener(this);
		}


		public void actionPerformed(ActionEvent e){

			boolean mussSpeicherpunktSetzen = true;
			
			//passende Aktion tätigen
			switch(id){
			case 0: struktogramm.elementBefuellen(element); //EingabeDialog öffnen, hier drin wird schon ein Rückgängigpunkt gesetzt
			mussSpeicherpunktSetzen = false;
			break;

			case 1: struktogramm.elementLoeschen(element, true); //dieses Element löschen, hier drin wird schon ein Rückgängigpunkt gesetzt
			mussSpeicherpunktSetzen = false;
			break;

			case 2: ((Verzweigung)element).seitenVertauschen();
			break;

			case 3: ((Fallauswahl)element).erstelleNeueSpalte();
			break;

			case 4: ((Fallauswahl)element).spalteVerschieben(true,fallnummer); //Spalte nach links verschieben
			break;

			case 5: ((Fallauswahl)element).spalteVerschieben(false,fallnummer); //Spalte nach rechts verschieben
			break;

			case 6: String fallname = ((Fallauswahl)element).gibFaelle()[fallnummer]; //Spalte bzw. Fall löschen
			Object[] entfOpts = { I18n.tr("dialog.common.yes"), I18n.tr("dialog.common.no") };
			if (JOptionPane.showOptionDialog(struktogramm, I18n.trf("popup.removeCaseConfirm", fallname),
					I18n.tr("popup.removeCaseTitle"), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
					null, entfOpts, entfOpts[1]) == 0) {
				((Fallauswahl)element).entferneSpalte(fallnummer);
			} else {
				mussSpeicherpunktSetzen = false;
			}
			break;

			case 7: struktogramm.gibTabbedPane().gibGUI().gibAuswahlPanel().setzeKopiertesStrElement(struktogramm.xmlErstellen(element)); //Abbild des Elementes in Kopier-Box ablegen
			mussSpeicherpunktSetzen = false;
			break;

			case 14: {
				Document kopie = struktogramm.gibTabbedPane().gibGUI().gibAuswahlPanel().gibKopiertesStrElement();
				if (kopie == null) {
					JOptionPane.showMessageDialog(struktogramm, I18n.tr("popup.pasteEmpty"), I18n.tr("popup.pasteTitle"),
							JOptionPane.INFORMATION_MESSAGE);
					mussSpeicherpunktSetzen = false;
				} else {
					struktogramm.elementAusKopierFeldEinfuegenAnKoordinaten(popupKlickX, popupKlickY);
					mussSpeicherpunktSetzen = false;
				}
			}
			break;

			case 8: //Größer (x-Richtung und y-Richtung)
				struktogramm.zoom(1, 1, element);
				break;
				
			case 9: //Kleiner(x-Richtung und y-Richtung)
				struktogramm.zoom(-1, -1, element);
				break;
				
			case 10: //Breiter (x-Richtung)
				struktogramm.zoom(1, 0, element);
				break;
				
			case 11: //Schmaler (x-Richtung)
				struktogramm.zoom(-1, 0, element);
				break;
				
			case 12: //Höher (y-Richtung)
				struktogramm.zoom(0, 1, element);
				break;
				
			case 13: //Weniger Hoch (y-Richtung)
				struktogramm.zoom(0, -1, element);
				break;

			}


			struktogramm.zeichenbereichAktualisieren();
			struktogramm.zeichne();

			if (mussSpeicherpunktSetzen){
				struktogramm.rueckgaengigPunktSetzen();
			}

		}
	}

}