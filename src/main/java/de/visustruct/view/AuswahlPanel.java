package de.visustruct.view;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.jdom2.Document;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import de.visustruct.control.Controlling;
import de.visustruct.control.Struktogramm;
import de.visustruct.i18n.I18n;


public class AuswahlPanel extends JPanel implements DropTargetListener, DragGestureListener, DragSourceListener{

	private static final long serialVersionUID = 3619714917985247680L;
	private AuswahlPanelElement[] panelElemente = new AuswahlPanelElement[9]; //9 StruktogrammElemente stehen zur Auswahl
	private JButton palettePngButton;
	private JButton paletteInfoButton;
	private DragSource dragSource;
	//private DropTarget dropTarget;
	private JLabel muelleimer;
	private boolean muelleimerIstAuf;
	private Controlling controlling;
	private Document kopiertesStrElement;
	private boolean paletteDragAktiv;
	private long letzterPaletteDragEndeZeitpunkt;

	public AuswahlPanel(Controlling controlling){

		this.controlling = controlling;

		setLayout(new GridBagLayout());
		setOpaque(true);
		Color pal = UIManager.getColor(VisuStructTheme.KEY_PALETTE_BACKGROUND);
		setBackground(pal != null ? pal : UIManager.getColor("Panel.background"));
		Color sep = UIManager.getColor("Separator.foreground");
		if (sep == null) {
			sep = new Color(0xD1, 0xD5, 0xDB);
		}
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 0, 1, sep),
				BorderFactory.createEmptyBorder(8, 10, 8, 12)));

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.ipadx = 1;
		c.ipady = 1;
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(1, 0, 3, 0);


		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;

		dragSource = new DragSource();
		for (int i = 0; i < panelElemente.length; i++) {
			int typ = StruktogrammPalette.TYPEN_REIHENFOLGE[i];
			panelElemente[i] = new AuswahlPanelElement(typ);
			panelElemente[i].addActionListener(e -> paletteElementGeklickt(typ));
			add(panelElemente[i], c);
			dragSource.createDefaultDragGestureRecognizer(panelElemente[i], DnDConstants.ACTION_COPY_OR_MOVE, this);
			c.gridy++;
		}

		c.insets = new Insets(6, 0, 4, 0);
		muelleimer = new JLabel();
		muelleimerIstAuf = true;
		muelleimerAuf(!muelleimerIstAuf);
		muelleimer.setHorizontalAlignment(SwingConstants.CENTER);
		muelleimer.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		muelleimer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		muelleimerTooltipSetzen();
		muelleimer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				controlling.gibAktuellesStruktogramm().markiertesElementLoeschen();
			}
		});
		add(muelleimer, c);

		c.gridy++;
		c.insets = new Insets(1, 0, 3, 0);

		palettePngButton = paletteAktionsButton(I18n.tr("palette.exportPng"));
		palettePngButton.addActionListener(e -> controlling.bildSpeichernNurPng());
		add(palettePngButton, c);
		c.gridy++;

		paletteInfoButton = paletteIconButton('\u2139', I18n.tr("palette.aboutTooltip"), I18n.tr("palette.aboutAccessible"));
		paletteInfoButton.addActionListener(e -> controlling.showInfo());
		JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
		infoRow.setOpaque(false);
		infoRow.add(paletteInfoButton);
		add(infoRow, c);
		c.gridy++;

		kopiertesStrElement = null;
		paletteDragAktiv = false;
		letzterPaletteDragEndeZeitpunkt = 0;

		c.weighty = 1000;
		c.fill = GridBagConstraints.VERTICAL;
		add(Box.createVerticalGlue(), c);

		//dropTarget = 
		new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,this, true, null);
	}

	private void paletteElementGeklickt(int typ) {
		if (paletteDragAktiv || System.currentTimeMillis() - letzterPaletteDragEndeZeitpunkt < 500) {
			return;
		}
		controlling.paletteElementEinfuegen(typ);
	}

	private static JButton paletteAktionsButton(String text) {
		JButton b = new JButton(text);
		b.setFocusable(false);
		b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		PaletteButtonStyle.apply(b);
		return b;
	}

	/** Schaltfläche nur mit Symbol; Erklärung über Tooltip und Screenreader über {@code accessibleName}. */
	private static JButton paletteIconButton(char symbol, String tooltip, String accessibleName) {
		JButton b = new JButton(String.valueOf(symbol));
		b.setToolTipText(tooltip);
		b.getAccessibleContext().setAccessibleName(accessibleName);
		b.setFocusable(false);
		b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
		b.setMargin(new Insets(2, 6, 2, 6));
		PaletteButtonStyle.apply(b);
		return b;
	}

	
	
	public void aktualisiereBeschriftungen(){
		muelleimerTooltipSetzen();
		if (palettePngButton != null) {
			palettePngButton.setText(I18n.tr("palette.exportPng"));
		}
		if (paletteInfoButton != null) {
			paletteInfoButton.setToolTipText(I18n.tr("palette.aboutTooltip"));
			paletteInfoButton.getAccessibleContext().setAccessibleName(I18n.tr("palette.aboutAccessible"));
		}
		for (AuswahlPanelElement el : panelElemente) {
			el.aktualisiereBeschriftung();
		}
		for (Component ch : getComponents()){
			if (ch instanceof JComponent){
				((JComponent) ch).revalidate();
			}
		}
		revalidate();
		repaint();
		JViewport vp = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
		if (vp != null){
			vp.revalidate();
			vp.repaint();
		}
		JScrollPane sc = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
		if (sc != null){
			sc.revalidate();
			sc.repaint();
		}
	}


	public void setzeKopiertesStrElement(Document doc){
		kopiertesStrElement = doc;
	}

	public Document gibKopiertesStrElement(){
		return kopiertesStrElement;
	}



	private void muelleimerTooltipSetzen() {
		muelleimer.setToolTipText("<html>" + I18n.tr("palette.trashDrop") + "<br>" + I18n.tr("palette.trashClick") + "</html>");
	}

	private void muelleimerAuf(boolean oeffnen){
		if (muelleimerIstAuf != oeffnen){
			muelleimerIstAuf = oeffnen;
			muelleimer.setIcon(erzeugeMuelleimerIcon(oeffnen));
		}
	}

	private static FlatSVGIcon erzeugeMuelleimerIcon(boolean aktiv) {
		FlatSVGIcon icon = new FlatSVGIcon("icons/lucide/trash-2.svg", 32, 32);
		if (aktiv) {
			java.awt.Color accent = UIManager.getColor("Component.accentColor");
			if (accent != null) {
				icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> accent));
			}
		}
		return icon;
	}


	public void kopiereGanzesStruktogramm(){
		setzeKopiertesStrElement(controlling.gibAktuellesStruktogramm().xmlErstellen());
	}




	//Methoden Drag ausgelöst
	//http://www.java2s.com/Code/Java/Swing-JFC/MakingaComponentDraggable.htm
	public void dragGestureRecognized(DragGestureEvent evt){//User hat angefangen ein Objekt zu ziehen

		// Erkenner hängt an den Element-Kacheln (JButton).
		Component quelle = evt.getComponent();

		if (quelle instanceof AuswahlPanelElement){

			int typ = ((AuswahlPanelElement) quelle).gibTyp();

			Transferable t = new StringSelection("n"+typ);

			paletteDragAktiv = true;
			dragSource.startDrag(evt, DragSource.DefaultCopyDrop, t, this);
		}
	}

	public void dragEnter(DragSourceDragEvent evt){

	}

	public void dragOver(DragSourceDragEvent evt){

	}

	public void dragExit(DragSourceEvent evt){

	}

	public void dropActionChanged(DragSourceDragEvent evt){

	}

	public void dragDropEnd(DragSourceDropEvent evt){
		paletteDragAktiv = false;
		letzterPaletteDragEndeZeitpunkt = System.currentTimeMillis();
		PaletteButtonStyle.clearPressedArmedState(evt.getDragSourceContext().getComponent());
	}











	//Drop empfangen
	//http://www.java2s.com/Code/Java/Swing-JFC/PanelDropTarget.htm
	public void drop(DropTargetDropEvent event){


		try{
			event.acceptDrop(event.getSourceActions());


			Transferable tr = event.getTransferable();
			String dragTyp = (String)tr.getTransferData(tr.getTransferDataFlavors()[0]);//Drag & Drop Transferdaten ermitteln (das ist in diesem Programm ein Buchstabe und eventuell eine Zahl dahinter (siehe: Struktogramm.drop(...))

			Component dropUeberComponent = getComponentAt(bildschirmKoordZuLokalenKoord(event.getLocation()));
			Struktogramm str = controlling.gibAktuellesStruktogramm();

			if (dragTyp.charAt(0) == 'z'){//z -> ein Drag wurde ausgelöst, indem ein StruktogrammElement aus dem Struktogramm gezogen wurde

				if(dropUeberComponent == muelleimer){
					//ein Element wurde aus dem aktuellen Struktogramm auf den Mülleimer gezogen -> aus dem Zwischenlager des Struktogramms entfernen

					str.elementAusZwischenlagerGanzEntfernen();
					str.zeichenbereichAktualisieren();
					str.zeichne();
					str.rueckgaengigPunktSetzen();

				}
			}

			muelleimerAuf(false);
			event.dropComplete(true);
		}catch (Exception e){
			e.printStackTrace();
			event.rejectDrop();
		}
	}

	public void dragExit(DropTargetEvent evt){
		muelleimerAuf(false);
	}

	public void dropActionChanged(DropTargetDragEvent evt){

	}

	public void dragEnter(DropTargetDragEvent evt){

	}

	public void dragOver(DropTargetDragEvent evt){
		Component tmp = getComponentAt(bildschirmKoordZuLokalenKoord(evt.getLocation())); //Komponente ermitteln, die unter der Maus ist

		muelleimerAuf(tmp == muelleimer); //wenn die Komponente unter der Maus das Mülleimer-Label ist, dann geöffneten Mülleimer zeigen, sonst den Geschlossenen
	}







	//die Drag & Drop Methoden liefern Mauskoordinaten für den ganzen Bildschirm, hier werden sie zu Koordinaten des AuswahlPanel konvertiert
	//siehe: http://www.tutego.de/java/articles/Absolute-Koordinaten-Swing-Element.html
	public Point bildschirmKoordZuLokalenKoord(Point bildschirmKoord){
		return new Point(bildschirmKoord.x - getX(), bildschirmKoord.y - getY());
	}



}