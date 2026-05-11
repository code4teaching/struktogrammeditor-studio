package de.visustruct.view;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import de.visustruct.control.Controlling;
import de.visustruct.control.GlobalSettings;
import de.visustruct.control.Konstanten;
import de.visustruct.i18n.I18n;
import de.visustruct.other.XActionCommands;

/** Hauptfenster von VisuStruct (basiert auf Struktogrammeditor). */

public class GUI extends JFrame implements Konstanten{

	private static final long serialVersionUID = -3526840402506170333L;
	private AuswahlPanel auswahlPanel; //Panel an der linken Seite, wo die Labels zu finden sind, von denen man neue StruktogrammElemente in das Struktogramm ziehen kann
	private StrTabbedPane tabbedpane; //TabbedPane, in dem die Struktogramme sind  
	private ElementEditorPanel elementEditorPanel;
	private Controlling controlling;
	private final JMenuBar menubar;

	public GUI(Controlling controlling) {

		super(GlobalSettings.guiTitel);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		int frameWidth = 1180;
		int frameHeight = 700;
		setSize(frameWidth, frameHeight);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (d.width - getSize().width) / 2;
		int y = (d.height - getSize().height) / 2;
		setLocation(x, y);


		this.controlling = controlling;

		//Container cp = getContentPane();
		//cp.setLayout(null);
		setLayout(new BorderLayout());

		setIconImage(new ImageIcon(getClass().getResource(GlobalSettings.logoName)).getImage());


		//setJMenuBar(new MenueLeiste(this));

		tabbedpane = new StrTabbedPane(controlling);
		elementEditorPanel = new ElementEditorPanel();
		//tabbedpane.setBounds(xPosTabbedPane, 0, 671, 401);
		//cp.add(tabbedpane);

		auswahlPanel = new AuswahlPanel(controlling);
		//auswahlPanel.setBounds(0,0,200,500);
		//add(auswahlPanel);

		JScrollPane paletteScroll = new JScrollPane(auswahlPanel);
		paletteScroll.setBorder(BorderFactory.createEmptyBorder());
		java.awt.Color vp = UIManager.getColor(VisuStructTheme.KEY_PALETTE_BACKGROUND);
		paletteScroll.getViewport().setBackground(vp != null ? vp : UIManager.getColor("Panel.background"));
		JSplitPane editorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedpane, elementEditorPanel);
		editorSplit.setResizeWeight(1.0);
		editorSplit.setDividerLocation(460);
		editorSplit.setDividerSize(5);
		editorSplit.setContinuousLayout(true);
		editorSplit.setBorder(BorderFactory.createEmptyBorder());

		JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, paletteScroll, editorSplit);
		splitpane.setOneTouchExpandable(true);
		splitpane.setDividerLocation(288);
		splitpane.setDividerSize(5);
		splitpane.setContinuousLayout(true);
		splitpane.setBorder(BorderFactory.createEmptyBorder());
		add(splitpane, BorderLayout.CENTER);

		//Struktogramm str = neuesStruktogramm();


		menubar = new JMenuBar();
		buildMenuBar();
		setJMenuBar(menubar);

		// Kein fullWindowContent / transparentTitleBar auf macOS: führt unter FlatLaf + aktuellem JDK
		// oft zu grauem, nicht bedienbarem Client-Bereich (Fensterinhalt wird nicht korrekt gezeichnet).

		addWindowListener(controlling);

		setResizable(true);
		setVisible(true);
		//addComponentListener(this);




		//tabbedpane.changeListenerAktivieren(); //wenn der ChangeListener früher aktiviert wird, kommt es zu Problemen, da darin graphicsInitialisieren aufgerufen wird, was nicht funktioniert wenn die entsprechende Komponente noch nicht vollständig erzeugt ist


	}

	/** Baut die Menüleiste aus den aktuellen I18n-Texten (z. B. nach Sprachwechsel). */
	public void rebuildMenuBar() {
		menubar.removeAll();
		buildMenuBar();
		menubar.revalidate();
	}

	private void buildMenuBar() {
		JMenu menu = createMenu(I18n.tr("menu.file"), KeyEvent.VK_F);
		{
			menu.add(createMenuItem(I18n.tr("menu.file.new"), XActionCommands.neu, KeyEvent.VK_N, KeyEvent.VK_N));
			menu.add(createMenuItem(I18n.tr("menu.file.open"), XActionCommands.oeffnen, KeyEvent.VK_O, KeyEvent.VK_O));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.file.save"), XActionCommands.speichern, KeyEvent.VK_S, KeyEvent.VK_S));
			menu.add(createMenuItem(I18n.tr("menu.file.saveAs"), XActionCommands.speicherUnter, KeyEvent.VK_A));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.file.saveImage"), XActionCommands.bildSpeichern, KeyEvent.VK_I));
			menu.add(createMenuItem(I18n.tr("menu.file.print"), XActionCommands.bildDrucken, KeyEvent.VK_P, KeyEvent.VK_P));
			menu.add(createMenuItem(I18n.tr("menu.file.copyImage"), XActionCommands.bildInZwischenAblage, KeyEvent.VK_B, KeyEvent.VK_K));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.file.generateCode"), XActionCommands.quellcodeErzeugen, KeyEvent.VK_G));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.file.closeDiagram"), XActionCommands.struktogrammSchliessen, KeyEvent.VK_C, KeyEvent.VK_W));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.file.about"), XActionCommands.info, KeyEvent.VK_B));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.file.exit"), XActionCommands.programmBeenden, KeyEvent.VK_X));
		}
		menubar.add(menu);

		menu = createMenu(I18n.tr("menu.edit"), KeyEvent.VK_E);
		{
			menu.add(createMenuItem(I18n.tr("menu.edit.undo"), XActionCommands.rueckgaengig, KeyEvent.VK_U, KeyEvent.VK_Z));
			menu.add(createMenuItem(I18n.tr("menu.edit.redo"), XActionCommands.widerrufen, KeyEvent.VK_R, KeyEvent.VK_Y));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.edit.caption"), XActionCommands.struktogrammbeschreibungHinzufuegen, KeyEvent.VK_T));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.edit.copyDiagram"), XActionCommands.ganzesStruktogrammKopieren, KeyEvent.VK_Y));
		}
		menubar.add(menu);

		menu = createMenu(I18n.tr("menu.settings"), KeyEvent.VK_S);
		{
			menu.add(createMenuItem(I18n.tr("menu.settings.stretch"), XActionCommands.letztesElementStrecken, KeyEvent.VK_L, GlobalSettings.gibLetzteElementeStrecken()));
			menu.add(new JSeparator());

			JMenu menu2 = createMenu(I18n.tr("menu.settings.theme"), KeyEvent.VK_T);
			{
				ButtonGroup group = new ButtonGroup();

				JRadioButtonMenuItem radioMenuitem = new JRadioButtonMenuItem(I18n.tr("menu.settings.theme.modernLight"));
				radioMenuitem.addActionListener(controlling);
				radioMenuitem.setActionCommand(XActionCommands.lookAndFeelFlatLight.toString());
				radioMenuitem.setSelected(GlobalSettings.getLookAndFeelAktuell() == lookAndFeelFlatLight);
				group.add(radioMenuitem);
				menu2.add(radioMenuitem);

				radioMenuitem = new JRadioButtonMenuItem(I18n.tr("menu.settings.theme.modernDark"));
				radioMenuitem.addActionListener(controlling);
				radioMenuitem.setActionCommand(XActionCommands.lookAndFeelFlatDark.toString());
				radioMenuitem.setSelected(GlobalSettings.getLookAndFeelAktuell() == lookAndFeelFlatDark);
				group.add(radioMenuitem);
				menu2.add(radioMenuitem);

				menu2.add(new JSeparator());

				radioMenuitem = new JRadioButtonMenuItem(I18n.tr("menu.settings.theme.osDefault"));
				radioMenuitem.addActionListener(controlling);
				radioMenuitem.setActionCommand(XActionCommands.lookAndFeelOSStandard.toString());
				radioMenuitem.setSelected(GlobalSettings.getLookAndFeelAktuell() == lookAndFeelOSStandard);
				group.add(radioMenuitem);
				menu2.add(radioMenuitem);

				radioMenuitem = new JRadioButtonMenuItem(I18n.tr("menu.settings.theme.swingDefault"));
				radioMenuitem.addActionListener(controlling);
				radioMenuitem.setActionCommand(XActionCommands.lookAndFeelSwingStandard.toString());
				radioMenuitem.setSelected(GlobalSettings.getLookAndFeelAktuell() == lookAndFeelSwingStandard);
				group.add(radioMenuitem);
				menu2.add(radioMenuitem);

				radioMenuitem = new JRadioButtonMenuItem(I18n.tr("menu.settings.theme.nimbus"));
				radioMenuitem.addActionListener(controlling);
				radioMenuitem.setActionCommand(XActionCommands.lookAndFeelNimbus.toString());
				radioMenuitem.setSelected(GlobalSettings.getLookAndFeelAktuell() == lookAndFeelNimbus);
				group.add(radioMenuitem);
				menu2.add(radioMenuitem);
			}
			menu.add(menu2);

			JMenu langMenu = createMenu(I18n.tr("menu.settings.languages"), KeyEvent.VK_I);
			{
				ButtonGroup langGroup = new ButtonGroup();
				JRadioButtonMenuItem en = new JRadioButtonMenuItem(I18n.tr("menu.settings.language.en"));
				en.addActionListener(controlling);
				en.setActionCommand(XActionCommands.languageEnglish.toString());
				langGroup.add(en);
				langMenu.add(en);

				JRadioButtonMenuItem de = new JRadioButtonMenuItem(I18n.tr("menu.settings.language.de"));
				de.addActionListener(controlling);
				de.setActionCommand(XActionCommands.languageGerman.toString());
				langGroup.add(de);
				langMenu.add(de);

				JRadioButtonMenuItem pt = new JRadioButtonMenuItem(I18n.tr("menu.settings.language.pt"));
				pt.addActionListener(controlling);
				pt.setActionCommand(XActionCommands.languagePortuguesePortugal.toString());
				langGroup.add(pt);
				langMenu.add(pt);

				// Erst nach Aufnahme in die ButtonGroup wählen (sonst kann die Auswahl je nach LAF inkonsistent sein)
				if (GlobalSettings.isUiGerman()) {
					de.setSelected(true);
				} else if (GlobalSettings.isUiPortuguesePortugal()) {
					pt.setSelected(true);
				} else {
					en.setSelected(true);
				}
			}
			menu.add(langMenu);

			menu.add(createMenuItem(I18n.tr("menu.settings.labelsStruktogramm"), XActionCommands.elementBeschriftungEinstellen, KeyEvent.VK_B));
			menu.add(createMenuItem(I18n.tr("menu.settings.changeFont"), XActionCommands.schriftartAendern, KeyEvent.VK_F));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.settings.mouseWheel"), XActionCommands.groesseAendernMitMausrad, KeyEvent.VK_M, GlobalSettings.isBeiMausradGroesseAendern()));
			menu.add(createMenuItem(I18n.tr("menu.settings.zoom"), XActionCommands.zoomeinstellungen, KeyEvent.VK_Z));
			menu.add(createMenuItem(I18n.tr("menu.settings.resetSizes"), XActionCommands.vergroesserungenRuckgaengigMachen, KeyEvent.VK_R));
			menu.add(new JSeparator());
			menu.add(createMenuItem(I18n.tr("menu.settings.shortcuts"), XActionCommands.elementShortcutsVerwenden, KeyEvent.VK_K, GlobalSettings.isElementShortcutsVerwenden()));
		}
		menubar.add(menu);
	}


	private JMenu createMenu(String name, int auswahlBuchstabe){
		JMenu neuMenu = new JMenu(name);
		neuMenu.setMnemonic(auswahlBuchstabe);
		return neuMenu;
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe){
		return createMenuItem(name, actionCommand, auswahlBuchstabe, -1, -1);
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe, int shortcutBuchstabe){
		return createMenuItem(name, actionCommand, auswahlBuchstabe, shortcutBuchstabe, GlobalSettings.strgOderApfelMask, false, false);
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe, int shortcutBuchstabe, int shortcutMask){
		return createMenuItem(name, actionCommand, auswahlBuchstabe, shortcutBuchstabe, shortcutMask, false, false);
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe, boolean isChecked){
		return createMenuItem(name, actionCommand, auswahlBuchstabe, -1, -1, true, isChecked);
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe, int shortcutBuchstabe, int shortcutMask, boolean isCheckBox, boolean isChecked){
		JMenuItem menuitem;

		if(isCheckBox){
			menuitem = new JCheckBoxMenuItem(name);
			((JCheckBoxMenuItem)menuitem).setSelected(isChecked);
		}else{
			menuitem = new JMenuItem(name);
		}

		menuitem.setActionCommand(actionCommand.toString());

		if(auswahlBuchstabe > -1){
			menuitem.setMnemonic(auswahlBuchstabe);
		}

		if(shortcutBuchstabe > -1){
			menuitem.setAccelerator(KeyStroke.getKeyStroke(shortcutBuchstabe, shortcutMask));
		}

		menuitem.addActionListener(controlling);

		return menuitem;
	}


	public StrTabbedPane gibTabbedpane(){
		return tabbedpane;
	}

	public ElementEditorPanel gibElementEditorPanel(){
		return elementEditorPanel;
	}


	public AuswahlPanel gibAuswahlPanel(){
		return auswahlPanel;
	}


	public JMenuBar getMenubar() {
		return menubar;
	}

}
