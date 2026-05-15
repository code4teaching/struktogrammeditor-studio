#!/usr/bin/env python3
"""
Legacy generator (auto-translate JSON). Prefer:

  python3 tools/sync_java_messages_from_swift.py

which reads VisuStruct-swift Localizable.strings via java_swift_i18n_map.json.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from missing_nl_pl_tr_ru_entries import MISSING_ENTRIES

REPO_ROOT = Path(__file__).resolve().parents[2]
I18N_DIR = REPO_ROOT / "src/main/resources/de/visustruct/i18n"
SOURCE = I18N_DIR / "Messages_en.properties"
JSON_DATA = Path(__file__).resolve().parents[1] / ".nl_pl_tr_ru_translations.json"

LOCALE_HEADERS = {
    "nl": "# Dutch",
    "pl": "# Polish",
    "tr": "# Turkish",
    "ru": "# Russian",
}

SECTION_COMMENTS = {
    "nl": {
        "# Structure block names (didactic; order = element types 0–9)": (
            "# Didactische namen van structuurblokken (volgorde = elementtypen 0–9)"
        ),
        "# Short palette labels (interface language preset)": (
            "# Korte paletlabels (interface-taal)"
        ),
    },
    "pl": {
        "# Structure block names (didactic; order = element types 0–9)": (
            "# Nazwy dydaktyczne bloków struktury (kolejność = typy elementów 0–9)"
        ),
        "# Short palette labels (interface language preset)": (
            "# Krótkie etykiety palety (język interfejsu)"
        ),
    },
    "tr": {
        "# Structure block names (didactic; order = element types 0–9)": (
            "# Yapı bloklarının didaktik adları (sıra = öğe türleri 0–9)"
        ),
        "# Short palette labels (interface language preset)": (
            "# Kısa palet etiketleri (arayüz dili)"
        ),
    },
    "ru": {
        "# Structure block names (didactic; order = element types 0–9)": (
            "# Дидактические названия блоков структуры (порядок = типы элементов 0–9)"
        ),
        "# Short palette labels (interface language preset)": (
            "# Краткие подписи палитры (язык интерфейса)"
        ),
    },
}

# Korrekturen für automatisch übersetzte Einträge in der JSON-Datei
FIXES: dict[str, dict[str, str]] = {
    "menu.edit": {"nl": "Bewerken", "pl": "Edycja", "tr": "Düzenle", "ru": "Правка"},
    "menu.file.open": {"nl": "Openen…", "pl": "Otwórz…", "tr": "Aç…", "ru": "Открыть…"},
    "menu.file.saveAs": {"nl": "Opslaan als…", "pl": "Zapisz jako…", "tr": "Farklı kaydet…", "ru": "Сохранить как…"},
    "menu.edit.redo": {"nl": "Opnieuw", "pl": "Ponów", "tr": "Yinele", "ru": "Повторить"},
    "menu.settings.language.en": {"nl": "Engels", "pl": "Angielski", "tr": "İngilizce", "ru": "Английский"},
    "menu.settings.language.de": {"nl": "Duits", "pl": "Niemiecki", "tr": "Almanca", "ru": "Немецкий"},
    "menu.settings.language.pt": {"nl": "Portugees", "pl": "Portugalski", "tr": "Portekizce", "ru": "Португальский"},
    "dialog.codeGen.copyCode": {"nl": "Code kopiëren …", "pl": "Kopiuj kod …", "tr": "Kodu kopyala …", "ru": "Скопировать код …"},
}

MENU_LANGUAGE_LABELS: dict[str, dict[str, str]] = {
    "nl": {
        "menu.settings.language.en": "Engels",
        "menu.settings.language.de": "Duits",
        "menu.settings.language.pt": "Portugees",
        "menu.settings.language.es": "Spaans",
        "menu.settings.language.fr": "Frans",
        "menu.settings.language.it": "Italiaans",
        "menu.settings.language.nl": "Nederlands",
        "menu.settings.language.pl": "Pools",
        "menu.settings.language.tr": "Turks",
        "menu.settings.language.ru": "Russisch",
    },
    "pl": {
        "menu.settings.language.en": "Angielski",
        "menu.settings.language.de": "Niemiecki",
        "menu.settings.language.pt": "Portugalski",
        "menu.settings.language.es": "Hiszpański",
        "menu.settings.language.fr": "Francuski",
        "menu.settings.language.it": "Włoski",
        "menu.settings.language.nl": "Niderlandzki",
        "menu.settings.language.pl": "Polski",
        "menu.settings.language.tr": "Turecki",
        "menu.settings.language.ru": "Rosyjski",
    },
    "tr": {
        "menu.settings.language.en": "İngilizce",
        "menu.settings.language.de": "Almanca",
        "menu.settings.language.pt": "Portekizce",
        "menu.settings.language.es": "İspanyolca",
        "menu.settings.language.fr": "Fransızca",
        "menu.settings.language.it": "İtalyanca",
        "menu.settings.language.nl": "Felemenkçe",
        "menu.settings.language.pl": "Lehçe",
        "menu.settings.language.tr": "Türkçe",
        "menu.settings.language.ru": "Rusça",
    },
    "ru": {
        "menu.settings.language.en": "Английский",
        "menu.settings.language.de": "Немецкий",
        "menu.settings.language.pt": "Португальский",
        "menu.settings.language.es": "Испанский",
        "menu.settings.language.fr": "Французский",
        "menu.settings.language.it": "Итальянский",
        "menu.settings.language.nl": "Нидерландский",
        "menu.settings.language.pl": "Польский",
        "menu.settings.language.tr": "Турецкий",
        "menu.settings.language.ru": "Русский",
    },
}

PATCH_EXISTING = {
    "Messages_en.properties": {
        "menu.settings.language.nl": "Dutch",
        "menu.settings.language.pl": "Polish",
        "menu.settings.language.tr": "Turkish",
        "menu.settings.language.ru": "Russian",
    },
    "Messages.properties": {
        "menu.settings.language.nl": "Dutch",
        "menu.settings.language.pl": "Polish",
        "menu.settings.language.tr": "Turkish",
        "menu.settings.language.ru": "Russian",
    },
    "Messages_de.properties": {
        "menu.settings.language.nl": "Niederländisch",
        "menu.settings.language.pl": "Polnisch",
        "menu.settings.language.tr": "Türkisch",
        "menu.settings.language.ru": "Russisch",
    },
    "Messages_pt_PT.properties": {
        "menu.settings.language.nl": "Neerlandês",
        "menu.settings.language.pl": "Polaco",
        "menu.settings.language.tr": "Turco",
        "menu.settings.language.ru": "Russo",
    },
    "Messages_es.properties": {
        "menu.settings.language.nl": "Neerlandés",
        "menu.settings.language.pl": "Polaco",
        "menu.settings.language.tr": "Turco",
        "menu.settings.language.ru": "Ruso",
    },
    "Messages_fr.properties": {
        "menu.settings.language.nl": "Néerlandais",
        "menu.settings.language.pl": "Polonais",
        "menu.settings.language.tr": "Turc",
        "menu.settings.language.ru": "Russe",
    },
    "Messages_it.properties": {
        "menu.settings.language.nl": "Olandese",
        "menu.settings.language.pl": "Polacco",
        "menu.settings.language.tr": "Turco",
        "menu.settings.language.ru": "Russo",
    },
}

NEW_MENU_KEYS = (
    "menu.settings.language.nl",
    "menu.settings.language.pl",
    "menu.settings.language.tr",
    "menu.settings.language.ru",
)


def load_translations() -> dict[str, dict[str, str]]:
    raw: dict[str, dict[str, str]] = json.loads(JSON_DATA.read_text(encoding="utf-8"))
    result: dict[str, dict[str, str]] = {loc: {} for loc in ("nl", "pl", "tr", "ru")}
    for key, locs in raw.items():
        for loc in ("nl", "pl", "tr", "ru"):
            result[loc][key] = locs[loc]
    for key, nl, pl, tr, ru in MISSING_ENTRIES:
        result["nl"][key] = nl
        result["pl"][key] = pl
        result["tr"][key] = tr
        result["ru"][key] = ru
    for key, fixes in FIXES.items():
        for loc, val in fixes.items():
            result[loc][key] = val
    for loc, menu in MENU_LANGUAGE_LABELS.items():
        result[loc].update(menu)
    return result


TRANSLATIONS = load_translations()


def escape_property_value(value: str) -> str:
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
    for line in source_lines[1:]:
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
    text = "\n".join(out) + "\n"
    if "menu.settings.language.nl=" not in text:
        lines = text.splitlines()
        new_lines: list[str] = []
        inserted = False
        for ln in lines:
            new_lines.append(ln)
            if not inserted and ln.startswith("menu.settings.language.it="):
                for ek in NEW_MENU_KEYS:
                    new_lines.append(f"{ek}={escape_property_value(trans[ek])}")
                inserted = True
        if not inserted:
            raise RuntimeError(f"{locale}: anchor menu.settings.language.it= not found")
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
        if line.startswith("menu.settings.language.it="):
            insert_idx = i + 1
            break
    if insert_idx is None:
        for i, line in enumerate(lines):
            if line.startswith("menu.settings.language.pt="):
                insert_idx = i + 1
                break
    if insert_idx is None:
        raise RuntimeError(f"{filename}: anchor menu.settings.language.it/pt= not found")
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
    if not JSON_DATA.is_file():
        print(f"JSON not found: {JSON_DATA}", file=sys.stderr)
        return 1

    source_lines = SOURCE.read_text(encoding="utf-8").splitlines()
    source_keys = parse_source_keys(SOURCE)
    patch_only = set(NEW_MENU_KEYS)
    source_base = [k for k in source_keys if k not in patch_only]

    for loc in ("nl", "pl", "tr", "ru"):
        missing = [k for k in source_base if k not in TRANSLATIONS[loc]]
        if missing:
            print(f"Missing keys for {loc}: {missing[:10]}… ({len(missing)} total)", file=sys.stderr)
            return 1

    for loc in ("nl", "pl", "tr", "ru"):
        out_path = I18N_DIR / f"Messages_{loc}.properties"
        content = build_output(loc, source_lines)
        out_path.write_text(content, encoding="utf-8")
        print(f"Wrote {out_path} ({len(content.splitlines())} lines)")

    for filename, entries in PATCH_EXISTING.items():
        patch_existing_bundle(filename, entries)
        path = I18N_DIR / filename
        print(f"Patched {path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
