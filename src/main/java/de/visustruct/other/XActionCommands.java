package de.visustruct.other;

public enum XActionCommands {
	neu, oeffnen, speichern, speicherUnter, bildSpeichern, bildDrucken, bildInZwischenAblage, quellcodeErzeugen,
	struktogrammSchliessen, programmBeenden,
	simulationToggle,
	
	rueckgaengig, widerrufen, ganzesStruktogrammKopieren,
	
	letztesElementStrecken, schriftartAendern,
	zoomeinstellungen, vergroesserungenRuckgaengigMachen,
	elementShortcutsVerwenden,

	/** Dialog „Beschriftung (Struktogramm)“ / Textvorlagen für neue Blöcke. */
	elementBeschriftungEinstellen,
	
	info,
	
	lookAndFeelOSStandard, lookAndFeelSwingStandard, lookAndFeelNimbus,
	lookAndFeelFlatLight, lookAndFeelFlatDark, struktogrammbeschreibungHinzufuegen,

	/** UI-Sprache Englisch (Menü „Einstellungen → Sprachen“). */
	languageEnglish,
	/** UI-Sprache Deutsch. */
	languageGerman,
	/** UI-Sprache Português (Portugal). */
	languagePortuguesePortugal

}
