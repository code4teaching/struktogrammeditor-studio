package de.visustruct.view;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import de.visustruct.control.GlobalSettings;
import de.visustruct.i18n.I18n;
import de.visustruct.control.Struktogramm;
import de.visustruct.other.JNumberField;
import de.visustruct.other.JTextAreaEasy;
import de.visustruct.struktogrammelemente.CodeGenRules;

/** Dialog: Quellcode-Export als Java, Python oder JavaScript. */
public class CodeErzeuger extends JDialog {

	private static final long serialVersionUID = 6073577055724789562L;

	private ButtonGroup buttongroup = new ButtonGroup();
	private JRadioButton javaButton = new JRadioButton();
	private JRadioButton pythonButton = new JRadioButton();
	private JRadioButton javaScriptButton = new JRadioButton();
	private ButtonGroup javaOutputGroup = new ButtonGroup();
	private JRadioButton javaSnippetButton = new JRadioButton("Snippet");
	private JRadioButton javaClassButton = new JRadioButton("Class");
	private JTextAreaEasy textarea;
	private JCheckBox checkboxKommentare = new JCheckBox();
	private JLabel jLabel1 = new JLabel();
	private JNumberField numberfieldEinrueckung = new JNumberField();
	private JLabel jLabel2 = new JLabel();
	private JNumberField numberfieldZeichenzahl = new JNumberField();
	private JButton buttonCodeErzeugen = new JButton();
	/** Je nach Zielsprache: Browser (JS) oder Zwischenablage (Java/Python). */
	private JButton buttonCodeSecondary = new JButton();
	private JButton buttonCodeCopyJs = new JButton();
	private JButton buttonSchliessen = new JButton();
	private Struktogramm str;

	public static final int typJava = 0;
	/** {@code match}/{@code case} für Mehrfachauswahl; Einrückung wie bei Java. */
	public static final int typPython = 1;
	/** Klammer-Syntax wie Java; Ausgabe beginnt mit {@code "use strict";}. */
	public static final int typJavaScript = 2;

