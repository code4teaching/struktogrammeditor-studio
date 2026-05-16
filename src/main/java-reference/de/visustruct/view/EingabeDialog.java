package de.visustruct.view;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.BoxLayout;

import de.visustruct.i18n.I18n;
import de.visustruct.other.JListEasy;
import de.visustruct.other.JTextAreaEasy;
import de.visustruct.struktogrammelemente.Fallauswahl;
import de.visustruct.struktogrammelemente.StruktogrammElement;
import de.visustruct.struktogrammelemente.Verzweigung;


public class EingabeDialog extends JDialog {

	private static final long serialVersionUID = -7385908673937166978L;
	// Anfang Attribute
	private JTextAreaEasy textarea;
	private JListEasy list;
	private String[] rueckgabeInhalt;
	private StruktogrammElement element;
	private boolean okWurdeGedrueckt = false;	
	private Color schriftfarbeNeu, hintergrundfarbeNeu;
	private final Color initialSchriftfarbe, initialHintergrundfarbe;
	private final boolean initialFarbenExplizit;
	private final JButton buttonSchriftfarbe, buttonHintergrundfarbe;
	

	public EingabeDialog(JFrame owner, String title, boolean modal, StruktogrammElement element) {

		super(owner, title, modal);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);


		this.element = element;

		initialSchriftfarbe = element.getFarbeSchrift();
		initialHintergrundfarbe = element.getFarbeHintergrund();
		initialFarbenExplizit = element.sindElementfarbenExplizit();
		schriftfarbeNeu = initialSchriftfarbe;
		hintergrundfarbeNeu = initialHintergrundfarbe;

		setLayout(new GridBagLayout());


