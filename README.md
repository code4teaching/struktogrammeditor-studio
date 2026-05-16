<p align="center">
  <a href="https://visustruct.org/" title="VisuStruct">
    <img src="docs/VisuStruct-icon.png" alt="VisuStruct" width="128" height="128">
  </a>
</p>

# VisuStruct

[![Build](https://github.com/code4teaching/VisuStruct/actions/workflows/maven-build.yml/badge.svg?branch=master)](https://github.com/code4teaching/VisuStruct/actions)

**VisuStruct** is a **structure-chart editor** for **Nassi–Shneiderman** diagrams (desktop, Kotlin / Swing).

Under **Settings → Languages** the **user interface** can be set to any of **21 languages** aligned with **VisuStruct-swift** (English, German, Spanish, French, Italian, Polish, Turkish, Korean, Japanese, Greek, Ukrainian, Arabic, Russian, Hebrew, Dutch, Swedish, Danish, Norwegian Bokmål, Chinese Simplified/Traditional, European Portuguese). Stored as `uilanguage` in `visustruct.properties` (e.g. `zh_Hans`, `pt_PT`). UI strings are synced from the Swift app where keys overlap: `python3 tools/sync_java_messages_from_swift.py` (map: `VisuStruct-swift/tools/java_swift_i18n_map.json`). Desktop-only labels may still fall back to English.

*Lineage:* Based on the open-source project [kekru/struktogrammeditor](https://github.com/kekru/struktogrammeditor) (Kevin Krummenauer, MIT). Copyright and terms: [LICENSE](LICENSE).

**Website:** [visustruct.org](https://visustruct.org) · **Repository:** [github.com/code4teaching/VisuStruct](https://github.com/code4teaching/VisuStruct)

```bash
git clone https://github.com/code4teaching/VisuStruct.git
```

---

## Changes in 1.2.3 (summary)

- **Kotlin:** full migration of production code; Java reference under `src/main/java-reference/`; tests still Java.
- **i18n:** localized element editor and dialogs; `tools/resync_message_bundles.py` when Swift repo is absent.
- **UI:** one-touch expandable element-editor split.

Full notes: [`release-notes/v1.2.3.md`](release-notes/v1.2.3.md).

## Changes in 1.2.2 (summary)

- **Languages:** **21 UI languages** (VisuStruct-swift); `tools/sync_java_messages_from_swift.py`.
- **CI:** GitHub Actions build passes again (headless-safe drag-and-drop init for tests).

Full notes: [`release-notes/v1.2.2.md`](release-notes/v1.2.2.md).

## Changes in 1.2.1 (summary)

- **Simulation:** **true/false** branch selection by case name; **reset (Stop)** for new input on rerun; highlight mode and play speed under **Settings → Simulation…**; sidebar layout (input always visible, current block shows instruction text).
- **Save:** pending **element-editor** text applied before save; more reliable save dialog on **macOS**.
- **Code generator:** structure chart text as comments **off** by default.

Full notes: [`release-notes/v1.2.1.md`](release-notes/v1.2.1.md).

## Changes in 1.2.0 (summary)

- **Simulation (prep):** **Kotlin** in the Maven build; **`SimulationDocument`** and related types plus **XML decode** aligned with the existing diagram file format; **`Struktogramm.toSimulationDocument()`** exports the current editor state for a future **`step()`** engine (no simulation UI yet).

Full notes: [`release-notes/v1.2.0.md`](release-notes/v1.2.0.md).

## Changes in 1.1.4 (summary)

- **Diagram:** canvas **view zoom** with **⌘+ / ⌘−** (macOS) or **Ctrl+ / Ctrl−** (Windows/Linux); **no** block resize via mouse wheel anymore.
- **Tabs:** prompt to **save / discard / cancel** when closing a diagram with unsaved changes; dirty state shown in the tab header (not as `*` in the title).
- **Code:** removed long blocks of commented-out legacy code (especially `Struktogramm`, `StrTabbedPane`, `XMLLeser`, palette/trash setup) for easier maintenance.

Full notes: [`release-notes/v1.1.4.md`](release-notes/v1.1.4.md).

## Changes in 1.1.3 (summary)

- **Tabs:** close tabs via middle-click, context menu, and **Ctrl+W** / **Cmd+W**; clicking the tab header switches diagrams reliably (close “×” is a label, not a button).
- **Palette / trash:** **left-click** deletes the **selected** (highlighted) block with the same confirmation as context-menu **Delete**; drag-to-trash unchanged.
- **Code generator:** **Java scope** and **`CodeGenRules`** updates (`StruktogrammElementListe`, `CodeErzeuger`, and related).

Full notes: [`release-notes/v1.1.3.md`](release-notes/v1.1.3.md).

## Changes in 1.1.2 (summary)

- **Diagram canvas (light theme):** styling aligned with [VisuStruct-SwiftUI](https://github.com/code4teaching/VisuStruct-SwiftUI): black block borders (2 px), outer frame inset 16 px, content inset 28 px, selection as blue outline (no blue fill).
- **Diagram text:** **Sans-Serif** diagram font (Segoe UI, Helvetica Neue, …), **17 pt**; **bold** control-flow keywords (`if`, `while`, …) like Swift; **do-while** shows **`do`** in the left gutter (extra inset); **Fallauswahl** DE palette label **Fallauswahl**.
- **Left palette:** labels follow **UI language** (`structure.palette.*`), except preset **Java (Standard)** (`if`, `while`, …).

Full notes: [`release-notes/v1.1.2.md`](release-notes/v1.1.2.md).

## Changes in 1.1.1 (summary)

- **About dialog:** clearer links for website, GitHub repository, and developer site.
- **Palette:** Lucide trash icon for the delete area, shown larger.
- **File menu:** added system **Print...** support for printing or saving via the OS dialog.

## Changes in 1.1 (summary)

- **Element editor:** first step toward the Swift-style bottom editor panel.

## Changes in 1.0.9 (summary)

- **Palette:** Lucide SVG icons on the element buttons, including `split` for Branch.
- **For loop:** multi-line start/condition/increment headers are shown and exported as `start; condition; increment`.
- **Java export:** `output:` statements become `System.out.println(...)`.

Full notes: [`release-notes/v1.0.9.md`](release-notes/v1.0.9.md).

## Changes in 1.0.8 (summary)

- **Code generator:** **JavaScript** target (brace syntax like Java, `"use strict";` prefix); **`celanguage`** `2` = JavaScript in **`visustruct.properties`** (with `0` = Java, `1` = Python).
- **Generate code dialog:** **Test in browser** (only when JavaScript is selected): confirmation explains valid JS in free lines; opens a temporary **HTML** preview via the system browser (`</script>` in user code is escaped).

Full notes: [`release-notes/v1.0.8.md`](release-notes/v1.0.8.md).

## Changes in 1.0.7 (summary)

- **Code generator:** **Delphi/Pascal** removed; **Python** added (**3.10+**, `match` / `case` for multi-way branches); dialog is **localized**; target language stored in **`visustruct.properties`** as **`celanguage`**: `0` = Java, `1` = Python.
- **Multi-way (Java):** default label for the last column is **`default`** (not “Else”); string from **`structure.multiway.defaultCaseLabel`** (i18n).
- **API:** `XMLLeser` load methods renamed **`ladeXML`** (fixes the old typo **`ladeXLM`**).
- **Code:** removed unused selection / size-box leftovers from upstream (`Struktogramm`, `StruktogrammElement`).
- **Build:** Maven **`de.visustruct:visustruct:1.0.7`** → artifact **`visustruct-1.0.7.jar`**; releases also ship **`visustruct.jar`**.

Full notes: [`release-notes/v1.0.7.md`](release-notes/v1.0.7.md).

## Changes in 1.0.6 (summary)

- **UI & theme:** cleaner menus; **light / dark** FlatLaf **without restart**; improved dark-mode readability.
- **Languages:** **Settings → Languages** lists **21 languages** (same set as VisuStruct-swift, except the Java-keyword pseudo-locale); stored in **`visustruct.properties`** (`uilanguage`). If unset, default follows **JVM locale**. Regenerate bundles: `python3 tools/sync_java_messages_from_swift.py`.
- **I18n:** menu, core confirmation dialogs, `JFileChooser` labels, context menu, palette (PNG, about, trash, etc.); **English** is no longer overridden by the OS default locale.
- **Structure-chart labels:** preset **“Java (default)”** (and localized equivalents); **preview** in the settings dialog; **palette** shows Java keywords on buttons when the Java preset is active.
- **Tabs:** new diagram tab uses a localized **“Untitled”**-style title instead of a hard-coded English string.
- **Build / `build.properties`:** empty `timestamp` / `revision` no longer break startup.
- **Build:** Maven **`de.visustruct:visustruct:1.0.6`**, **`visustruct-1.0.6.jar`**, plus **`visustruct.jar`**.

Full notes: [`release-notes/v1.0.6.md`](release-notes/v1.0.6.md).

## Changes in 1.0.5 (summary)

- **Canvas:** colors aligned with VisuStruct light style via **`CanvasStyle`** (background, borders, selection, drag preview).
- **Save / tabs:** tab title and dirty `*` refer to the **correct** diagram; saves use **UTF-8** with an **error dialog** on write failure.
- **macOS:** menu bar **inside the window** (same as Windows/Linux) so **Save** works reliably with FlatLaf; **FlatLaf 3.7.1**; save dialog after tab rename deferred one UI tick.
- **Light theme menus:** **`VisuStructTheme`** improves menu-item readability.

## Changes in 1.0.4 (summary)

This release aligns **branding and technical identity** with **VisuStruct**. **`.visustruct` / XML** project files stay compatible; element types remain numeric in XML.

- **Java packages:** **`de.visustruct.*`** (was `de.kekru.struktogrammeditor`).
- **Maven:** **`de.visustruct:visustruct:1.0.4`**. Dependents must update **groupId**, **artifactId**, and **version** if needed. Fat JAR: **`visustruct-1.0.4.jar`**; workflow also publishes **`visustruct.jar`**.
- **Settings:** default file **`visustruct.properties`**; migrates from **`struktogrammeditor.properties`** on the next settings save.
- **macOS `.app`:** bundle ID and document UTI → **`de.visustruct.*`**; you may need to set **Open With** for `.visustruct` again. **Main:** **`de.visustruct.control.Main`**.
- **Upstream:** [kekru/struktogrammeditor](https://github.com/kekru/struktogrammeditor) remains credited (MIT) in README and the about dialog.

## Changes in 1.0.3 (summary)

- **Build:** Maven Wrapper **3.3.4** (script-only), Maven **3.9.9**; `maven-compiler-plugin` **3.14.1**, `maven-assembly-plugin` **3.7.1**; compile with **`-Xlint:deprecation`**.
- **API / Swing:** modern modifiers (`getModifiersEx`, …), generics in `JListEasy` / `FontChooser`; `JNumberField` deprecation cleanups.

## Changes in 1.0.2 (summary)

- **Code generator:** faster buffered output; clearer comments when “diagram text as comments” is enabled.
- **New-element text presets** in the settings dialog (including English/Java-style placeholders).
- **Branding / files:** VisuStruct and **`.visustruct`** filters and associations.
- **Logo** and minor UI tweaks.

## Platform (1.0.1 and earlier)

- **Java 17** target (build and run with JDK 17+).
- **FlatLaf** light/dark themes; **JDOM2**; no legacy AppleJavaExtensions.
- **Motif** look-and-feel removed; **Metal** and FlatLaf available.
- Current fat JAR name follows **`pom.xml`** **`version`** (e.g. **`visustruct-1.2.3.jar`**).

---

## Download (no build required)

**Install VisuStruct from this repository** (the JAR below)—not the legacy **[kekru/struktogrammeditor](https://github.com/kekru/struktogrammeditor)** project, which remains **historical MIT lineage** and attribution only.

Students only need a **Java 17+** runtime — **not** Maven.

**Latest published build** (stable filename):

**[Download visustruct.jar](https://github.com/code4teaching/VisuStruct/releases/latest/download/visustruct.jar)**

Run from the folder that contains the JAR:

```bash
java -jar visustruct.jar
```

On **Windows**, double-click often works if `.jar` is associated with Java. If an **old JRE** (e.g. 8) is the default, you will get **`Error: A JNI error has occurred`** — install **[JDK 17+](https://adoptium.net/)** and ensure `java -version` reports **17** or newer (see **Troubleshooting** below).

All releases: [github.com/code4teaching/VisuStruct/releases](https://github.com/code4teaching/VisuStruct/releases)

**Maintainers:** To ship a new version, create a GitHub **Release** with a tag such as **`vX.Y.Z`** (one bump per release — not for every local change). Workflow [`.github/workflows/release-assets.yml`](.github/workflows/release-assets.yml) builds the JARs and attaches **`visustruct.jar`** (stable download URL above). Update **`pom.xml`** `<version>` together with that release.

---

## Prerequisites

- [JDK 17](https://adoptium.net/) or newer (to **run** the JAR; to **build**, see below)
- **Building only:** network on first run so Maven can download dependencies

## Troubleshooting

### `Error: A JNI error has occurred, please check your installation and try again`

This almost always means the **JVM is too old** for the JAR. VisuStruct is built for **Java 17**; an older runtime (e.g. **Java 8**) cannot load the classes and may show this message before a more specific error.

**Fix:** Install a **JDK or JRE 17+** (e.g. [Eclipse Temurin](https://adoptium.net/)). Then check in a terminal:

```text
java -version
```

You should see something like `openjdk version "17"` or `"21"`. If Windows still uses an old `java.exe`, adjust **PATH** or call the full path to the new runtime, e.g.:

```powershell
"C:\Program Files\Eclipse Adoptium\jdk-17...\bin\java.exe" -jar visustruct.jar
```

### `UnsupportedClassVersionError`

Same cause: upgrade to **Java 17+** as above.

## Build a runnable JAR

Clone or download the project and open a terminal in the project root.

**Windows (PowerShell):**

```powershell
.\mvnw.cmd clean package
```

**Linux or macOS:**

```bash
chmod +x mvnw
./mvnw clean package
```

Output (version from `pom.xml`):

```text
target/visustruct-1.2.3.jar
```

## Run

```bash
java -jar target/visustruct-1.2.3.jar
```

Double-click may work if `.jar` is associated with Java.

---

## Contributing

Suggestions and **bug reports** are welcome via [**Issues** and **Pull requests**](https://github.com/code4teaching/VisuStruct).

**Commits** and **code comments** may use **English**.

### UI strings (21 languages)

- Bundles: `src/main/resources/de/visustruct/i18n/Messages_*.properties` (plus **`structure.*`** keys where applicable).
- **Source of truth for new keys:** add the English string to **`Messages_en.properties`**, then extend the Swift↔Java map in the sibling repo **`VisuStruct-swift/tools/java_swift_i18n_map.json`** (clone next to this project; see `.gitignore`).
- **Regenerate** all locale files from Swift where mapped (`tools/README.md`):

  ```bash
  python3 tools/sync_java_messages_from_swift.py
  python3 tools/analyze_i18n_gaps.py   # optional: unmapped keys
  ```

  Unmapped keys keep the existing bundle text or fall back to English. Desktop-only dialogs may stay English until mapped.
- **Menu language** is **Settings → Languages** only (`uilanguage` in `visustruct.properties`); do not duplicate a language list in other settings dialogs.
- Open an issue first if keys, didactic wording, or Swift parity are unclear.

---

## License

**MIT** — see [LICENSE](LICENSE). Copyright to the original codebase remains with **Kevin Krummenauer** (also stated in LICENSE).

Palette icons are from [Lucide](https://lucide.dev/) (ISC; some icons derived from Feather/MIT). The bundled license text is in `src/main/resources/licenses/LUCIDE.txt`.

## Historical upstream website (attribution)

[whiledo.de — Struktogrammeditor](https://whiledo.de/index.php?p=struktogrammeditor) — original editor by Kevin Krummenauer (context and license lineage; **install VisuStruct** from this GitHub project instead).
