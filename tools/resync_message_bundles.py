#!/usr/bin/env python3
"""
Rebuild Messages_<locale>.properties from Messages_en.properties (key order + comments).

Priority per key:
  1. Existing value in the target bundle (if present)
  2. translation_aliases.json (semantic copy from another Java key)
  3. English template value

Use after adding keys to Messages_en.properties or when bundles have duplicate
English blocks at the file end. For full Swift parity run sync_java_messages_from_swift.py
when ../VisuStruct-swift is available.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
JAVA_I18N = REPO_ROOT / "src/main/resources/de/visustruct/i18n"
SOURCE = JAVA_I18N / "Messages_en.properties"
ALIASES_PATH = Path(__file__).resolve().parent / "translation_aliases.json"

LOCALE_HEADERS = {
    "Messages_de.properties": "# de — from VisuStruct-swift (de.lproj)",
    "Messages_es.properties": "# es — from VisuStruct-swift (es.lproj)",
    "Messages_fr.properties": "# fr — from VisuStruct-swift (fr.lproj)",
    "Messages_it.properties": "# it — from VisuStruct-swift (it.lproj)",
    "Messages_nl.properties": "# nl — from VisuStruct-swift (nl.lproj)",
    "Messages_pl.properties": "# pl — from VisuStruct-swift (pl.lproj)",
    "Messages_tr.properties": "# tr — from VisuStruct-swift (tr.lproj)",
    "Messages_ru.properties": "# ru — from VisuStruct-swift (ru.lproj)",
    "Messages_ko.properties": "# ko — from VisuStruct-swift (ko.lproj)",
    "Messages_ja.properties": "# ja — from VisuStruct-swift (ja.lproj)",
    "Messages_el.properties": "# el — from VisuStruct-swift (el.lproj)",
    "Messages_uk.properties": "# uk — from VisuStruct-swift (uk.lproj)",
    "Messages_ar.properties": "# ar — from VisuStruct-swift (ar.lproj)",
    "Messages_he.properties": "# he — from VisuStruct-swift (he.lproj)",
    "Messages_sv.properties": "# sv — from VisuStruct-swift (sv.lproj)",
    "Messages_da.properties": "# da — from VisuStruct-swift (da.lproj)",
    "Messages_nb.properties": "# nb — from VisuStruct-swift (nb.lproj)",
    "Messages_zh_Hans.properties": "# zh-Hans — from VisuStruct-swift (zh-Hans.lproj)",
    "Messages_zh_Hant.properties": "# zh-Hant — from VisuStruct-swift (zh-Hant.lproj)",
    "Messages_pt_PT.properties": "# pt-PT — from VisuStruct-swift (pt-PT.lproj)",
}

SECTION_COMMENT_FALLBACK = {
    "# Structure block names (didactic; order = element types 0–9)": (
        "# Structure block names (didactic; order = element types 0–9)"
    ),
    "# Short palette labels (interface language preset)": (
        "# Short palette labels (interface language preset)"
    ),
}


def parse_properties(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    if not path.is_file():
        return out
    for line in path.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if not s or s.startswith("#") or "=" not in s:
            continue
        key, value = s.split("=", 1)
        out[key] = value.replace("\\n", "\n").replace("\\t", "\t")
    return out


def escape_property_value(value: str) -> str:
    return (
        value.replace("\\", "\\\\")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
    )


def parse_template_lines(path: Path) -> list[str]:
    return path.read_text(encoding="utf-8").splitlines()


def parse_template_keys(path: Path) -> list[str]:
    keys: list[str] = []
    for line in parse_template_lines(path):
        s = line.strip()
        if not s or s.startswith("#") or "=" not in s:
            continue
        keys.append(s.split("=", 1)[0])
    return keys


def load_alias_config() -> tuple[dict[str, list[str]], set[str]]:
    if not ALIASES_PATH.is_file():
        return {}, set()
    cfg = json.loads(ALIASES_PATH.read_text(encoding="utf-8"))
    aliases = {k: list(v) for k, v in cfg.get("aliases", {}).items()}
    strip_set = set(cfg.get("stripEllipsisFromSource", []))
    return aliases, strip_set


def alias_value(
    key: str,
    aliases: dict[str, list[str]],
    strip_sources: set[str],
    existing: dict[str, str],
    en: dict[str, str],
) -> str | None:
    for source_key in aliases.get(key, []):
        if source_key not in existing:
            continue
        val = existing[source_key]
        if source_key in strip_sources:
            val = val.rstrip("…").rstrip("...").strip()
        return val
    return None


def build_translations(
    existing: dict[str, str],
    en: dict[str, str],
    keys: list[str],
    aliases: dict[str, list[str]],
    strip_sources: set[str],
) -> dict[str, str]:
    trans: dict[str, str] = {}
    for key in keys:
        en_val = en.get(key, "")
        has_locale = key in existing and existing[key] != en_val
        if has_locale:
            trans[key] = existing[key]
            continue
        derived = alias_value(key, aliases, strip_sources, existing, en)
        if derived is not None and derived != en_val:
            trans[key] = derived
            continue
        if key in existing:
            trans[key] = existing[key]
        elif key in en:
            trans[key] = en[key]
        else:
            raise KeyError(key)
    return trans


def build_output(
    java_file: str,
    source_lines: list[str],
    trans: dict[str, str],
) -> str:
    header = LOCALE_HEADERS.get(java_file, f"# {java_file}")
    out: list[str] = [header]
    for line in source_lines[1:]:
        stripped = line.strip()
        if not stripped:
            out.append("")
            continue
        if stripped.startswith("#"):
            out.append(SECTION_COMMENT_FALLBACK.get(stripped, stripped))
            continue
        if "=" not in stripped:
            out.append(line)
            continue
        key = stripped.split("=", 1)[0]
        out.append(f"{key}={escape_property_value(trans[key])}")
    return "\n".join(out) + "\n"


def target_files() -> list[Path]:
    skip = {"Messages_en.properties", "Messages.properties"}
    return sorted(
        p for p in JAVA_I18N.glob("Messages_*.properties") if p.name not in skip
    )


def main() -> int:
    if not SOURCE.is_file():
        print(f"Missing {SOURCE}", file=sys.stderr)
        return 1

    aliases, strip_sources = load_alias_config()
    en = parse_properties(SOURCE)
    keys = parse_template_keys(SOURCE)
    source_lines = parse_template_lines(SOURCE)

    for path in target_files():
        existing = parse_properties(path)
        trans = build_translations(existing, en, keys, aliases, strip_sources)
        content = build_output(path.name, source_lines, trans)
        path.write_text(content, encoding="utf-8")
        localized = sum(1 for k in keys if trans.get(k) != en.get(k))
        print(f"Wrote {path.name} ({localized}/{len(keys)} keys differ from en)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
