package de.visustruct.control;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import de.visustruct.view.UiTheme;

import de.visustruct.i18n.I18n;
import de.visustruct.other.Helpers;
import de.visustruct.other.XActionCommands;
import de.visustruct.simulation.SimulationEngine;
import de.visustruct.simulation.codec.XmlDecodeException;
import de.visustruct.simulation.model.SimulationDocument;
import de.visustruct.struktogrammelemente.StruktogrammElement;
import de.visustruct.view.AuswahlPanel;
import de.visustruct.view.CodeErzeuger;
import de.visustruct.view.EinstellungsDialog;
import de.visustruct.view.FontChooser;
import de.visustruct.view.GUI;
import de.visustruct.view.ZoomEinstellungen;

public class Controlling implements Konstanten, ActionListener, WindowListener, KeyListener {

	private static final Logger LOG = Logger.getLogger(Controlling.class.getName());

	private GUI gui;
	private boolean simulationMode;
	private enum Betriebssysteme {Windows, Mac, Linux};

	public Controlling(String[] params){
		handleOSSettingsAndTheme();

		gui = new GUI(this);
		// Nach vollständigem LaF/Layout: Paletten-Texte erneut setzen (sonst kann updateUI/Texte überschreiben).
		SwingUtilities.invokeLater(this::aktualisierePalettenBeschriftungen);
		neuesStruktogramm();		

		if(params != null){
			for(int i=0; i < params.length; i++){
				if(new File(params[i]).exists()){
					openStruktogramm(params[i]);
				}
			}
		}
	}



	public void handleOSSettingsAndTheme(){

		try{
			applyConfiguredTheme();

			if(getOS() == Betriebssysteme.Mac){
				new MacHandler(this);
			}

		}catch(Exception e){
			LOG.log(Level.SEVERE, "Theme/OS-Initialisierung fehlgeschlagen", e);
		}
	}