		GridBagConstraints c = new GridBagConstraints();			
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.ipadx = 1;
		c.ipady = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0,0,4,0);
		
		
		JButton button;
		
		c.gridwidth = 3;


		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		add(new JLabel("Header text:"), c);

		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 100;
		c.weighty = 150;
		c.fill = GridBagConstraints.BOTH;
		add(new JScrollPane(textarea = new JTextAreaEasy()), c);

		c.weighty = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		
		String[] vorherigerInhalt = element.gibText();

		for (int i=0; i < vorherigerInhalt.length; i++){
			textarea.hinzufuegen(vorherigerInhalt[i]);

			if (i != vorherigerInhalt.length -1){//nach der letzten Zeile soll kein Zeilenumbruch passieren, damit man direkt in der letzten Zeile weiterschreiben kann
				textarea.zeilenumbruch();
			}
		}

		rueckgabeInhalt = vorherigerInhalt;
		

		int anzahlListen = element.gibAnzahlListen();

		if (anzahlListen > 0){

			c.gridx = 0;
			c.gridy = 2;
			c.weightx = 1;
			add(new JLabel(element instanceof Verzweigung ? "Left and right branch labels:" : "Case labels:"), c);

			c.gridx = 0;
			c.gridy = 3;
			c.weightx = 100;
			// Genug Höhe für mehrere Case-Zeilen (sonst wirkt Bearbeiten "weg").
			c.weighty = 60;
			c.fill = GridBagConstraints.BOTH;
			add(new JScrollPane(list = new JListEasy()), c);
			
			c.weighty = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			
			
			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			c.gridx = 2;
			c.gridy = 4;
			c.weightx = 100;
			JPanel caseButtons = new JPanel();
			caseButtons.setLayout(new BoxLayout(caseButtons, BoxLayout.Y_AXIS));
			JButton rename = new JButton(I18n.tr("dialog.caseLabel.renameButton"));
			rename.addActionListener(e -> buttonFallname_ActionPerformed(e));
			caseButtons.add(rename);

			// Nur bei Mehrfachauswahl (nicht bei Verzweigung): neue Case-Spalte direkt im Dialog anlegen.
			if (element instanceof Fallauswahl && !(element instanceof Verzweigung)) {
				JButton insert = new JButton(I18n.tr("popup.insertNewCase"));
				insert.addActionListener(e -> buttonNeuerFall_ActionPerformed(e));
				caseButtons.add(insert);
			}
			add(caseButtons, c);
			
			
			
			//Element ist Verzweigung oder Fallauswahl, anzahlListen ist für Schleifen 0, weil diese keine Überschrift brauchen
			//JListEasy erzeugen, die die Fallnamen, oder bei einer Verzweigung "Ja" und "Nein" enthält, um diese umbenennen zu können
			refreshCaseList();

		}
		
		
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		
		c.gridx = 0;
		c.gridy = 4;
		c.weightx = 1;
		button = new JButton("Text color");
		buttonSchriftfarbe = button;
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonSchriftfarbeGeklickt();
			}
		});
		add(button, c);
		
		
		c.gridx = 1;
		c.gridy = 4;
		c.weightx = 100;
		button = new JButton("Fill color");
		buttonHintergrundfarbe = button;
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonHintergrundfarbeGeklickt();
			}
		});
		add(button, c);
		
		
		c.gridx = 0;
		c.gridy = 5;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		button = new JButton("OK");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonOK_ActionPerformed(e);
			}
		});
		add(button, c);
		
		c.fill = GridBagConstraints.NONE;
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 2;
		c.gridy = 5;
		c.weightx = 100;
		button = new JButton("Cancel");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonAbbrechen_ActionPerformed(e);
			}
		});
		add(button, c);
		
		
		aktualisiereButtonfarben();

		setSize(400,500);
		setLocationRelativeTo(owner);
		setVisible(true); //alles nach setVisible(true) wird erst beim Schließen des Dialogs gemacht
	}
	

	//OK-Button wurde gedrückt
	public void buttonOK_ActionPerformed(ActionEvent evt) {
		if (element instanceof Fallauswahl){//Element ist vom Typ Fallauswahl oder Verzweigung

			//alle Zeilen der JListEasy auslesen und in ein String-Array schreiben
			String[] fallBezeichnungen = new String[list.gibAnzahl()];
			for (int i=0; i < fallBezeichnungen.length; i++){
				fallBezeichnungen[i] = list.gibInhalt(i);
			}

			element.setzeFaelle(fallBezeichnungen);//Fallüberschriften setzen
		}
		
		boolean farbenGeaendert = schriftfarbeNeu.getRGB() != initialSchriftfarbe.getRGB()
				|| hintergrundfarbeNeu.getRGB() != initialHintergrundfarbe.getRGB();
		if (initialFarbenExplizit || farbenGeaendert) {
			element.setFarbeSchrift(schriftfarbeNeu);
			element.setFarbeHintergrund(hintergrundfarbeNeu);
		}

		rueckgabeInhalt = textarea.gibTextzeilenArray();

		okWurdeGedrueckt = true;

		setVisible(false);
	}

	
	private void buttonSchriftfarbeGeklickt(){
		schriftfarbeNeu = ColorDialog.showColorChooser(this, schriftfarbeNeu);
		aktualisiereButtonfarben();
	}
	
	private void buttonHintergrundfarbeGeklickt(){
		hintergrundfarbeNeu = ColorDialog.showColorChooser(this, hintergrundfarbeNeu);
		aktualisiereButtonfarben();
	}

	//Abbrechen-Button wurde gedrückt
	public void buttonAbbrechen_ActionPerformed(ActionEvent evt) {
		setVisible(false);
	}


	//Button zum Ändern der Fallnamen wurde gedrückt
	public void buttonFallname_ActionPerformed(ActionEvent evt) {
		if(list.gibIndex() >= 0){

			String fallname = (String) JOptionPane.showInputDialog(this, I18n.tr("dialog.caseLabel.newMessage"),
					I18n.tr("dialog.caseLabel.newTitle"), JOptionPane.PLAIN_MESSAGE, null, null,
					list.gibMarkiertenInhalt());

			if (fallname != null){
				if (fallname.equals("")){
					fallname = " ";//wenn der Fallname ein leerer String ist, daraus einen String mit einem Leerzeichen machen, ansonsten ist die Zeile nur noch sehr klein in der Liste (von der Höhe her), wenn nichts drin steht
				}
				list.setzeText(fallname,list.gibIndex());//Fallname setzen
			}

		}else{//es ist noch kein Fall in der JListEasy ausgewählt
			JOptionPane.showMessageDialog(this, I18n.tr("dialog.caseSelectFirst.message"),
					I18n.tr("dialog.caseSelectFirst.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void buttonNeuerFall_ActionPerformed(ActionEvent evt) {
		if (!(element instanceof Fallauswahl) || (element instanceof Verzweigung)) {
			return;
		}
		((Fallauswahl) element).erstelleNeueSpalte();
		refreshCaseList();
		// neu eingefügten Fall markieren (vor dem letzten Eintrag = default)
		if (list != null && list.gibAnzahl() >= 2) {
			list.setzeIndex(list.gibAnzahl() - 2);
		}
	}

	private void refreshCaseList() {
		if (list == null) {
			return;
		}
		list.entferneAlle();
		String[] inhalt = element.gibFaelle();
		for (String s : inhalt) {
			list.fuegeHinzu(s);
		}
	}



	//wenn zuvor der OK-Button gedrückt wurde, wird der Inhalt der TextArea zurückgegeben, ansonsten null (dann soll nichts verändert werden, weil der User Abbrechen oder das Schließen-X angeklickt hat)
	public String[] gibTextArray(){
		return okWurdeGedrueckt ? rueckgabeInhalt : null;
	}

	public Color getSchriftfarbeNeu() {
		return schriftfarbeNeu;
	}

	public Color getHintergrundfarbeNeu() {
		return hintergrundfarbeNeu;
	}
	
	
	private void aktualisiereButtonfarben(){
		buttonSchriftfarbe.setForeground(schriftfarbeNeu);
		buttonHintergrundfarbe.setBackground(hintergrundfarbeNeu);
	}

}
