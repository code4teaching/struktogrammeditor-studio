package de.visustruct.i18n

import de.visustruct.control.GlobalSettings
import java.text.MessageFormat
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import javax.swing.UIManager

/**
 * UI-Texte aus [ResourceBundle] gemäß [GlobalSettings.getUiLocale].
 *
 * Ohne spezielles [ResourceBundle.Control] würde [ResourceBundle.getBundle] bei fehlendem
 * sprachspezifischen Bundle auf die System-Locale zurückfallen.
 */
object I18n {

    private const val BUNDLE = "de.visustruct.i18n.Messages"

    /** Kein Fallback auf [Locale.getDefault] — nur Kandidatenliste. */
    private val CONTROL: ResourceBundle.Control = ResourceBundle.Control.getNoFallbackControl(
        ResourceBundle.Control.FORMAT_PROPERTIES,
    )

    private var bundle: ResourceBundle = load(Locale.ENGLISH)

    private fun load(locale: Locale): ResourceBundle {
        val cl = I18n::class.java.classLoader
            ?: Thread.currentThread().contextClassLoader
            ?: ClassLoader.getSystemClassLoader()
        return ResourceBundle.getBundle(BUNDLE, locale, cl, CONTROL)
    }

    /** Bundle an die in den Einstellungen gespeicherte UI-Sprache anbinden. */
    @JvmStatic
    fun syncWithSettings() {
        bundle = load(GlobalSettings.getUiLocale())
    }

    @JvmStatic
    fun tr(key: String): String =
        try {
            bundle.getString(key)
        } catch (_: MissingResourceException) {
            key
        }

    /** Wie [tr], mit [MessageFormat]-Platzhaltern `{0}`, … */
    @JvmStatic
    fun trf(key: String, vararg args: Any?): String {
        val pattern = tr(key)
        if (args.isEmpty()) {
            return pattern
        }
        return MessageFormat(pattern, currentLocale()).format(args)
    }

    @JvmStatic
    fun currentLocale(): Locale = bundle.locale

    /**
     * [javax.swing.JFileChooser]-Texte im aktuellen Bundle; nach jedem [UIManager.setLookAndFeel]
     * bzw. [javax.swing.SwingUtilities.updateComponentTreeUI] erneut aufrufen.
     */
    @JvmStatic
    fun applyFileChooserStrings() {
        UIManager.put("FileChooser.acceptAllFileFilterText", tr("fileChooser.acceptAll"))
        UIManager.put("FileChooser.cancelButtonText", tr("fileChooser.cancel"))
        UIManager.put("FileChooser.cancelButtonToolTipText", tr("fileChooser.cancelTooltip"))
        UIManager.put("FileChooser.directoryOpenButtonText", tr("fileChooser.open"))
        UIManager.put("FileChooser.directoryOpenButtonToolTipText", tr("fileChooser.openTooltip"))
        UIManager.put("FileChooser.fileNameHeaderText", tr("fileChooser.fileName"))
        UIManager.put("FileChooser.fileNameLabelText", tr("fileChooser.fileName"))
        UIManager.put("FileChooser.filesOfTypeLabelText", tr("fileChooser.filesOfType"))
        UIManager.put("FileChooser.helpButtonText", tr("fileChooser.help"))
        UIManager.put("FileChooser.helpButtonToolTipText", tr("fileChooser.helpTooltip"))
        UIManager.put("FileChooser.lookInLabelText", tr("fileChooser.lookIn"))
        UIManager.put("FileChooser.newFolderErrorText", tr("fileChooser.newFolderError"))
        UIManager.put("FileChooser.openButtonText", tr("fileChooser.open"))
        UIManager.put("FileChooser.openButtonToolTipText", tr("fileChooser.openTooltip"))
        UIManager.put("FileChooser.openDialogTitleText", tr("fileChooser.openTitle"))
        UIManager.put("FileChooser.saveButtonText", tr("fileChooser.save"))
        UIManager.put("FileChooser.saveButtonToolTipText", tr("fileChooser.saveTooltip"))
        UIManager.put("FileChooser.saveDialogTitleText", tr("fileChooser.saveTitle"))
        UIManager.put("FileChooser.saveInLabelText", tr("fileChooser.saveIn"))
        UIManager.put("FileChooser.updateButtonText", tr("fileChooser.update"))
        UIManager.put("FileChooser.updateButtonToolTipText", tr("fileChooser.updateTooltip"))
        UIManager.put("FileChooser.newFolderButtonText", tr("fileChooser.newFolder"))
        UIManager.put("FileChooser.newFolderButtonToolTipText", tr("fileChooser.newFolderTooltip"))
    }
}