	public CodeErzeuger(JFrame owner, String title, boolean modal, Struktogramm str) {
		super(owner, title, modal);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		int frameWidth = 498;
		int frameHeight = 458;
		setSize(frameWidth, frameHeight);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (d.width - getSize().width) / 2;
		int y = (d.height - getSize().height) / 2;
		setLocation(x, y);
		Container cp = getContentPane();
		cp.setLayout(null);

		javaButton.setBounds(16, 210, 260, 17);
		javaButton.setText(I18n.tr("dialog.codeGen.targetJava"));
		cp.add(javaButton);
		pythonButton.setBounds(16, 232, 260, 17);
		pythonButton.setText(I18n.tr("dialog.codeGen.targetPython"));
		cp.add(pythonButton);
		javaScriptButton.setBounds(16, 254, 260, 17);
		javaScriptButton.setText(I18n.tr("dialog.codeGen.targetJavaScript"));
		cp.add(javaScriptButton);
		buttongroup.add(javaButton);
		buttongroup.add(pythonButton);
		buttongroup.add(javaScriptButton);
		int savedLang = GlobalSettings.getCodeErzeugerProgrammiersprache();
		if (savedLang == typPython) {
			pythonButton.setSelected(true);
		} else if (savedLang == typJavaScript) {
			javaScriptButton.setSelected(true);
		} else {
			javaButton.setSelected(true);
		}

		javaSnippetButton.setBounds(286, 210, 88, 17);
		javaClassButton.setBounds(378, 210, 88, 17);
		javaOutputGroup.add(javaSnippetButton);
		javaOutputGroup.add(javaClassButton);
		javaSnippetButton.setSelected(true);
		cp.add(javaSnippetButton);
		cp.add(javaClassButton);

		checkboxKommentare.setBounds(16, 286, 400, 17);
		checkboxKommentare.setText(I18n.tr("dialog.codeGen.emitComments"));
		checkboxKommentare.setSelected(GlobalSettings.isCodeErzeugerAlsKommentar());
		cp.add(checkboxKommentare);
		jLabel1.setBounds(16, 314, 323, 16);
		jLabel1.setText(I18n.tr("dialog.codeGen.indentFirstLine"));
		jLabel1.setFont(new Font("MS Sans Serif", Font.PLAIN, 13));
		cp.add(jLabel1);
		numberfieldEinrueckung.setBounds(344, 314, 49, 24);
		numberfieldEinrueckung.setText(""+GlobalSettings.getCodeErzeugerEinrueckungGesamt());
		cp.add(numberfieldEinrueckung);
		jLabel2.setBounds(16, 346, 300, 16);
		jLabel2.setText(I18n.tr("dialog.codeGen.spacesPerLevel"));
		jLabel2.setFont(new Font("MS Sans Serif", Font.PLAIN, 13));
		cp.add(jLabel2);
		numberfieldZeichenzahl.setBounds(344, 346, 49, 24);
		numberfieldZeichenzahl.setText(""+GlobalSettings.getCodeErzeugerEinrueckungProStufe());
		cp.add(numberfieldZeichenzahl);
		buttonCodeErzeugen.setBounds(16, 386, 120, 25);
		buttonCodeErzeugen.setText(I18n.tr("dialog.codeGen.generate"));
		buttonCodeErzeugen.setMargin(new Insets(2, 2, 2, 2));
		buttonCodeErzeugen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				buttonCodeErzeugen_ActionPerformed(evt);
			}
		});
		cp.add(buttonCodeErzeugen);
		buttonCodeSecondary.setBounds(144, 386, 228, 25);
		buttonCodeSecondary.setMargin(new Insets(2, 2, 2, 2));
		buttonCodeSecondary.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonCodeSecondary_ActionPerformed();
			}
		});
		cp.add(buttonCodeSecondary);
		buttonCodeCopyJs.setBounds(288, 386, 84, 25);
		buttonCodeCopyJs.setMargin(new Insets(2, 2, 2, 2));
		buttonCodeCopyJs.setText(I18n.tr("dialog.codeGen.copyCode"));
		buttonCodeCopyJs.setToolTipText(I18n.tr("dialog.codeGen.copyCode.tooltip"));
		buttonCodeCopyJs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copyCodeToClipboard();
			}
		});
		cp.add(buttonCodeCopyJs);
		buttonSchliessen.setBounds(380, 386, 102, 25);
		buttonSchliessen.setText(I18n.tr("dialog.codeGen.close"));
		buttonSchliessen.setMargin(new Insets(2, 2, 2, 2));
		buttonSchliessen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				buttonSchliessen_ActionPerformed(evt);
			}
		});
		cp.add(buttonSchliessen);

		java.awt.event.ItemListener sprachWahlListener = e -> {
			if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
				aktualisiereCodeSecondaryButton();
			}
		};
		javaButton.addItemListener(sprachWahlListener);
		pythonButton.addItemListener(sprachWahlListener);
		javaScriptButton.addItemListener(sprachWahlListener);

		textarea = new JTextAreaEasy(8,10,480,200);
		textarea.setzeFont(new Font("Monospaced", Font.PLAIN, 15));
		textarea.setzeContainer(cp);

		this.str = str;

		aktualisiereCodeSecondaryButton();
		setResizable(false);
		setVisible(true);
	}

	private void aktualisiereCodeSecondaryButton() {
		boolean js = javaScriptButton.isSelected();
		boolean java = javaButton.isSelected();
		javaSnippetButton.setVisible(java);
		javaClassButton.setVisible(java);
		javaSnippetButton.setEnabled(java);
		javaClassButton.setEnabled(java);
		buttonCodeSecondary.setEnabled(true);
		buttonCodeSecondary.setFocusable(true);
		if (js) {
			buttonCodeSecondary.setBounds(144, 386, 136, 25);
			buttonCodeSecondary.setText(I18n.tr("dialog.codeGen.openInBrowser"));
			buttonCodeSecondary.setToolTipText(I18n.tr("dialog.codeGen.openInBrowser.tooltip"));
			buttonCodeCopyJs.setVisible(true);
		} else {
			buttonCodeSecondary.setBounds(144, 386, 228, 25);
			buttonCodeSecondary.setText(I18n.tr("dialog.codeGen.copyCode"));
			buttonCodeSecondary.setToolTipText(I18n.tr("dialog.codeGen.copyCode.tooltip"));
			buttonCodeCopyJs.setVisible(false);
		}
	}

	private void buttonCodeSecondary_ActionPerformed() {
		if (javaScriptButton.isSelected()) {
			openJsPreviewInBrowser();
			return;
		}
		copyCodeToClipboard();
	}

	private void copyCodeToClipboard() {
		String code = textarea.gibText();
		if (code == null) {
			code = "";
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
		JOptionPane.showMessageDialog(this, I18n.tr("dialog.codeGen.copyDone.message"),
				I18n.tr("dialog.codeGen.copyDone.title"), JOptionPane.INFORMATION_MESSAGE);
	}

	private void openJsPreviewInBrowser() {
		String code = textarea.gibText();
		if (code == null || code.isBlank()) {
			JOptionPane.showMessageDialog(this, I18n.tr("dialog.codeGen.jsBrowserEmpty.message"),
					I18n.tr("dialog.codeGen.jsBrowserEmpty.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		String escaped = code.replaceAll("(?i)</script>", "<\\/script>");
		String lang = I18n.currentLocale().toLanguageTag();
		boolean german = lang.toLowerCase().startsWith("de");
		String title = german ? "VisuStruct - JavaScript-Test" : "VisuStruct - JavaScript Test";
		String hint = german
				? "Der Code laeuft im Browser. console.log-Ausgaben erscheinen unten."
				: "The code runs in the browser. console.log output appears below.";
		String html = "<!DOCTYPE html>\n"
				+ "<html lang=\"" + lang + "\">\n"
				+ "<head>\n"
				+ "<meta charset=\"UTF-8\">\n"
				+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
				+ "<title>" + escapeForHtmlText(title) + "</title>\n"
				+ "<style>\n"
				+ "body{margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#0b1220;color:#e5e7eb;}\n"
				+ "header{padding:14px 16px;background:#111827;border-bottom:1px solid rgba(255,255,255,.08);}\n"
				+ "h1{margin:0;font-size:14px;font-weight:700;letter-spacing:.01em;}\n"
				+ "main{padding:14px 16px;display:grid;gap:12px;max-width:960px;}\n"
				+ "pre{margin:0;padding:12px;background:rgba(255,255,255,.06);border:1px solid rgba(255,255,255,.1);border-radius:12px;overflow:auto;white-space:pre-wrap;}\n"
				+ "button{padding:10px 12px;border-radius:12px;border:1px solid rgba(255,255,255,.12);background:rgba(59,130,246,.22);color:#fff;font-weight:700;cursor:pointer;}\n"
				+ ".hint{font-size:12px;color:rgba(229,231,235,.75);line-height:1.45;}\n"
				+ ".code-label{font-size:12px;color:rgba(229,231,235,.65);font-weight:700;text-transform:uppercase;letter-spacing:.06em;}\n"
				+ "</style>\n"
				+ "</head>\n"
				+ "<body>\n"
				+ "<header><h1>" + escapeForHtmlText(title) + "</h1></header>\n"
				+ "<main>\n"
				+ "<div class=\"hint\">" + escapeForHtmlText(hint) + "</div>\n"
				+ "<button id=\"run\">Run</button>\n"
				+ "<div class=\"code-label\">Output</div>\n"
				+ "<pre id=\"out\"></pre>\n"
				+ "<div class=\"code-label\">Code</div>\n"
				+ "<pre id=\"code\"></pre>\n"
				+ "<script>\n"
				+ "const out=document.getElementById('out');\n"
				+ "const codeBlock=document.getElementById('code');\n"
				+ "const userCode=String.raw`" + escapeForTemplateLiteral(escaped) + "`;\n"
				+ "codeBlock.textContent=userCode;\n"
				+ "const print=(s)=>{out.textContent+=s+'\\n';};\n"
				+ "const oldLog=console.log;\n"
				+ "console.log=(...args)=>{oldLog(...args);print(args.map(a=>typeof a==='string'?a:JSON.stringify(a)).join(' '));};\n"
				+ "document.getElementById('run').addEventListener('click',()=>{out.textContent='';try{(0,eval)(userCode);}catch(e){print(String(e));}});\n"
				+ "</script>\n"
				+ "</main>\n"
				+ "</body>\n"
				+ "</html>\n";
		try {
			java.nio.file.Path temp = Files.createTempFile("visustruct-js-", ".html");
			Files.writeString(temp, html, StandardCharsets.UTF_8);
			java.io.File file = temp.toFile();
			file.deleteOnExit();
			openPreviewFile(file);
		} catch (Exception ex) {
			String detail = ex.getMessage();
			if (detail == null || detail.isBlank()) {
				detail = ex.toString();
			}
			JOptionPane.showMessageDialog(this, I18n.trf("dialog.codeGen.jsBrowserIoError.message", detail),
					I18n.tr("dialog.codeGen.jsBrowserIoError.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void openPreviewFile(File file) throws Exception {
		Exception letzterFehler = null;
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Desktop.Action.OPEN)) {
				try {
					desktop.open(file);
					return;
				} catch (Exception ex) {
					letzterFehler = ex;
				}
			}
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				try {
					desktop.browse(file.toURI());
					return;
				} catch (Exception ex) {
					letzterFehler = ex;
				}
			}
		}
		if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
			try {
				new ProcessBuilder("open", file.getAbsolutePath()).start();
				return;
			} catch (Exception ex) {
				letzterFehler = ex;
			}
		}
		if (letzterFehler != null) {
			throw letzterFehler;
		}
		JOptionPane.showMessageDialog(this, I18n.tr("dialog.codeGen.jsBrowserNoDesktop.message"),
				I18n.tr("dialog.codeGen.jsBrowserNoDesktop.title"), JOptionPane.ERROR_MESSAGE);
	}

	private static String escapeForHtmlText(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String escapeForTemplateLiteral(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("`", "\\`").replace("${", "\\${");
	}

	private static JRadioButton getSelectedRadioButton(ButtonGroup bg) {
		for (java.util.Enumeration<AbstractButton> e = bg.getElements(); e.hasMoreElements();) {
			AbstractButton b = e.nextElement();
			if (b.isSelected()) {
				return (JRadioButton) b;
			}
		}
		return null;
	}

	public void buttonCodeErzeugen_ActionPerformed(ActionEvent evt) {
		if (numberfieldEinrueckung.isNumeric() && numberfieldZeichenzahl.isNumeric()){

			JRadioButton radioB = getSelectedRadioButton(buttongroup);
			int typ = typJava;
			if (radioB == pythonButton) {
				typ = typPython;
			} else if (radioB == javaScriptButton) {
				typ = typJavaScript;
			}

			textarea.leeren();
			textarea.beginQuellcodeBatch();
			final int einrueckung = numberfieldEinrueckung.getInt();
			final int einrueckungProStufe = numberfieldZeichenzahl.getInt();
			final boolean alsKommentar = checkboxKommentare.isSelected();
			boolean batchBeendet = false;
			final boolean javaScriptGen = typ == typJavaScript;
			try {
				if (javaScriptGen) {
					textarea.hinzufuegen("\"use strict\";\n\n");
					CodeGenRules.beginJavaScriptCodeGeneration(str.gibListe());
				}
				try {
					str.gibListe().quellcodeAllerUnterelementeGenerieren(typ, einrueckung, einrueckungProStufe, alsKommentar, textarea);
					textarea.endQuellcodeBatch();
					batchBeendet = true;
					if (typ == typJava) {
						String code = javaCodeNachbearbeiten(textarea.gibText(), einrueckungProStufe);
						textarea.leeren();
						textarea.hinzufuegen(code);
					} else {
						String code = CodeGenRules.postProcessGeneratedCode(textarea.gibText(), typ);
						if (typ == typPython && CodeGenRules.pythonNeedsRandomImport(code)) {
							code = "import random\n\n" + code;
						}
						textarea.leeren();
						textarea.hinzufuegen(code);
					}
				} finally {
					if (javaScriptGen) {
						CodeGenRules.endJavaScriptCodeGeneration();
					}
				}
			} finally {
				if (!batchBeendet) {
					textarea.endQuellcodeBatch();
				}
			}
			GlobalSettings.setCodeErzeugerEinrueckungGesamt(einrueckung);
			GlobalSettings.setCodeErzeugerEinrueckungProStufe(einrueckungProStufe);
			GlobalSettings.setCodeErzeugerProgrammiersprache(typ);
			GlobalSettings.setCodeErzeugerAlsKommentar(alsKommentar);
			GlobalSettings.saveSettings();
			aktualisiereCodeSecondaryButton();
		}else{
			JOptionPane.showMessageDialog(this, I18n.tr("dialog.codeInvalidInput.message"),
					I18n.tr("dialog.codeInvalidInput.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private String javaCodeNachbearbeiten(String code, int einrueckungProStufe) {
		String body = scannerDeklarationenBereinigen(CodeGenRules.postProcessGeneratedCode(code, typJava));
		boolean scannerNoetig = CodeGenRules.javaUsesScanner(body);
		if (javaClassButton.isSelected()) {
			return javaClassCode(body, scannerNoetig, einrueckungProStufe);
		}
		if (scannerNoetig) {
			return "// import java.util.Scanner;\nScanner scanner = new Scanner(System.in);\n\n" + body;
		}
		return body;
	}

	private static String scannerDeklarationenBereinigen(String code) {
		String marker = "Scanner scanner = new Scanner(System.in);";
		String importKommentar = "// import java.util.Scanner;";
		String[] zeilen = code.split("\\R", -1);
		StringBuilder b = new StringBuilder();
		for (String zeile : zeilen) {
			if (zeile.trim().equals(importKommentar)) {
				continue;
			}
			if (zeile.trim().equals(marker)) {
				continue;
			}
			if (b.length() > 0) {
				b.append('\n');
			}
			b.append(zeile);
		}
		return b.toString();
	}

	private String javaClassCode(String body, boolean scannerNoetig, int einrueckungProStufe) {
		String indent = " ".repeat(Math.max(0, einrueckungProStufe));
		String indent2 = indent + indent;
		StringBuilder b = new StringBuilder();
		if (scannerNoetig) {
			b.append("import java.util.Scanner;\n\n");
		}
		b.append("public class ").append(javaKlassenName()).append(" {\n");
		b.append(indent).append("public static void main(String[] args) {\n");
		if (scannerNoetig) {
			b.append(indent2).append("Scanner scanner = new Scanner(System.in);\n");
			if (!body.isBlank()) {
				b.append('\n');
			}
		}
		for (String zeile : body.split("\\R", -1)) {
			if (!zeile.isEmpty()) {
				b.append(indent2).append(zeile);
			}
			b.append('\n');
		}
		b.append(indent).append("}\n");
		b.append("}\n");
		return b.toString();
	}

	private String javaKlassenName() {
		String basis = str.getStruktogrammBeschreibung();
		if (basis == null || basis.isBlank()) {
			String pfad = str.gibAktuellenSpeicherpfad();
			if (pfad != null && !pfad.isBlank()) {
				basis = new File(pfad).getName();
				int dot = basis.lastIndexOf('.');
				if (dot > 0) {
					basis = basis.substring(0, dot);
				}
			}
		}
		return javaIdentifierAusName(basis);
	}

	private static String javaIdentifierAusName(String raw) {
		if (raw == null || raw.isBlank()) {
			return "VisuStruct";
		}
		StringBuilder b = new StringBuilder();
		for (String part : raw.split("[^A-Za-z0-9_$]+")) {
			if (part.isEmpty()) {
				continue;
			}
			b.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				b.append(part.substring(1));
			}
		}
		if (b.length() == 0) {
			return "VisuStruct";
		}
		if (!Character.isJavaIdentifierStart(b.charAt(0))) {
			b.insert(0, "VisuStruct");
		}
		for (int i = 1; i < b.length(); i++) {
			if (!Character.isJavaIdentifierPart(b.charAt(i))) {
				b.setCharAt(i, '_');
			}
		}
		return b.toString();
	}

	public static String gibKommentarZeichen(boolean kommentarStart, int codeTyp) {
		if (codeTyp == typPython) {
			return "\"\"\"";
		}
		// Java und JavaScript: Blockkommentare wie in Java
		if (kommentarStart) {
			return "/*";
		}
		return "*/";
	}

	public void buttonSchliessen_ActionPerformed(ActionEvent evt) {
		setVisible(false);
	}

}
