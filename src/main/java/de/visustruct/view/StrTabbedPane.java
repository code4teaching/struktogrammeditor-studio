package de.visustruct.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.visustruct.control.Controlling;
import de.visustruct.control.Struktogramm;
import de.visustruct.i18n.I18n;

public class StrTabbedPane extends JTabbedPane implements ChangeListener{

	private static final String CLIENT_TAB_TITLE_LABEL = "visustruct.tabTitleLabel";
	private static final String CLIENT_TAB_DIRTY_LABEL = "visustruct.tabDirtyLabel";

	private static final long serialVersionUID = 1L;
	private Controlling controlling;

   public StrTabbedPane(Controlling controlling){
      super(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
      this.controlling = controlling;
      setBorder(BorderFactory.createEmptyBorder(8, 6, 10, 10));

      addChangeListener(this);
      addKeyListener(controlling);

      addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            int idx = indexAtLocation(e.getX(), e.getY());
            if (idx < 0) {
               return;
            }
            if (SwingUtilities.isMiddleMouseButton(e)) {
               schliesseTab(idx);
               return;
            }
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
               renameTabAt(idx);
            }
         }

         @Override
         public void mousePressed(MouseEvent e) {
            maybeShowTabPopup(e);
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            maybeShowTabPopup(e);
         }

