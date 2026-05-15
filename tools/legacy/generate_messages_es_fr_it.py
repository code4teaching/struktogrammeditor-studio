#!/usr/bin/env python3
"""
Generate Messages_es.properties, Messages_fr.properties, Messages_it.properties
from Messages_en.properties with complete professional UI translations.

Also adds menu.settings.language.{es,fr,it} to existing bundles (en, de, pt_PT, root).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
I18N_DIR = REPO_ROOT / "src/main/resources/de/visustruct/i18n"
SOURCE = I18N_DIR / "Messages_en.properties"

LOCALE_HEADERS = {
    "es": "# Spanish",
    "fr": "# French",
    "it": "# Italian",
}

SECTION_COMMENTS = {
    "es": {
        "# Structure block names (didactic; order = element types 0–9)": (
            "# Nombres didácticos de los bloques de estructura (orden = tipos de elemento 0–9)"
        ),
        "# Short palette labels (interface language preset)": (
            "# Etiquetas cortas de la paleta (idioma de la interfaz)"
        ),
    },
    "fr": {
        "# Structure block names (didactic; order = element types 0–9)": (
            "# Noms didactiques des blocs de structure (ordre = types d'élément 0–9)"
        ),
        "# Short palette labels (interface language preset)": (
            "# Libellés courts de la palette (langue de l'interface)"
        ),
    },
    "it": {
        "# Structure block names (didactic; order = element types 0–9)": (
            "# Nomi didattici dei blocchi di struttura (ordine = tipi di elemento 0–9)"
        ),
        "# Short palette labels (interface language preset)": (
            "# Etichette brevi della tavolozza (lingua dell'interfaccia)"
        ),
    },
}

# (key, Spanish, French, Italian)
_ENTRIES: list[tuple[str, str, str, str]] = [
    ("menu.file", "Archivo", "Fichier", "File"),
    ("menu.edit", "Editar", "Modifier", "Modifica"),
    ("menu.settings", "Configuración", "Paramètres", "Impostazioni"),
    ("menu.file.new", "Nuevo", "Nouveau", "Nuovo"),
    ("menu.file.open", "Abrir…", "Ouvrir…", "Apri…"),
    ("menu.file.openRecent", "Abrir reciente", "Ouvrir récent", "Apri recente"),
    ("menu.file.openRecent.empty", "(sin archivos recientes)", "(aucun fichier récent)", "(nessun file recente)"),
    ("menu.file.save", "Guardar", "Enregistrer", "Salva"),
    ("menu.file.saveAs", "Guardar como…", "Enregistrer sous…", "Salva con nome…"),
    ("menu.file.saveImage", "Guardar como imagen…", "Enregistrer comme image…", "Salva come immagine…"),
    ("menu.file.print", "Imprimir…", "Imprimer…", "Stampa…"),
    ("menu.file.copyImage", "Copiar imagen al portapapeles", "Copier l'image dans le presse-papiers", "Copia immagine negli appunti"),
    ("menu.file.generateCode", "Generar código fuente…", "Générer le code source…", "Genera codice sorgente…"),
    ("menu.file.closeDiagram", "Cerrar diagrama", "Fermer le diagramme", "Chiudi diagramma"),
    ("menu.file.about", "Acerca de", "À propos", "Informazioni su"),
    ("menu.file.exit", "Salir", "Quitter", "Esci"),
    ("menu.edit.undo", "Deshacer", "Annuler", "Annulla"),
    ("menu.edit.redo", "Rehacer", "Rétablir", "Ripeti"),
    ("menu.edit.caption", "Título del diagrama…", "Légende du diagramme…", "Didascalia del diagramma…"),
    ("menu.edit.copyDiagram", "Copiar diagrama completo", "Copier le diagramme entier", "Copia intero diagramma"),
    ("menu.edit.simulation", "Simulación…", "Simulation…", "Simulazione…"),
    ("menu.edit.diagramMode", "Editar diagrama…", "Modifier le diagramme…", "Modifica diagramma…"),
    ("simulation.step", "Paso", "Étape", "Passo"),
    ("simulation.controls", "Controles", "Commandes", "Controlli"),
    ("simulation.play", "Reproducir", "Lecture", "Avvia"),
    ("simulation.pause", "Pausa", "Pause", "Pausa"),
    ("simulation.stop", "Detener", "Arrêter", "Stop"),
    (
        "simulation.play.tooltip",
        "Ejecutar pasos automáticamente (velocidad y resaltado en Configuración; se pausa si hace falta entrada)",
        "Exécuter les étapes automatiquement (vitesse et surbrillance dans Paramètres ; pause si une saisie est requise)",
        "Esegue i passi automaticamente (velocità e evidenziazione in Impostazioni; si mette in pausa se serve un input)",
    ),
    ("simulation.pause.tooltip", "Pausar la ejecución automática", "Mettre en pause l'exécution automatique", "Metti in pausa l'esecuzione automatica"),
    ("simulation.stop.tooltip", "Reiniciar la simulación desde el principio", "Recommencer la simulation depuis le début", "Riavvia la simulazione dall'inizio"),
    ("simulation.highlight", "Resaltado", "Surbrillance", "Evidenziazione"),
    ("simulation.highlight.lastStep", "Último paso", "Dernière étape", "Ultimo passo"),
    ("simulation.highlight.nextStep", "Siguiente paso", "Étape suivante", "Passo successivo"),
    ("simulation.back", "Volver al diagrama", "Retour au diagramme", "Torna al diagramma"),
    ("simulation.variables", "Variables", "Variables", "Variabili"),
    ("simulation.output", "Salida", "Sortie", "Output"),
    ("simulation.path", "Bloque actual", "Bloc actuel", "Blocco corrente"),
    ("simulation.input", "Entrada", "Saisie", "Input"),
    ("simulation.input.idle", "No se requiere entrada en este paso", "Aucune saisie requise à cette étape", "Nessun input richiesto in questo passo"),
    ("simulation.input.submit", "Enviar", "Valider", "Invia"),
    ("simulation.input.submit.tooltip", "Enviar el valor introducido y continuar", "Envoyer la valeur saisie et continuer", "Invia il valore inserito e continua"),
    ("simulation.back.tooltip", "Volver al editor del diagrama", "Retour à l'éditeur de diagramme", "Torna all'editor del diagramma"),
    ("simulation.step.tooltip", "Ejecutar exactamente un paso de la simulación", "Exécuter exactement une étape de simulation", "Esegue esattamente un passo della simulazione"),
    ("simulation.error.title", "Simulación", "Simulation", "Simulazione"),
    ("menu.settings.stretch", "Estirar el último bloque si es necesario", "Étirer le dernier bloc si nécessaire", "Allunga l'ultimo blocco se necessario"),
    ("menu.settings.theme", "Tema", "Thème", "Tema"),
    ("menu.settings.theme.modernLight", "Moderno · claro", "Moderne · clair", "Moderno · chiaro"),
    ("menu.settings.theme.modernDark", "Moderno · oscuro", "Moderne · sombre", "Moderno · scuro"),
    ("menu.settings.theme.osDefault", "Predeterminado del sistema", "Par défaut du système", "Predefinito di sistema"),
    ("menu.settings.theme.swingDefault", "Predeterminado de Java Swing", "Par défaut Java Swing", "Predefinito Java Swing"),
    ("menu.settings.theme.nimbus", "Nimbus", "Nimbus", "Nimbus"),
    ("menu.settings.languages", "Idiomas", "Langues", "Lingue"),
    ("menu.settings.language.en", "Inglés", "Anglais", "Inglese"),
    ("menu.settings.language.de", "Alemán", "Allemand", "Tedesco"),
    ("menu.settings.language.pt", "Portugués", "Portugais", "Portoghese"),
    ("menu.settings.labelsStruktogramm", "Etiquetas (diagrama de Nassi-Shneiderman)", "Libellés (organigramme)", "Etichette (diagramma di struttura)"),
    ("menu.settings.changeFont", "Cambiar fuente…", "Changer la police…", "Cambia carattere…"),
    ("menu.settings.mouseWheel", "Cambiar tamaño de bloques con la rueda del ratón", "Redimensionner les blocs avec la molette", "Ridimensiona i blocchi con la rotella del mouse"),
    ("menu.settings.zoom", "Configuración de zoom…", "Paramètres de zoom…", "Impostazioni zoom…"),
    ("menu.settings.resetSizes", "Restablecer tamaños de todos los bloques", "Réinitialiser toutes les tailles de blocs", "Reimposta dimensioni di tutti i blocchi"),
    ("menu.settings.shortcuts", "Usar atajos de teclado para insertar bloques", "Utiliser les raccourcis clavier pour insérer des blocs", "Usa scorciatoie da tastiera per inserire blocchi"),
    ("dialog.diagramCaption", "Título del diagrama", "Légende du diagramme", "Didascalia del diagramma"),
    ("dialog.exitUnsaved.title", "Cambios sin guardar", "Modifications non enregistrées", "Modifiche non salvate"),
    (
        "dialog.exitUnsaved.message",
        "Uno o más diagramas tienen cambios sin guardar.\n¿Salir sin guardar?",
        "Un ou plusieurs diagrammes contiennent des modifications non enregistrées.\nQuitter sans enregistrer ?",
        "Uno o più diagrammi contengono modifiche non salvate.\nUscire senza salvare?",
    ),
    ("dialog.exitUnsaved.quit", "Salir sin guardar", "Quitter sans enregistrer", "Esci senza salvare"),
    ("dialog.exitUnsaved.stay", "Cancelar", "Annuler", "Annulla"),
    ("dialog.saveBeforeClose.title", "Cerrar diagrama", "Fermer le diagramme", "Chiudi diagramma"),
    ("dialog.saveBeforeClose.message", "¿Guardar el diagrama actual antes de cerrarlo?", "Enregistrer le diagramme actuel avant de le fermer ?", "Salvare il diagramma corrente prima di chiuderlo?"),
    ("dialog.saveBeforeClose.save", "Guardar", "Enregistrer", "Salva"),
    ("dialog.saveBeforeClose.dontSave", "Cerrar sin guardar", "Fermer sans enregistrer", "Chiudi senza salvare"),
    ("dialog.saveBeforeClose.cancel", "Cancelar", "Annuler", "Annulla"),
    ("dialog.common.yes", "Sí", "Oui", "Sì"),
    ("dialog.common.no", "No", "Non", "No"),
    ("dialog.renameTab.message", "Nombre del diagrama:", "Nom du diagramme :", "Nome del diagramma:"),
    ("dialog.renameTab.title", "Renombrar pestaña", "Renommer l'onglet", "Rinomina scheda"),
    (
        "dialog.pasteNeedPosition.message",
        "Mueva el ratón sobre el diagrama hasta la posición de inserción deseada y vuelva a usar Pegar.",
        "Placez le curseur sur le diagramme à l'emplacement d'insertion souhaité, puis utilisez à nouveau Coller.",
        "Sposta il mouse sul diagramma nella posizione desiderata, quindi usa di nuovo Incolla.",
    ),
    ("dialog.pasteNeedPosition.title", "Pegar", "Coller", "Incolla"),
    ("dialog.deleteBlock.title", "Eliminar bloque", "Supprimer le bloc", "Elimina blocco"),
    ("dialog.deleteBlock.message", "¿Eliminar este bloque?", "Supprimer ce bloc ?", "Rimuovere questo blocco?"),
    ("dialog.deleteBlock.messageNested", "¿Eliminar este bloque y todos los bloques anidados?", "Supprimer ce bloc et tous les blocs imbriqués ?", "Rimuovere questo blocco e tutti i blocchi annidati?"),
    ("dialog.deleteBlock.remove", "Eliminar", "Supprimer", "Elimina"),
    ("dialog.deleteBlock.cancel", "Cancelar", "Annuler", "Annulla"),
    ("dialog.overwriteFile.title", "El archivo ya existe", "Le fichier existe", "Il file esiste"),
    ("dialog.overwriteFile.message", "El archivo {0} ya existe.\n¿Sobrescribir?", "Le fichier {0} existe déjà.\nRemplacer ?", "Il file {0} esiste già.\nSovrascrivere?"),
    ("dialog.overwriteFile.overwrite", "Sobrescribir", "Remplacer", "Sovrascrivi"),
    ("dialog.overwriteFile.skip", "No sobrescribir", "Ne pas remplacer", "Non sovrascrivere"),
    ("dialog.saveNoPath.title", "Guardar", "Enregistrer", "Salva"),
    ("dialog.saveNoPath.message", "No hay ruta de archivo para guardar. Use «Guardar como…».", "Aucun chemin d'enregistrement. Utilisez « Enregistrer sous… ».", "Nessun percorso di salvataggio. Usa «Salva con nome…»."),
    ("dialog.saveError.title", "Guardar", "Enregistrer", "Salva"),
    ("dialog.saveError.message", "No se pudo guardar el archivo:\n{0}", "Le fichier n'a pas pu être enregistré :\n{0}", "Impossibile salvare il file:\n{0}"),
    ("dialog.recentMissing.title", "Archivo reciente", "Fichier récent", "File recente"),
    ("dialog.recentMissing.message", "Este archivo ya no está disponible:\n{0}\nSe ha eliminado de la lista de recientes.", "Ce fichier n'est plus disponible :\n{0}\nIl a été retiré de la liste des fichiers récents.", "Questo file non è più disponibile:\n{0}\nÈ stato rimosso dall'elenco dei file recenti."),
    ("dialog.exportImage.title", "Exportar imagen", "Exporter l'image", "Esporta immagine"),
    ("dialog.exportImage.formatFailed", "No se pudo escribir la imagen (formato: {0}).", "L'image n'a pas pu être écrite (format : {0}).", "Impossibile scrivere l'immagine (formato: {0})."),
    ("dialog.exportImage.error", "Se produjo un error al guardar:\n{0}", "Une erreur s'est produite lors de l'enregistrement :\n{0}", "Si è verificato un errore durante il salvataggio:\n{0}"),
    ("dialog.caseLabel.newMessage", "Etiqueta del caso:", "Libellé du cas :", "Etichetta del caso:"),
    ("dialog.caseLabel.newTitle", "Renombrar caso", "Renommer le cas", "Rinomina caso"),
    ("dialog.caseLabel.renameButton", "Renombrar caso seleccionado", "Renommer le cas sélectionné", "Rinomina caso selezionato"),
    ("dialog.caseSelectFirst.title", "Selección requerida", "Sélection requise", "Selezione richiesta"),
    ("dialog.caseSelectFirst.message", "Seleccione primero un caso en la lista.", "Veuillez d'abord sélectionner un cas dans la liste.", "Seleziona prima un caso nell'elenco."),
    ("dialog.codeInvalidInput.title", "Entrada no válida", "Saisie non valide", "Input non valido"),
    ("dialog.codeInvalidInput.message", "Introduzca números enteros en los campos.", "Veuillez saisir des nombres entiers dans les champs.", "Inserisci numeri interi nei campi."),
    ("dialog.codeGen.targetJava", "Java", "Java", "Java"),
    ("dialog.codeGen.targetPython", "Python", "Python", "Python"),
    ("dialog.codeGen.targetJavaScript", "JavaScript", "JavaScript", "JavaScript"),
    ("dialog.codeGen.emitComments", "Incluir texto del diagrama como comentarios", "Inclure le texte du diagramme en commentaires", "Includi il testo del diagramma come commenti"),
    ("dialog.codeGen.indentFirstLine", "Sangría de la primera línea (espacios):", "Indentation de la première ligne (espaces) :", "Indentazione della prima riga (spazi):"),
    ("dialog.codeGen.spacesPerLevel", "Espacios por nivel de sangría:", "Espaces par niveau d'indentation :", "Spazi per livello di indentazione:"),
    ("dialog.codeGen.generate", "Generar", "Générer", "Genera"),
    ("dialog.codeGen.close", "Cerrar", "Fermer", "Chiudi"),
    ("dialog.codeGen.openInBrowser", "Probar en el navegador…", "Tester dans le navigateur…", "Prova nel browser…"),
    (
        "dialog.codeGen.openInBrowser.tooltip",
        "Abre el JavaScript generado en un archivo HTML temporal en el navegador predeterminado.",
        "Ouvre le JavaScript généré dans un fichier HTML temporaire dans le navigateur par défaut.",
        "Apre il JavaScript generato in un file HTML temporaneo nel browser predefinito.",
    ),
    ("dialog.codeGen.copyCode", "Copiar código …", "Copier le code …", "Copia codice …"),
    ("dialog.codeGen.copyCode.tooltip", "Copia el código mostrado al portapapeles.", "Copie le code affiché dans le presse-papiers.", "Copia il codice visualizzato negli appunti."),
    ("dialog.codeGen.copyDone.title", "Portapapeles", "Presse-papiers", "Appunti"),
    ("dialog.codeGen.copyDone.message", "El código fuente se ha copiado al portapapeles.", "Le code source a été copié dans le presse-papiers.", "Il codice sorgente è stato copiato negli appunti."),
    ("dialog.codeGen.jsBrowserHint.title", "Ejecutar JavaScript en el navegador", "Exécuter JavaScript dans le navigateur", "Esegui JavaScript nel browser"),
    (
        "dialog.codeGen.jsBrowserHint.message",
        "Las instrucciones que introdujo en el diagrama se copian tal cual al script.\n\nPara ejecutarlo necesita JavaScript válido (p. ej., let x = 5; o const MAX = 10;).\n\nEl pseudocódigo (p. ej., «número entero …») no se traduce y puede provocar errores.",
        "Les instructions saisies dans le diagramme sont copiées telles quelles dans le script.\n\nPour l'exécuter, il faut du JavaScript valide (p. ex. let x = 5; ou const MAX = 10;).\n\nLe pseudocode (p. ex. « nombre entier … ») n'est pas traduit et peut provoquer des erreurs.",
        "Le istruzioni inserite nel diagramma vengono copiate così come sono nello script.\n\nPer eseguirlo serve JavaScript valido (es. let x = 5; o const MAX = 10;).\n\nIl pseudocodice (es. «numero intero …») non viene tradotto e può causare errori.",
    ),
    ("dialog.codeGen.jsBrowserEmpty.title", "Sin código", "Aucun code", "Nessun codice"),
    ("dialog.codeGen.jsBrowserEmpty.message", "Genere primero JavaScript o pegue el código en el área de texto.", "Générez d'abord du JavaScript ou collez le code dans la zone de texte.", "Genera prima JavaScript o incolla il codice nell'area di testo."),
    ("dialog.codeGen.jsBrowserNoDesktop.title", "No se puede abrir el navegador", "Impossible d'ouvrir le navigateur", "Impossibile aprire il browser"),
    ("dialog.codeGen.jsBrowserNoDesktop.message", "Abrir un navegador no está admitido en este entorno.", "L'ouverture d'un navigateur n'est pas prise en charge dans cet environnement.", "L'apertura di un browser non è supportata in questo ambiente."),
    ("dialog.codeGen.jsBrowserIoError.title", "Archivo de vista previa", "Fichier d'aperçu", "File di anteprima"),
    ("dialog.codeGen.jsBrowserIoError.message", "No se pudo crear o abrir el archivo de vista previa:\n{0}", "Le fichier d'aperçu n'a pas pu être créé ou ouvert :\n{0}", "Impossibile creare o aprire il file di anteprima:\n{0}"),
    ("dialog.codeGen.jsBrowserPageTitle", "VisuStruct", "VisuStruct", "VisuStruct"),
    (
        "dialog.codeGen.jsBrowserConsoleHint",
        "La salida de console.log aparece en la consola de desarrollo.\n\nSafari: Safari → Ajustes → Avanzado → activar «Mostrar menú Desarrollo en la barra de menús»; después Desarrollo → Mostrar consola JavaScript (macOS: ⌥⌘C).\n\nFirefox: Abrir herramientas de desarrollo, pestaña «Consola» (macOS: ⌥⌘K; Windows: Ctrl+Mayús+K).\n\nChrome: Abrir herramientas para desarrolladores, pestaña «Consola» (macOS: ⌥⌘I; Windows: F12 o Ctrl+Mayús+J).\n\nMicrosoft Edge: Abrir herramientas para desarrolladores, pestaña «Consola» (Windows: F12 o Ctrl+Mayús+J).",
        "La sortie de console.log apparaît dans la console de développement.\n\nSafari : Safari → Réglages → Avancé → activer « Afficher le menu Développement dans la barre des menus » ; puis Développement → Afficher la console JavaScript (macOS : ⌥⌘C).\n\nFirefox : Ouvrir les outils de développement, onglet « Console » (macOS : ⌥⌘K ; Windows : Ctrl+Maj+K).\n\nChrome : Ouvrir les outils de développement, onglet « Console » (macOS : ⌥⌘I ; Windows : F12 ou Ctrl+Maj+J).\n\nMicrosoft Edge : Ouvrir les outils de développement, onglet « Console » (Windows : F12 ou Ctrl+Maj+J).",
        "L'output di console.log compare nella console per sviluppatori.\n\nSafari: Safari → Impostazioni → Avanzate → attiva «Mostra menu Sviluppo nella barra dei menu»; poi Sviluppo → Mostra console JavaScript (macOS: ⌥⌘C).\n\nFirefox: Apri strumenti di sviluppo, scheda «Console» (macOS: ⌥⌘K; Windows: Ctrl+Maiusc+K).\n\nChrome: Apri strumenti per sviluppatori, scheda «Console» (macOS: ⌥⌘I; Windows: F12 o Ctrl+Maiusc+J).\n\nMicrosoft Edge: Apri strumenti per sviluppatori, scheda «Console» (Windows: F12 o Ctrl+Maiusc+J).",
    ),
    ("tab.untitled", "Sin título", "Sans titre", "Senza titolo"),
    ("tab.close.tooltip", "Cerrar esta pestaña", "Fermer cet onglet", "Chiudi questa scheda"),
    ("tab.close.a11y", "Cerrar pestaña del diagrama", "Fermer l'onglet du diagramme", "Chiudi scheda del diagramma"),
    ("tab.unsaved.a11y", "Cambios sin guardar", "Modifications non enregistrées", "Modifiche non salvate"),
    ("fileChooser.openTitle", "Abrir", "Ouvrir", "Apri"),
    ("fileChooser.saveTitle", "Guardar", "Enregistrer", "Salva"),
    ("fileChooser.lookIn", "Buscar en:", "Rechercher dans :", "Cerca in:"),
    ("fileChooser.saveIn", "Guardar en:", "Enregistrer dans :", "Salva in:"),
    ("fileChooser.fileName", "Nombre de archivo:", "Nom du fichier :", "Nome file:"),
    ("fileChooser.filesOfType", "Tipo de archivos:", "Type de fichiers :", "Tipo di file:"),
    ("fileChooser.open", "Abrir", "Ouvrir", "Apri"),
    ("fileChooser.openTooltip", "Abrir el archivo seleccionado.", "Ouvrir le fichier sélectionné.", "Apri il file selezionato."),
    ("fileChooser.save", "Guardar", "Enregistrer", "Salva"),
    ("fileChooser.saveTooltip", "Guardar el archivo seleccionado.", "Enregistrer le fichier sélectionné.", "Salva il file selezionato."),
    ("fileChooser.cancel", "Cancelar", "Annuler", "Annulla"),
    ("fileChooser.cancelTooltip", "Cerrar el cuadro de diálogo", "Fermer la boîte de dialogue", "Chiudi la finestra di dialogo"),
    ("fileChooser.update", "Actualizar", "Actualiser", "Aggiorna"),
    ("fileChooser.updateTooltip", "Actualizar la lista", "Actualiser la liste", "Aggiorna l'elenco"),
    ("fileChooser.newFolder", "Nueva carpeta", "Nouveau dossier", "Nuova cartella"),
    ("fileChooser.newFolderTooltip", "Crear nueva carpeta", "Créer un nouveau dossier", "Crea nuova cartella"),
    ("fileChooser.acceptAll", "Todos los archivos", "Tous les fichiers", "Tutti i file"),
    ("fileChooser.help", "Ayuda", "Aide", "Guida"),
    ("fileChooser.helpTooltip", "Ayuda", "Aide", "Guida"),
    ("fileChooser.newFolderError", "No se pudo crear la carpeta.", "Impossible de créer le dossier.", "Impossibile creare la cartella."),
    ("structure.element.statement", "Instrucción", "Instruction", "Istruzione"),
    ("structure.element.decision", "Decisión", "Décision", "Decisione"),
    ("structure.element.multiway", "Selección múltiple (switch)", "Sélection multiple (switch)", "Selezione multipla (switch)"),
    ("structure.element.forLoop", "Bucle for", "Boucle for", "Ciclo for"),
    ("structure.element.whileLoop", "Bucle while", "Boucle while", "Ciclo while"),
    ("structure.element.doWhileLoop", "Bucle do-while", "Boucle do-while", "Ciclo do-while"),
    ("structure.element.infiniteLoop", "Bucle infinito", "Boucle infinie", "Ciclo infinito"),
    ("structure.element.breakExit", "Salida (break)", "Sortie (break)", "Uscita (break)"),
    ("structure.element.call", "Llamada", "Appel", "Chiamata"),
    ("structure.element.empty", "Bloque vacío", "Bloc vide", "Blocco vuoto"),
    ("structure.palette.statement", "Instrucción", "Instruction", "Istruzione"),
    ("structure.palette.decision", "Si", "Si", "Se"),
    ("structure.palette.multiway", "Switch", "Switch", "Switch"),
    ("structure.palette.forLoop", "Bucle for", "Boucle for", "Ciclo for"),
    ("structure.palette.whileLoop", "Bucle while", "Boucle while", "Ciclo while"),
    ("structure.palette.doWhileLoop", "Bucle do-while", "Boucle do-while", "Ciclo do-while"),
    ("structure.palette.infiniteLoop", "Bucle infinito", "Boucle infinie", "Ciclo infinito"),
    ("structure.palette.breakExit", "Break", "Break", "Break"),
    ("structure.palette.call", "Llamada", "Appel", "Chiamata"),
    ("structure.palette.empty", "Vacío", "Vide", "Vuoto"),
    ("structure.multiway.defaultCaseLabel", "default", "default", "default"),
    ("elementPreset.englishJava", "Estilo Java", "Style Java", "Stile Java"),
    ("elementPreset.didacticUiLanguage", "Idioma de la interfaz", "Langue de l'interface", "Lingua dell'interfaccia"),
    ("dialog.elementText.title", "Configuración — textos predeterminados de bloques", "Paramètres — textes par défaut des blocs", "Impostazioni — testi predefiniti dei blocchi"),
    ("dialog.elementText.intro", "Texto predeterminado para bloques <b>recién insertados</b> en el diagrama de estructura.", "Texte par défaut pour les blocs <b>nouvellement insérés</b> dans l'organigramme.", "Testo predefinito per i blocchi <b>appena inseriti</b> nel diagramma di struttura."),
    ("dialog.elementText.previewTitle", "Vista previa de textos predeterminados", "Aperçu des textes par défaut", "Anteprima dei testi predefiniti"),
    ("dialog.elementText.cancel", "Cancelar", "Annuler", "Annulla"),
    ("dialog.elementText.ok", "Aceptar", "OK", "OK"),
    ("dialog.elementText.uiLanguageSection", "Idioma de la interfaz", "Langue de l'interface", "Lingua dell'interfaccia"),
    ("dialog.elementText.simulationSection", "Simulación", "Simulation", "Simulazione"),
    ("dialog.elementText.simulationSpeedLabel", "Pausa entre pasos:", "Pause entre les étapes :", "Pausa tra i passi:"),
    ("dialog.elementText.simulationSpeedUnit", "s", "s", "s"),
    ("dialog.elementText.simulationHighlightLabel", "Resaltado en el diagrama:", "Surbrillance sur le diagramme :", "Evidenziazione nel diagramma:"),
    ("dialog.elementText.simulationHighlightLast", "Último ejecutado", "Dernière exécution", "Ultimo eseguito"),
    ("dialog.elementText.simulationHighlightNext", "Siguiente", "Étape suivante", "Prossimo passo"),
    ("popup.zoom", "Zoom", "Zoom", "Zoom"),
    ("popup.zoom.larger", "Más grande", "Plus grand", "Più grande"),
    ("popup.zoom.smaller", "Más pequeño", "Plus petit", "Più piccolo"),
    ("popup.zoom.wider", "Más ancho", "Plus large", "Più largo"),
    ("popup.zoom.narrower", "Más estrecho", "Plus étroit", "Più stretto"),
    ("popup.zoom.taller", "Más alto", "Plus haut", "Più alto"),
    ("popup.zoom.shorter", "Más bajo", "Plus bas", "Più basso"),
    ("popup.editText", "Editar texto", "Modifier le texte", "Modifica testo"),
    ("popup.copy", "Copiar", "Copier", "Copia"),
    ("popup.paste", "Pegar", "Coller", "Incolla"),
    ("popup.delete", "Eliminar…", "Supprimer…", "Elimina…"),
    ("popup.swapBranches", "Intercambiar ramas verdadero y falso", "Échanger les branches vrai et faux", "Scambia rami vero e falso"),
    ("popup.insertNewCase", "Insertar nuevo caso", "Insérer un nouveau cas", "Inserisci nuovo caso"),
    ("popup.caseSubmenu", "Caso: {0}", "Cas : {0}", "Caso: {0}"),
    ("popup.moveCaseLeft", "Mover caso {0} a la izquierda", "Déplacer le cas {0} vers la gauche", "Sposta caso {0} a sinistra"),
    ("popup.moveCaseRight", "Mover caso {0} a la derecha", "Déplacer le cas {0} vers la droite", "Sposta caso {0} a destra"),
    ("popup.removeCase", "Eliminar caso {0}", "Supprimer le cas {0}", "Rimuovi caso {0}"),
    ("popup.removeCaseConfirm", "¿Eliminar el caso {0} y todos los bloques anidados?", "Supprimer le cas {0} et tous les blocs imbriqués ?", "Eliminare il caso {0} e tutti i blocchi annidati?"),
    ("popup.removeCaseTitle", "Eliminar caso", "Supprimer le cas", "Rimuovi caso"),
    ("popup.pasteEmpty", "Aún no ha copiado nada. Use primero Copiar.", "Rien n'a encore été copié. Utilisez d'abord Copier.", "Non hai ancora copiato nulla. Usa prima Copia."),
    ("popup.pasteTitle", "Pegar", "Coller", "Incolla"),
    ("palette.aboutVisuStruct", "Acerca de VisuStruct", "À propos de VisuStruct", "Informazioni su VisuStruct"),
    ("palette.aboutTooltip", "Acerca de VisuStruct", "À propos de VisuStruct", "Informazioni su VisuStruct"),
    ("palette.deleteElement", "Eliminar bloque", "Supprimer le bloc", "Elimina blocco"),
    ("palette.generateCode", "Generar código", "Générer le code", "Genera codice"),
    (
        "palette.simulation.tooltip",
        "Iniciar simulación o volver al editor del diagrama",
        "Démarrer la simulation ou revenir à l'éditeur de diagramme",
        "Avvia simulazione o torna all'editor del diagramma",
    ),
    ("palette.trashDrop", "Soltar aquí para eliminar", "Déposer ici pour supprimer", "Trascina qui per eliminare"),
    ("palette.trashClick", "Clic para eliminar el bloque seleccionado", "Cliquer pour supprimer le bloc sélectionné", "Clic per eliminare il blocco selezionato"),
]

EXTRA_MENU_KEYS = {
    "es": {
        "menu.settings.language.es": "Español",
        "menu.settings.language.fr": "Francés",
        "menu.settings.language.it": "Italiano",
    },
    "fr": {
        "menu.settings.language.es": "Espagnol",
        "menu.settings.language.fr": "Français",
        "menu.settings.language.it": "Italien",
    },
    "it": {
        "menu.settings.language.es": "Spagnolo",
        "menu.settings.language.fr": "Francese",
        "menu.settings.language.it": "Italiano",
    },
}

PATCH_EXISTING = {
    "Messages_en.properties": {
        "menu.settings.language.es": "Spanish",
        "menu.settings.language.fr": "French",
        "menu.settings.language.it": "Italian",
    },
    "Messages.properties": {
        "menu.settings.language.es": "Spanish",
        "menu.settings.language.fr": "French",
        "menu.settings.language.it": "Italian",
    },
    "Messages_de.properties": {
        "menu.settings.language.es": "Spanisch",
        "menu.settings.language.fr": "Französisch",
        "menu.settings.language.it": "Italienisch",
    },
    "Messages_pt_PT.properties": {
        "menu.settings.language.es": "Espanhol",
        "menu.settings.language.fr": "Francês",
        "menu.settings.language.it": "Italiano",
    },
}

TRANSLATIONS: dict[str, dict[str, str]] = {
    "es": {k: es for k, es, _, _ in _ENTRIES},
    "fr": {k: fr for k, _, fr, _ in _ENTRIES},
    "it": {k: it for k, _, _, it in _ENTRIES},
}
for loc, extras in EXTRA_MENU_KEYS.items():
    TRANSLATIONS[loc].update(extras)


def escape_property_value(value: str) -> str:
    """Java .properties: literal newlines must be \\n on one line."""
    return (
        value.replace("\\", "\\\\")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
    )


def parse_source_keys(path: Path) -> list[str]:
    keys: list[str] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        if "=" in s:
            keys.append(s.split("=", 1)[0])
    return keys


def build_output(locale: str, source_lines: list[str]) -> str:
    trans = TRANSLATIONS[locale]
    comment_map = SECTION_COMMENTS[locale]
    out: list[str] = [LOCALE_HEADERS[locale]]
    for line in source_lines[1:]:  # skip English header from source
        stripped = line.strip()
        if not stripped:
            out.append("")
            continue
        if stripped.startswith("#"):
            out.append(comment_map.get(stripped, stripped))
            continue
        if "=" not in stripped:
            out.append(line)
            continue
        key = stripped.split("=", 1)[0]
        if key not in trans:
            raise KeyError(f"Missing translation for {key!r} in locale {locale}")
        out.append(f"{key}={escape_property_value(trans[key])}")
    # Append new menu keys after menu.settings.language.pt if not already present
    extra = EXTRA_MENU_KEYS[locale]
    text = "\n".join(out) + "\n"
    if "menu.settings.language.es=" not in text:
        insert_after = "menu.settings.language.pt="
        lines = text.splitlines()
        new_lines: list[str] = []
        inserted = False
        for ln in lines:
            new_lines.append(ln)
            if not inserted and ln.startswith(insert_after):
                for ek in ("menu.settings.language.es", "menu.settings.language.fr", "menu.settings.language.it"):
                    new_lines.append(f"{ek}={escape_property_value(extra[ek])}")
                inserted = True
        text = "\n".join(new_lines) + "\n"
    return text


def patch_existing_bundle(filename: str, entries: dict[str, str]) -> None:
    path = I18N_DIR / filename
    lines = path.read_text(encoding="utf-8").splitlines()
    existing_keys = set()
    for line in lines:
        s = line.strip()
        if s and not s.startswith("#") and "=" in s:
            existing_keys.add(s.split("=", 1)[0])
    new_lines = list(lines)
    insert_idx = None
    for i, line in enumerate(lines):
        if line.startswith("menu.settings.language.pt="):
            insert_idx = i + 1
            break
    if insert_idx is None:
        raise RuntimeError(f"{filename}: anchor menu.settings.language.pt= not found")
    to_add = [(k, v) for k, v in entries.items() if k not in existing_keys]
    if not to_add:
        return
    for offset, (k, v) in enumerate(to_add):
        new_lines.insert(insert_idx + offset, f"{k}={v}")
    path.write_text("\n".join(new_lines) + "\n", encoding="utf-8")


def main() -> int:
    if not SOURCE.is_file():
        print(f"Source not found: {SOURCE}", file=sys.stderr)
        return 1

    source_lines = SOURCE.read_text(encoding="utf-8").splitlines()
    source_keys = parse_source_keys(SOURCE)
    patch_only = set(next(iter(PATCH_EXISTING.values())).keys())
    source_base = [k for k in source_keys if k not in patch_only]
    entry_keys = [k for k, _, _, _ in _ENTRIES]
    if set(source_base) != set(entry_keys):
        missing = set(source_base) - set(entry_keys)
        extra = set(entry_keys) - set(source_base)
        print("Key mismatch with Messages_en.properties:", file=sys.stderr)
        if missing:
            print(f"  missing in _ENTRIES: {sorted(missing)}", file=sys.stderr)
        if extra:
            print(f"  extra in _ENTRIES: {sorted(extra)}", file=sys.stderr)
        return 1

    for loc in ("es", "fr", "it"):
        out_path = I18N_DIR / f"Messages_{loc}.properties"
        content = build_output(loc, source_lines)
        out_path.write_text(content, encoding="utf-8")
        line_count = len(content.splitlines())
        print(f"Wrote {out_path} ({line_count} lines)")

    for filename, entries in PATCH_EXISTING.items():
        patch_existing_bundle(filename, entries)
        path = I18N_DIR / filename
        print(f"Patched {path} ({len(path.read_text(encoding='utf-8').splitlines())} lines)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
