package de.visustruct.view;
import java.io.File;

import javax.swing.filechooser.FileFilter;

//FileFilter für Dateitypen zum Abspeichern des Struktogramms und Dateitypen zum Abspeichern als Bilddatei.
//Diagramme: .visustruct (aktuell), .xml; .strk = Legacy-Format des Original-Struktogrammeditors (öffnen + optional speichern).
//http://www.java2s.com/Code/JavaAPI/javax.swing/JFileChoosersetFileFilterFileFilterfilter.htm
public class StrFileFilter extends FileFilter {
	private int filtertyp;
	public static final int filterAlleSpeicherdateien = 0;
	/** Standard-Erweiterung für VisuStruct (Inhalt weiterhin XML). */
	public static final int filterStruktogrammStudio = 8;

	/** Standard-Endung für VisuStruct-Diagrammdateien (Inhalt XML). */
	public static final String EXTENSION_VISUSTRUCT = ".visustruct";
	/** Original-Struktogrammeditor ({@code .strk}). */
	public static final int filterLegacyStrk = 1;
	private static final int filterAlleBilddateien = 3;

	public StrFileFilter(int filtertyp){
		this.filtertyp = filtertyp;
	}

	//accept(...)-Methode überschreiben, damit der JFileChooser weis, ob er eine bestimmte Datei anzeigen soll
	public boolean accept(File f){
		return f.isDirectory() || dateiAkzeptiert(f.getAbsolutePath());//Ordner anzeigen und Dateien anzeigen die durch dateiAkzeptiert(...) akzeptiert werden
	}


	//Beschreibungen für die einzelnen Filtertypen
	public String getDescription(){
		switch(filtertyp){
		case filterStruktogrammStudio: return "VisuStruct (*" + EXTENSION_VISUSTRUCT + ")";
		case filterAlleSpeicherdateien: return "Diagram files (*" + EXTENSION_VISUSTRUCT + ", *.xml, legacy *.strk)";
		case filterLegacyStrk: return "Legacy .strk (original editor)";
		case 2: return "XML (*.xml)";
		case filterAlleBilddateien: return "Image files";
		case 4: return "BMP images";
		case 5: return "GIF images";
		case 6: return "JPEG images";
		case 7: return "PNG images";
		default: return "";
		}
	}


	private String gibAktuelleErweiterung(){//Dateierweiterung bei diesem Filter
		switch(filtertyp){
		case filterStruktogrammStudio: return EXTENSION_VISUSTRUCT;
		case filterAlleSpeicherdateien: return EXTENSION_VISUSTRUCT;
		case filterLegacyStrk: return ".strk";
		case 2: return ".xml";
		case filterAlleBilddateien: return ".png";
		case 4: return ".bmp";
		case 5: return ".gif";
		case 6: return ".jpg";
		case 7: return ".png";
		default: return "";
		}
	}


	public String erweiterungBeiBedarfAnhaengen(String pfad){
		/*if (!pfad.endsWith(gibAktuelleErweiterung())){//wenn der Pfad nicht mit der richtigen Dateierweiterung endet...
         return pfad + gibAktuelleErweiterung();//...wird diese angehangen
      }else{
         return pfad;
      }*/

		if(dateiAkzeptiert(pfad)){
			return pfad;
		}else{
			return pfad + gibAktuelleErweiterung();
		}
	}


	/**
	 * Wenn der Name noch keine der üblichen Struktogramm-Speicherendungen hat, wird {@link #EXTENSION_VISUSTRUCT} angehängt
	 * (z. B. wenn im Speichern-Dialog „Alle Dateien“ gewählt ist).
	 */
	public static String haengeStandardSpeicherendungAnFallsNoetig(String pfad){
		String s = pfad.toLowerCase();
		if (s.endsWith(EXTENSION_VISUSTRUCT) || s.endsWith(".xml") || s.endsWith(".strk")){
			return pfad;
		}
		return pfad + EXTENSION_VISUSTRUCT;
	}


	private boolean dateiAkzeptiert(String pfad){
		pfad = pfad.toLowerCase();
		switch(filtertyp){
		case filterAlleSpeicherdateien:
			return pfad.endsWith(EXTENSION_VISUSTRUCT) || pfad.endsWith(".xml") || pfad.endsWith(".strk");
		case filterAlleBilddateien: return pfad.endsWith(".bmp") || pfad.endsWith(".gif") || pfad.endsWith(".jpg") || pfad.endsWith(".jpeg") || pfad.endsWith(".png"); //wenn der Filter "Bilddateien" ist, werden alle Bilddateien akzeptiert
		default: return pfad.endsWith(gibAktuelleErweiterung()); //es werden nur die Dateien mit genau der ausgesuchten Endung akzeptiert
		}
	}

}