         private void maybeShowTabPopup(MouseEvent e) {
            if (!e.isPopupTrigger()) {
               return;
            }
            int idx = indexAtLocation(e.getX(), e.getY());
            if (idx < 0) {
               return;
            }
            JPopupMenu menu = new JPopupMenu();
            JMenuItem closeItem = new JMenuItem(I18n.tr("menu.file.closeDiagram"));
            closeItem.addActionListener(a -> schliesseTab(idx));
            menu.add(closeItem);
            menu.show(StrTabbedPane.this, e.getX(), e.getY());
         }
      });
   }


   /**
    * Doppelklick auf den Reiter: Namen ändern. Ungespeichert-Kennzeichen (Punkt) bleibt erhalten.
    * Ohne gespeicherte Datei: danach Speicherdialog (Ordner und Dateiname), damit der Speicherort gleich gewählt werden kann.
    */
   private void renameTabAt(int index) {
      String current = getTitleAt(index);
      boolean dirty = isTabTitleDirty(index);

      String neu = (String) JOptionPane.showInputDialog(controlling.getGUI(),
            I18n.tr("dialog.renameTab.message"), I18n.tr("dialog.renameTab.title"), JOptionPane.PLAIN_MESSAGE,
            null, null, current);
      if (neu == null) {
         return;
      }
      neu = neu.trim();
      if (neu.isEmpty()) {
         return;
      }
      if (neu.length() > 200) {
         neu = neu.substring(0, 200);
      }

      setTitleAt(index, neu);
      setTabDirtyUI(index, dirty);
      Struktogramm str = gibStruktogrammAt(index);
      if (str != null && str.gibAktuellenSpeicherpfad().isEmpty()) {
         str.setVorgeschlagenenSpeicherBasisnamen(neu);
      }
      controlling.titelleisteAktualisieren();

      setSelectedIndex(index);
      if (str != null && str.gibAktuellenSpeicherpfad().isEmpty()) {
         // Nach dem Umbenennen-Dialog erst im nächsten EDT-Takt speichern (sonst bleibt der Speicherdialog unter macOS oft ohne Wirkung).
         SwingUtilities.invokeLater(() -> controlling.speichern(false));
      }
   }


   private Struktogramm gibStruktogrammAt(int index) {
      if (index < 0 || index >= getTabCount()) {
         return null;
      }
      Component c = getComponentAt(index);
      if (!(c instanceof JScrollPane)) {
         return null;
      }
      Component view = ((JScrollPane) c).getViewport().getView();
      return (view instanceof Struktogramm) ? (Struktogramm) view : null;
   }


   /** Tab-Index des gegebenen Struktogramms (nicht abhängig vom ausgewählten Reiter). */
   public int indexOfStruktogramm(Struktogramm str) {
      if (str == null) {
         return -1;
      }
      for (int i = 0; i < getTabCount(); i++) {
         if (gibStruktogrammAt(i) == str) {
            return i;
         }
      }
      return -1;
   }


   public void titelFuerStruktogrammSetzen(Struktogramm str, String titel) {
      int i = indexOfStruktogramm(str);
      if (i >= 0 && titel != null) {
         boolean dirty = isTabTitleDirty(i);
         setTitleAt(i, titel);
         setTabDirtyUI(i, dirty);
      }
   }


   public void titelFuerStruktogrammBearbeitetMarkieren(Struktogramm str, boolean bearbeitet) {
      int i = indexOfStruktogramm(str);
      if (i < 0) {
         return;
      }
      String titel = getTitleAt(i);
      if (titel.endsWith("*")) {
         titel = titel.substring(0, titel.length() - 1);
         setTitleAt(i, titel);
      }
      if (bearbeitet) {
         if (titel.isEmpty()) {
            setTitleAt(i, I18n.tr("tab.untitled"));
         }
         setTabDirtyUI(i, true);
      } else {
         setTabDirtyUI(i, false);
      }
   }
   
   
   public GUI gibGUI(){
	   return controlling.getGUI();
   }

   public Struktogramm struktogrammHinzufuegen(){
      Struktogramm str = new Struktogramm(this);
      JScrollPane scroll = new JScrollPane(str);
      Color bc = UIManager.getColor("Component.borderColor");
      if (bc == null) {
         bc = UIManager.getColor("controlShadow");
      }
      if (bc == null) {
         bc = Color.LIGHT_GRAY;
      }
      scroll.setBorder(BorderFactory.createLineBorder(bc, 1, true));
      add(I18n.tr("tab.untitled"), scroll);
      installClosableTabHeader(getTabCount() - 1);
      setSelectedIndex(getTabCount() -1); //...weil es sonst in graphicsInitialisieren Probleme gibt
      return str;
   }

	@Override
	public void setTitleAt(int index, String title) {
		String t = title == null ? "" : title;
		if (t.endsWith("*")) {
			t = t.substring(0, t.length() - 1);
		}
		super.setTitleAt(index, t);
		Component tc = getTabComponentAt(index);
		if (tc instanceof JPanel p) {
			Object o = p.getClientProperty(CLIENT_TAB_TITLE_LABEL);
			if (o instanceof JLabel lab) {
				lab.setText(t);
			}
		}
	}

	/**
	 * Reiter mit sichtbarem Titel ({@link JLabel}) und Schließen-Button (×); Titel bleibt über
	 * {@link #setTitleAt(int, String)} synchron.
	 */
	private void installClosableTabHeader(int index) {
		if (index < 0 || index >= getTabCount()) {
			return;
		}
		JPanel tabBar = new JPanel(new BorderLayout(4, 0));
		tabBar.setOpaque(false);

		JLabel dirtyLabel = new JLabel("");
		dirtyLabel.setOpaque(false);
		dirtyLabel.setVisible(false);
		dirtyLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		dirtyLabel.setFont(dirtyLabel.getFont().deriveFont(Font.BOLD, 12f));
		tabBar.putClientProperty(CLIENT_TAB_DIRTY_LABEL, dirtyLabel);

		JLabel titleLabel = new JLabel(getTitleAt(index));
		titleLabel.setOpaque(false);
		tabBar.putClientProperty(CLIENT_TAB_TITLE_LABEL, titleLabel);

		JLabel closeLbl = new JLabel("\u00D7");
		closeLbl.setFont(closeLbl.getFont().deriveFont(Font.BOLD, 15f));
		closeLbl.setToolTipText(I18n.tr("tab.close.tooltip"));
		closeLbl.getAccessibleContext().setAccessibleName(I18n.tr("tab.close.a11y"));
		closeLbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		closeLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		closeLbl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				e.consume();
				int i = indexOfTabComponent(tabBar);
				if (i >= 0) {
					schliesseTab(i);
				}
			}
		});

		tabBar.add(dirtyLabel, BorderLayout.WEST);
		tabBar.add(titleLabel, BorderLayout.CENTER);
		tabBar.add(closeLbl, BorderLayout.EAST);

		MouseAdapter headerMouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
					int i = indexOfTabComponent(tabBar);
					if (i >= 0 && getSelectedIndex() != i) {
						setSelectedIndex(i);
					}
				}
				maybeTabHeaderPopup(e, tabBar);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeTabHeaderPopup(e, tabBar);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				int i = indexOfTabComponent(tabBar);
				if (i < 0) {
					return;
				}
				if (SwingUtilities.isMiddleMouseButton(e)) {
					schliesseTab(i);
					e.consume();
					return;
				}
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
					renameTabAt(i);
					e.consume();
				}
			}
		};
		tabBar.addMouseListener(headerMouse);
		titleLabel.addMouseListener(headerMouse);
		dirtyLabel.addMouseListener(headerMouse);

		setTabComponentAt(index, tabBar);
	}

	private static void applyDirtyDotForeground(JLabel dirtyLab) {
		Color c = UIManager.getColor("Component.accentColor");
		if (c == null) {
			c = UIManager.getColor("TabbedPane.focusColor");
		}
		if (c == null) {
			c = UIManager.getColor("TabbedPane.selectedForeground");
		}
		if (c == null) {
			c = UIManager.getColor("Label.foreground");
		}
		if (c == null) {
			c = new Color(0x33, 0x66, 0xCC);
		}
		dirtyLab.setForeground(c);
	}

	private void setTabDirtyUI(int index, boolean dirty) {
		if (index < 0 || index >= getTabCount()) {
			return;
		}
		Component tc = getTabComponentAt(index);
		if (!(tc instanceof JPanel tabBar)) {
			return;
		}
		Object o = tabBar.getClientProperty(CLIENT_TAB_DIRTY_LABEL);
		if (!(o instanceof JLabel dirtyLab)) {
			return;
		}
		if (dirty) {
			dirtyLab.setText("\u25CF");
			applyDirtyDotForeground(dirtyLab);
			dirtyLab.setVisible(true);
			dirtyLab.getAccessibleContext().setAccessibleName(I18n.tr("tab.unsaved.a11y"));
		} else {
			dirtyLab.setText("");
			dirtyLab.setVisible(false);
			dirtyLab.getAccessibleContext().setAccessibleName("");
		}
		tabBar.revalidate();
	}

	/** Nach Theme-Wechsel: Punktfarbe an aktuelles UI anpassen. */
	public void refreshTabHeaderDirtyAppearance() {
		for (int i = 0; i < getTabCount(); i++) {
			if (isTabTitleDirty(i)) {
				Component tc = getTabComponentAt(i);
				if (tc instanceof JPanel tabBar) {
					Object o = tabBar.getClientProperty(CLIENT_TAB_DIRTY_LABEL);
					if (o instanceof JLabel dirtyLab) {
						applyDirtyDotForeground(dirtyLab);
					}
				}
			}
		}
	}

	private void maybeTabHeaderPopup(MouseEvent e, JPanel tabBar) {
		if (!e.isPopupTrigger()) {
			return;
		}
		int idx = indexOfTabComponent(tabBar);
		if (idx < 0) {
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		JMenuItem closeItem = new JMenuItem(I18n.tr("menu.file.closeDiagram"));
		closeItem.addActionListener(a -> schliesseTab(idx));
		menu.add(closeItem);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}
   
   
   public void titelDerAktuellenSeiteSetzen(String titel){
      Struktogramm s = gibAktuellesStruktogramm();
      if (s != null) {
         titelFuerStruktogrammSetzen(s, titel);
      }
   }
   
   public void titelDerAktuellenSeiteAlsBearbeitetOderAlsGespespeichertMarkieren(boolean bearbeitet){
      Struktogramm dokumentStr = gibAktuellesStruktogramm();
      if (dokumentStr != null) {
         titelFuerStruktogrammBearbeitetMarkieren(dokumentStr, bearbeitet);
      }
   }
   
   
   /**
    * Aktuellen Reiter schließen (Menü „Diagramm schließen“) — siehe {@link #schliesseTab(int)}.
    */
   public void aktuellesStruktogrammschliessen() {
      schliesseTab(getSelectedIndex());
   }

   /**
    * Reiter an {@code index} schließen. Ohne ungespeicherte Änderungen sofort entfernen; sonst wie bisher
    * nachfragen. Nach dem letzten Reiter wird automatisch ein neues Diagramm angelegt.
    * <p>Zusätzlich: Mittelklick auf Reiter, Kontextmenü (rechte Maustaste) → „Diagramm schließen“.</p>
    */
   public void schliesseTab(int index) {
      if (index < 0 || index >= getTabCount()) {
         return;
      }
      if (!isTabTitleDirty(index)) {
         removeTabEnsureMinimum(index);
         return;
      }
      setSelectedIndex(index);
      Object[] opts = {
            I18n.tr("dialog.saveBeforeClose.save"),
            I18n.tr("dialog.saveBeforeClose.dontSave"),
            I18n.tr("dialog.saveBeforeClose.cancel"),
      };
      int r = JOptionPane.showOptionDialog(controlling.getGUI(),
            I18n.tr("dialog.saveBeforeClose.message"),
            I18n.tr("dialog.saveBeforeClose.title"),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, opts, opts[2]);
      if (r == JOptionPane.CLOSED_OPTION || r == 2) {
         return;
      }
      if (r == 0) {
         controlling.speichern(false);
         if (isTabTitleDirty(index)) {
            return;
         }
      }
      removeTabEnsureMinimum(index);
   }

   private boolean isTabTitleDirty(int index) {
      if (index < 0 || index >= getTabCount()) {
         return false;
      }
      Component tc = getTabComponentAt(index);
      if (tc instanceof JPanel tabBar) {
         Object o = tabBar.getClientProperty(CLIENT_TAB_DIRTY_LABEL);
         if (o instanceof JLabel dirtyLab && dirtyLab.isVisible() && !dirtyLab.getText().isEmpty()) {
            return true;
         }
      }
      String t = getTitleAt(index);
      return !t.isEmpty() && t.endsWith("*");
   }

   private void removeTabEnsureMinimum(int index) {
      if (index < 0 || index >= getTabCount()) {
         return;
      }
      removeTabAt(index);
      if (getTabCount() == 0) {
         controlling.neuesStruktogramm();
      }
      controlling.titelleisteAktualisieren();
   }
   
   
   /** Alle geöffneten Diagramme nach Theme-Wechsel neu einfärben und zeichnen. */
   public void refreshAllStruktogrammeNachThemeWechsel() {
      for (int i = 0; i < getTabCount(); i++) {
         Struktogramm str = gibStruktogrammAt(i);
         if (str != null) {
            str.refreshAfterThemeChange();
         }
      }
      refreshTabHeaderDirtyAppearance();
   }


   public Struktogramm gibAktuellesStruktogramm(){
      if(getTabCount() > 0){
         /*getSelectedComponent() liefert das JScrollPane,
           dieses liefert mit getComponents()[0] seinen JViewPort,
           dieser liefert mit getComponents()[0] dann das Struktogramm*/
         return (Struktogramm)(((JViewport)(((JScrollPane)getSelectedComponent()).getComponents())[0]).getComponents())[0];
      }else{
         return null;
      }
   }
   
   
   public void stateChanged(ChangeEvent e){
	   controlling.titelleisteAktualisieren();
   }
   
   
   public boolean einOderMehrereStruktogrammeNichtGespeichert(){
      for(int i=0; i < getTabCount(); i++){
         if (isTabTitleDirty(i)) {
            return true;
         }
      }
      
      return false;
   }
}