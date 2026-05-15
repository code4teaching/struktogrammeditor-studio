package de.visustruct.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.UIManager;

import de.visustruct.control.GlobalSettings;

/**
 * UI-Texte aus {@link ResourceBundle} gemäß {@link GlobalSettings#getUiLocale()}.
 * <p>
 * Ohne spezielles {@link ResourceBundle.Control} würde {@link ResourceBundle#getBundle(String, Locale)}
 * bei fehlendem sprachspezifischen Bundle auf die <b>System-Locale</b> zurückfallen und die falsche Sprache laden.
	 * Unterstützt alle in {@link GlobalSettings#UI_LANGUAGE_OPTIONS} (21 Sprachen, wie VisuStruct-swift).
 */
public final class I18n {

	private static final String BUNDLE = "de.visustruct.i18n.Messages";
	/** Kein Fallback auf {@link Locale#getDefault()} — nur Kandidatenliste (z. B. {@code en} → {@code Messages_en} / Root). */
	private static final ResourceBundle.Control CONTROL = ResourceBundle.Control.getNoFallbackControl(
			ResourceBundle.Control.FORMAT_PROPERTIES);

	private static ResourceBundle bundle = load(Locale.ENGLISH);

	private I18n() {
	}

	private static ResourceBundle load(Locale locale) {
		ClassLoader cl = I18n.class.getClassLoader();
		if (cl == null) {
			cl = Thread.currentThread().getContextClassLoader();
		}
		if (cl == null) {
			cl = ClassLoader.getSystemClassLoader();
		}
		return ResourceBundle.getBundle(BUNDLE, locale, cl, CONTROL);
	}

	/** Bundle an die in den Einstellungen gespeicherte UI-Sprache anbinden. */
	public static void syncWithSettings() {
		bundle = load(GlobalSettings.getUiLocale());
	}

	public static String tr(String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/** Wie {@link #tr(String)}, mit {@link MessageFormat}-Platzhaltern {@code {0}}, … */
	public static String trf(String key, Object... args) {
		String pattern = tr(key);
		if (args == null || args.length == 0) {
			return pattern;
		}
		return new MessageFormat(pattern, currentLocale()).format(args);
	}

	public static Locale currentLocale() {
		return bundle.getLocale();
	}

	/**
	 * {@link javax.swing.JFileChooser}-Texte im aktuellen Bundle; nach jedem {@link UIManager#setLookAndFeel}
	 * bzw. {@link javax.swing.SwingUtilities#updateComponentTreeUI} erneut aufrufen.
	 */
	public static void applyFileChooserStrings() {
		UIManager.put("FileChooser.acceptAllFileFilterText", tr("fileChooser.acceptAll"));
		UIManager.put("FileChooser.cancelButtonText", tr("fileChooser.cancel"));
		UIManager.put("FileChooser.cancelButtonToolTipText", tr("fileChooser.cancelTooltip"));
		UIManager.put("FileChooser.directoryOpenButtonText", tr("fileChooser.open"));
		UIManager.put("FileChooser.directoryOpenButtonToolTipText", tr("fileChooser.openTooltip"));
		UIManager.put("FileChooser.fileNameHeaderText", tr("fileChooser.fileName"));
		UIManager.put("FileChooser.fileNameLabelText", tr("fileChooser.fileName"));
		UIManager.put("FileChooser.filesOfTypeLabelText", tr("fileChooser.filesOfType"));
		UIManager.put("FileChooser.helpButtonText", tr("fileChooser.help"));
		UIManager.put("FileChooser.helpButtonToolTipText", tr("fileChooser.helpTooltip"));
		UIManager.put("FileChooser.lookInLabelText", tr("fileChooser.lookIn"));
		UIManager.put("FileChooser.newFolderErrorText", tr("fileChooser.newFolderError"));
		UIManager.put("FileChooser.openButtonText", tr("fileChooser.open"));
		UIManager.put("FileChooser.openButtonToolTipText", tr("fileChooser.openTooltip"));
		UIManager.put("FileChooser.openDialogTitleText", tr("fileChooser.openTitle"));
		UIManager.put("FileChooser.saveButtonText", tr("fileChooser.save"));
		UIManager.put("FileChooser.saveButtonToolTipText", tr("fileChooser.saveTooltip"));
		UIManager.put("FileChooser.saveDialogTitleText", tr("fileChooser.saveTitle"));
		UIManager.put("FileChooser.saveInLabelText", tr("fileChooser.saveIn"));
		UIManager.put("FileChooser.updateButtonText", tr("fileChooser.update"));
		UIManager.put("FileChooser.updateButtonToolTipText", tr("fileChooser.updateTooltip"));
		UIManager.put("FileChooser.newFolderButtonText", tr("fileChooser.newFolder"));
		UIManager.put("FileChooser.newFolderButtonToolTipText", tr("fileChooser.newFolderTooltip"));
	}
}