	/** Setzt das gewählte Theme (über Swing {@link UIManager#setLookAndFeel}) und wendet {@link UiTheme} an. */
	private void applyConfiguredTheme() {
		try {
			LookAndFeel lookAndFeel = null;

			switch (GlobalSettings.getLookAndFeelAktuell()) {
			case lookAndFeelOSStandard:
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException e1) {
					LOG.log(Level.WARNING, "System-Look-and-Feel konnte nicht gesetzt werden", e1);
				}
				break;

			case lookAndFeelFlatLight:
				try {
					if (getOS() == Betriebssysteme.Mac) {
						UIManager.setLookAndFeel(new FlatMacLightLaf());
					} else {
						UIManager.setLookAndFeel(new FlatLightLaf());
					}
				} catch (UnsupportedLookAndFeelException e) {
					LOG.log(Level.WARNING, "FlatLight-Look-and-Feel konnte nicht gesetzt werden", e);
				}
				break;

			case lookAndFeelFlatDark:
				try {
					if (getOS() == Betriebssysteme.Mac) {
						UIManager.setLookAndFeel(new FlatMacDarkLaf());
					} else {
						UIManager.setLookAndFeel(new FlatDarkLaf());
					}
				} catch (UnsupportedLookAndFeelException e) {
					LOG.log(Level.WARNING, "FlatDark-Look-and-Feel konnte nicht gesetzt werden", e);
				}
				break;

			case lookAndFeelNimbus:
				lookAndFeel = new NimbusLookAndFeel();
				break;

			case lookAndFeelSwingStandard:
				lookAndFeel = new MetalLookAndFeel();
				break;
			}

			if (lookAndFeel != null) {
				try {
					UIManager.setLookAndFeel(lookAndFeel);
				} catch (UnsupportedLookAndFeelException e) {
					LOG.log(Level.WARNING, "Look-and-Feel (Metal/Nimbus) konnte nicht gesetzt werden", e);
				}
			}

			UiTheme.applyAfterTheme();
			I18n.applyFileChooserStrings();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Look-and-Feel-Konfiguration fehlgeschlagen", e);
		}
	}



	private static Betriebssysteme getOS(){
		String s = System.getProperty("os.name").toLowerCase();

		if(s.startsWith("windows")){
			return Betriebssysteme.Windows;

		}else if(s.startsWith("mac")){
			return Betriebssysteme.Mac;

		}else if(s.startsWith("linux")){
			return Betriebssysteme.Linux;

		}else{
			return Betriebssysteme.Windows;
		}
	}


	public Struktogramm gibAktuellesStruktogramm(){
		if (gui == null) {
			return null;
		}
		return gui.gibTabbedpane().gibAktuellesStruktogramm();
	}

	public void paletteElementEinfuegen(int typ){
		Struktogramm str = gibAktuellesStruktogramm();
		if (str != null){
			str.neuesElementAnAktuellerStelleEinfuegen(typ);
			gui.gibTabbedpane().requestFocusInWindow();
		}
	}



	public Struktogramm neuesStruktogramm(){
		Struktogramm str = gui.gibTabbedpane().struktogrammHinzufuegen();
		str.graphicsInitialisieren();//erst nach setVisible(true); sonst gibt es Probleme in Struktogramm.graphicsInitialisieren()
		str.zeichenbereichAktualisieren();
		str.zeichne();
		return str;
	}


	public void speichern(boolean neuenSpeicherpfadAuswaehlenLassen){
		Struktogramm str = gibAktuellesStruktogramm();

		if(str != null){
			GlobalSettings.setzeSpeicherpfad(str.speichern(neuenSpeicherpfadAuswaehlenLassen,GlobalSettings.getZuletztGenutzterSpeicherpfad()));//Struktogramm wird gespeichert (zuletztGenutzterSpeicherpfad wird dabei übergeben, damit der JFileChooser, sofern er genutzt wird, dort startet) und der neue Speicherpfad wird gesichert
			String p = str.gibAktuellenSpeicherpfad();
			if (!p.isEmpty()) {
				GlobalSettings.rememberRecentStruktogrammPath(p);
			}
			GlobalSettings.saveSettings();
			titelleisteAktualisieren();
			gui.rebuildMenuBar();
		}
	}

	public void laden(){

		String pfad = Struktogramm.oeffnenDialog(GlobalSettings.getZuletztGenutzterSpeicherpfad(), gui);//Parameter ist der Startordner für den OpenDialog

		if(!pfad.equals("")){
			openStruktogramm(pfad);
		}
	}



	private void openStruktogramm(String pfad) {
		Struktogramm str = neuesStruktogramm();
		str.graphicsInitialisieren();
		str.laden(pfad);
		GlobalSettings.setzeSpeicherpfad(pfad);
		GlobalSettings.rememberRecentStruktogrammPath(pfad);
		GlobalSettings.saveSettings();
		titelleisteAktualisieren();
		gui.rebuildMenuBar();
	}


	/** Öffnet eine gespeicherte Datei aus dem Menü „Zuletzt geöffnet“. */
	public void oeffneStruktogrammAusZuletztListe(String pfad) {
		if (pfad == null || pfad.isBlank()) {
			return;
		}
		File f = new File(pfad);
		if (!f.isFile()) {
			GlobalSettings.removeRecentStruktogrammPath(pfad);
			GlobalSettings.saveSettings();
			gui.rebuildMenuBar();
			JOptionPane.showMessageDialog(gui, I18n.trf("dialog.recentMissing.message", pfad),
					I18n.tr("dialog.recentMissing.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		openStruktogramm(pfad);
	}


	/** Für macOS (OpenFilesHandler): Struktogramm aus Pfad öffnen, sobald die GUI steht. */
	public void oeffneStruktogrammDateiAusFinder(String pfad) {
		if (pfad != null && new File(pfad).exists()) {
			openStruktogramm(pfad);
		}
	}

	public void bildSpeichern(){
		Struktogramm str = gibAktuellesStruktogramm();

		if(str != null){
			GlobalSettings.setzeBildSpeicherpfad(str.alsBilddateiSpeichern(GlobalSettings.getZuletztGenutzterPfadFuerBild()));
			GlobalSettings.saveSettings();
		}
	}

	public void bildDrucken(){
		Struktogramm str = gibAktuellesStruktogramm();
		if (str == null) {
			return;
		}

		BufferedImage image = str.generateImage(false);
		PrinterJob job = PrinterJob.getPrinterJob();
		job.setJobName(GlobalSettings.APP_DISPLAY_NAME);
		job.setPrintable((graphics, pageFormat, pageIndex) -> {
			if (pageIndex > 0) {
				return Printable.NO_SUCH_PAGE;
			}
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				double scale = Math.min(
						pageFormat.getImageableWidth() / image.getWidth(),
						pageFormat.getImageableHeight() / image.getHeight());
				scale = Math.min(scale, 1.0d);
				double x = pageFormat.getImageableX() + (pageFormat.getImageableWidth() - image.getWidth() * scale) / 2.0d;
				double y = pageFormat.getImageableY() + (pageFormat.getImageableHeight() - image.getHeight() * scale) / 2.0d;
				g2.translate(x, y);
				g2.scale(scale, scale);
				g2.drawImage(image, 0, 0, null);
			} finally {
				g2.dispose();
			}
			return Printable.PAGE_EXISTS;
		});

		if (!job.printDialog()) {
			return;
		}
		try {
			job.print();
		} catch (PrinterException ex) {
			JOptionPane.showMessageDialog(gui, ex.getMessage(), I18n.tr("menu.file.print"), JOptionPane.ERROR_MESSAGE);
		}
	}

	public void titelleisteAktualisieren(){
		String pfad = "";

		Struktogramm str = gibAktuellesStruktogramm();
		if(str != null){
			pfad = str.gibAktuellenSpeicherpfad();
			if (!pfad.equals("")){
				pfad = " ["+pfad+"]";//wenn das aktuelle Struktogramm gespeichert oder geladen wurde, so wird sein Speicherpfad in der Titelleiste angezeigt
			}
		}

		gui.setTitle(GlobalSettings.guiTitel+pfad);
	}



	@Override
	public void actionPerformed(ActionEvent e) {

		switch(XActionCommands.valueOf(e.getActionCommand())){
		case neu:
			neuesStruktogramm();
			break;

		case oeffnen:
			laden();
			break;

		case speichern:
			speichern(false);
			break;

		case speicherUnter:
			speichern(true);
			break;

		case bildSpeichern:
			bildSpeichern();
			break;

		case bildDrucken:
			bildDrucken();
			break;

		case bildInZwischenAblage:
			copyImagetoClipBoard(gibAktuellesStruktogramm().generateImage(false));
			break;

		case quellcodeErzeugen:
			new CodeErzeuger(gui, I18n.tr("menu.file.generateCode"), true, gibAktuellesStruktogramm());
			break;

		case struktogrammSchliessen:
			gui.gibTabbedpane().aktuellesStruktogrammschliessen();
			break;

		case programmBeenden:
			programmBeendenGeklickt();
			break;

		case rueckgaengig:
			gibAktuellesStruktogramm().schrittZurueck();
			break;

		case widerrufen:
			gibAktuellesStruktogramm().schrittNachVorne();
			break;

		case ganzesStruktogrammKopieren:
			gui.gibAuswahlPanel().kopiereGanzesStruktogramm();
			break;

		case letztesElementStrecken:
			letzteElementeStreckenGeklickt(e.getSource());
			break;

		case elementBeschriftungEinstellen:
			new EinstellungsDialog(gui, true);
			break;

		case schriftartAendern:
			new FontChooser(this,true);
			break;

		case zoomeinstellungen:
			new ZoomEinstellungen(gui);
			break;

		case vergroesserungenRuckgaengigMachen:
			gibAktuellesStruktogramm().zoomsZuruecksetzen();
			break;

		case elementShortcutsVerwenden:
			elementEinfuegenShortcutsVerwendenGeklickt(e.getSource());
			break;

		case info:
			showInfo();
			break;

		case lookAndFeelOSStandard:
			changeTheme(lookAndFeelOSStandard);
			break;

		case lookAndFeelSwingStandard:
			changeTheme(lookAndFeelSwingStandard);
			break;

		case lookAndFeelNimbus:
			changeTheme(lookAndFeelNimbus);
			break;

		case lookAndFeelFlatLight:
			changeTheme(lookAndFeelFlatLight);
			break;

		case lookAndFeelFlatDark:
			changeTheme(lookAndFeelFlatDark);
			break;

		case languageEnglish:
			changeUiLanguageIfNeeded("en");
			break;

		case languageGerman:
			changeUiLanguageIfNeeded("de");
			break;

		case languagePortuguesePortugal:
			changeUiLanguageIfNeeded("pt_PT");
			break;
			
		case struktogrammbeschreibungHinzufuegen:
			addStruktogrammbeschriftung();
			break;

		case simulationToggle:
			onSimulationToggle();
			break;

		}

	}


	private void changeUiLanguageIfNeeded(String tag) {
		String next = GlobalSettings.normalizeUiLanguageTag(tag);
		if (next.equals(GlobalSettings.getUiLanguageTag())) {
			return;
		}
		GlobalSettings.setUiLanguageTag(next);
		I18n.syncWithSettings();
		GlobalSettings.saveSettings();
		gui.rebuildMenuBar();
		SwingUtilities.updateComponentTreeUI(gui);
		gui.validate();
		I18n.applyFileChooserStrings();
		// Nach updateComponentTreeUI (FlatLaf), sonst bleiben Paletten-Buttons oft auf der alten Sprache.
		aktualisierePalettenBeschriftungen();
		SwingUtilities.invokeLater(this::aktualisierePalettenBeschriftungen);
	}

	private void aktualisierePalettenBeschriftungen() {
		if (gui == null) {
			return;
		}
		gui.gibAuswahlPanel().aktualisiereBeschriftungen();
	}

	private void addStruktogrammbeschriftung() {
		Struktogramm str = gibAktuellesStruktogramm();
		String s = JOptionPane.showInputDialog(gui, I18n.tr("dialog.diagramCaption"), str.getStruktogrammBeschreibung());
		if(s == null){
			return;
		}
		str.setStruktogrammBeschreibung(s);
		str.rueckgaengigPunktSetzen();
		str.zeichenbereichAktualisieren();
		str.zeichne();		
	}



	private void changeTheme(int themeIndex){
		GlobalSettings.setLookAndFeelAktuell(themeIndex);
		GlobalSettings.saveSettings();
		applyConfiguredTheme();
		SwingUtilities.updateComponentTreeUI(gui);
		gui.validate();
		I18n.applyFileChooserStrings();
		aktualisierePalettenBeschriftungen();
		SwingUtilities.invokeLater(this::aktualisierePalettenBeschriftungen);
		gui.gibTabbedpane().refreshAllStruktogrammeNachThemeWechsel();
	}


	public void showInfo(){
		final String projektUrl = "https://github.com/code4teaching/VisuStruct";
		final String webUrl = "https://www.visustruct.org";
		final String developerUrl = "https://www.sebastiao.org";
		final String originalUrl = "https://github.com/kekru/struktogrammeditor";
		final String lucideUrl = "https://lucide.dev/";

		String html = "<html><body style=\"font-family:sans-serif;font-size:11pt;\">"
				+ "<p style=\"margin-top:0;margin-bottom:10px;\"><b>"
				+ GlobalSettings.APP_DISPLAY_NAME + " " + GlobalSettings.versionsString + "</b></p>"
				+ "<p style=\"margin-top:0;\">Holger Sebastiao<br/>"
				+ "Web: <a href=\"" + webUrl + "\">www.visustruct.org</a><br/>"
				+ "GitHub: <a href=\"" + projektUrl + "\">code4teaching/VisuStruct</a><br/>"
				+ "Developer: <a href=\"" + developerUrl + "\">www.sebastiao.org</a></p>"
				+ "<hr style=\"border:0;border-top:1px solid #ccc;\"/>"
				+ "<p style=\"margin-bottom:6px;\"><b>Acknowledgement</b></p>"
				+ "<p style=\"margin-top:0;\"><b>VisuStruct</b> builds on the open-source structure chart editor by Kevin Krummenauer (MIT).<br/>"
				+ "Prior source: <a href=\"" + originalUrl + "\">kekru/struktogrammeditor</a> on GitHub.</p>"
				+ "<p style=\"margin-top:0;\">Palette icons: <a href=\"" + lucideUrl + "\">Lucide Icons</a> "
				+ "(ISC License; some icons derived from Feather/MIT).<br/>"
				+ "License text is included in <code>licenses/LUCIDE.txt</code>.</p>"
				+ "</body></html>";

		JEditorPane pane = new JEditorPane("text/html", html);
		pane.setEditable(false);
		pane.setOpaque(false);
		pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		pane.addHyperlinkListener(e -> {
			if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED || e.getURL() == null) {
				return;
			}
			if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				return;
			}
			try {
				Desktop.getDesktop().browse(e.getURL().toURI());
			} catch (Exception ignored) {
				// ohne Browser keine Aktion
			}
		});

		JScrollPane scroll = new JScrollPane(pane);
		scroll.setBorder(null);
		scroll.getViewport().setOpaque(false);
		scroll.setPreferredSize(new Dimension(440, 260));

		String title = "Information - " + GlobalSettings.APP_DISPLAY_NAME + " " + GlobalSettings.versionsString;
		JOptionPane.showMessageDialog(gui, scroll, title, JOptionPane.INFORMATION_MESSAGE);
	}


	private void letzteElementeStreckenGeklickt(Object source){
		GlobalSettings.setzeLetzteElementeStrecken(((JCheckBoxMenuItem)source).isSelected());
		GlobalSettings.saveSettings();
		gibAktuellesStruktogramm().zeichenbereichAktualisieren();
		gibAktuellesStruktogramm().zeichne();
	}


	private void elementEinfuegenShortcutsVerwendenGeklickt(Object source){
		boolean einOderAus = ((JCheckBoxMenuItem)source).isSelected();
		GlobalSettings.setElementShortcutsVerwenden(einOderAus);
		GlobalSettings.saveSettings();
	}


	public GUI getGUI(){
		return gui;
	}

	/** Wechsel des Diagramm-Tabs beendet die Simulation (analog iOS-Moduswechsel). */
	public void onStruktogrammTabChanged() {
		if (simulationMode) {
			endSimulationInternal();
		}
	}

	/** Beendet die Simulation (Menü, Tab-Wechsel, Panel-Button „Zurück“). */
	public void leaveSimulationMode() {
		if (simulationMode) {
			endSimulationInternal();
		}
	}

	public boolean isSimulationMode() {
		return simulationMode;
	}

	/** Gleiche Aktion wie Bearbeiten → Simulation… / Diagramm bearbeiten… (Paletten-Button). */
	public void toggleSimulationFromUi() {
		onSimulationToggle();
	}

	private void onSimulationToggle() {
		if (simulationMode) {
			endSimulationInternal();
		} else {
			enterSimulation();
		}
	}

	private void enterSimulation() {
		if (gui.gibTabbedpane().getTabCount() <= 0) {
			return;
		}
		gui.gibElementEditorPanel().applyPendingTextToDiagram();
		Struktogramm str = gibAktuellesStruktogramm();
		if (str == null) {
			return;
		}
		try {
			SimulationDocument doc = str.toSimulationDocument();
			SimulationEngine eng = new SimulationEngine(doc);
			simulationMode = true;
			gui.getSimulationPanel().setEngine(eng);
			gui.showSimulationCard();
			gui.setEditSimulationMenuText(I18n.tr("menu.edit.diagramMode"));
			aktualisierePalettenBeschriftungen();
		} catch (XmlDecodeException ex) {
			JOptionPane.showMessageDialog(gui, ex.getMessage(), I18n.tr("simulation.error.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void endSimulationInternal() {
		simulationMode = false;
		gui.showEditorCard();
		gui.getSimulationPanel().clearEngine();
		gui.setEditSimulationMenuText(I18n.tr("menu.edit.simulation"));
		Struktogramm str = gibAktuellesStruktogramm();
		if (str != null) {
			str.setzeSimulationSpotlightPfad(null);
		}
		aktualisierePalettenBeschriftungen();
	}






	public boolean programmBeendenGeklickt(){
		leaveSimulationMode();
		if(gui.gibTabbedpane().einOderMehrereStruktogrammeNichtGespeichert()){                  
			Object[] options = { I18n.tr("dialog.exitUnsaved.quit"), I18n.tr("dialog.exitUnsaved.stay") };
			int r = JOptionPane.showOptionDialog(gui, I18n.tr("dialog.exitUnsaved.message"),
					I18n.tr("dialog.exitUnsaved.title"), JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null, options, options[1]);
			if (r == 0) {
				System.exit(0);
				return true;
			}
			return false;
		}
		System.exit(0);
		return true;
	}


	@Override
	public void windowOpened(WindowEvent e) {

	}


	@Override
	public void windowClosing(WindowEvent e) {
		programmBeendenGeklickt();
	}


	@Override
	public void windowClosed(WindowEvent e) {

	}


	@Override
	public void windowIconified(WindowEvent e) {

	}


	@Override
	public void windowDeiconified(WindowEvent e) {

	}


	@Override
	public void windowActivated(WindowEvent e) {

	}


	@Override
	public void windowDeactivated(WindowEvent e) {

	}


	@Override
	public void keyTyped(KeyEvent e) {
	}


	@Override
	public void keyPressed(KeyEvent e) {

	}


	@Override
	public void keyReleased(KeyEvent e) {
		if(e.getSource() == gui.gibTabbedpane() && (e.getModifiersEx() & GlobalSettings.strgOderApfelMask) == 0){

			switch(e.getKeyCode()){
			case KeyEvent.VK_A:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAnweisung);
				}
				break;

			case KeyEvent.VK_I:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typVerzweigung);
				}
				break;

			case KeyEvent.VK_S:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typFallauswahl);
				}
				break;

			case KeyEvent.VK_F:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typForSchleife);
				}
				break;

			case KeyEvent.VK_W:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typWhileSchleife);
				}
				break;

			case KeyEvent.VK_D:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typDoUntilSchleife);
				}
				break;

			case KeyEvent.VK_E:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typEndlosschleife);
				}
				break;

			case KeyEvent.VK_B:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAussprung);
				}
				break;

			case KeyEvent.VK_M:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAufruf);
				}
				break;

			case KeyEvent.VK_DELETE:
				gibAktuellesStruktogramm().elementAnAktuellerStelleLoeschen();
				break;
			}
		}

	}



	//http://stackoverflow.com/questions/4552045/copy-bufferedimage-to-clipboard
	public static void copyImagetoClipBoard(final BufferedImage image) {

		Transferable transferable = new Transferable() {

			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if(flavor.equals(DataFlavor.imageFlavor) && image != null) {
					return image;
				}else{
					throw new UnsupportedFlavorException(flavor);
				}
			}

			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] {DataFlavor.imageFlavor};
			}

			public boolean isDataFlavorSupported(DataFlavor flavor) {
				DataFlavor[] flavors = getTransferDataFlavors();
				for(DataFlavor f : flavors){
					if(flavor.equals(f)) {
						return true;
					}
				}

				return false;
			}
		};

		ClipboardOwner clipboardOwner = new ClipboardOwner() {				
			@Override
			public void lostOwnership(Clipboard clipboard, Transferable contents) {

			}
		};

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, clipboardOwner);

	}



}
