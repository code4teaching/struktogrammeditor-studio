#!/usr/bin/env python3
"""
Build/update Java Messages_*.properties from VisuStruct-swift Localizable.strings
and Settings.bundle (java_swift_i18n_map.json).

Priority per key: Swift (mapped) → existing Messages_<locale> → Messages_en.
Messages_en.properties is the structural template (not overwritten).
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SWIFT_ROOT = REPO_ROOT.parent / "VisuStruct-swift"
MAP_CANDIDATES = (
    SWIFT_ROOT / "tools" / "java_swift_i18n_map.json",
    REPO_ROOT / "tools" / "java_swift_i18n_map.json",
)
JAVA_I18N = REPO_ROOT / "src/main/resources/de/visustruct/i18n"
SWIFT_VISU = SWIFT_ROOT / "VisuStruct-iOS" / "VisuStructXcodeOpen" / "VisuStruct"
SOURCE = JAVA_I18N / "Messages_en.properties"

ENTRY_RE = re.compile(r'^"([^"]+)"\s*=\s*"(.*)"\s*;\s*$')

SECTION_COMMENT_FALLBACK = {
    "# Structure block names (didactic; order = element types 0–9)": (
        "# Structure block names (didactic; order = element types 0–9)"
    ),
    "# Short palette labels (interface language preset)": (
        "# Short palette labels (interface language preset)"
    ),
}


def menu_property_key(java_tag: str) -> str:
    if java_tag == "pt_PT":
        return "menu.settings.language.pt"
    return f"menu.settings.language.{java_tag}"


def unescape_strings(value: str) -> str:
    return value.replace("\\n", "\n").replace('\\"', '"').replace("\\\\", "\\")


def parse_strings(path: Path) -> dict[str, str]:
    if not path.is_file():
        return {}
    out: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        m = ENTRY_RE.match(line.strip())
        if m:
            out[m.group(1)] = unescape_strings(m.group(2))
    return out


def parse_properties(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if not s or s.startswith("#") or "=" not in s:
            continue
        k, v = s.split("=", 1)
        out[k] = v.replace("\\n", "\n").replace("\\t", "\t")
    return out


def escape_property_value(value: str) -> str:
    return (
        value.replace("\\", "\\\\")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
    )


def load_swift_locale(cfg: dict, swift_loc: str) -> tuple[dict[str, str], dict[str, str]]:
    lproj = cfg["swiftLocaleDirs"][swift_loc]
    strings = parse_strings(SWIFT_VISU / lproj / "Localizable.strings")
    root = parse_strings(SWIFT_VISU / "Settings.bundle" / lproj / "Root.strings")
    return strings, root


def menu_label_for_lang(java_lang: str, strings: dict[str, str], root: dict[str, str], cfg: dict) -> str | None:
    sk = cfg.get("menuLanguageSwiftKeys", {}).get(java_lang)
    if sk and sk in strings:
        return strings[sk]
    rk = cfg.get("menuLanguageRootKeys", {}).get(java_lang)
    if rk and rk in root:
        return root[rk]
    return None


def all_menu_keys(cfg: dict) -> list[str]:
    return [menu_property_key(tag) for tag in cfg["javaUiLanguageTags"]]


def build_translations_for_locale(cfg: dict, swift_loc: str, java_file: str) -> dict[str, str]:
    strings, root = load_swift_locale(cfg, swift_loc)
    existing_path = JAVA_I18N / java_file
    existing = parse_properties(existing_path) if existing_path.is_file() else {}
    en = parse_properties(SOURCE)

    trans: dict[str, str] = {}
    for item in cfg["entries"]:
        sk, jk = item["swiftKey"], item["javaKey"]
        if sk in strings:
            trans[jk] = strings[sk]
    for item in cfg.get("settingsBundleEntries", []):
        rk, jk = item["rootKey"], item["javaKey"]
        if rk in root:
            trans[jk] = root[rk]

    for java_tag in cfg["javaUiLanguageTags"]:
        label = menu_label_for_lang(java_tag, strings, root, cfg)
        if label:
            trans[menu_property_key(java_tag)] = label

    menu_keys = set(all_menu_keys(cfg))
    all_keys = parse_source_keys(SOURCE)
    for key in all_keys:
        if key in menu_keys:
            continue
        if key not in trans:
            if key in existing:
                trans[key] = existing[key]
            elif key in en:
                trans[key] = en[key]

    for mk in all_menu_keys(cfg):
        if mk not in trans:
            if mk in existing:
                trans[mk] = existing[mk]
            elif mk in en:
                trans[mk] = en[mk]

    return trans


def parse_source_keys(path: Path) -> list[str]:
    keys: list[str] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        if "=" in s:
            keys.append(s.split("=", 1)[0])
    return keys


def append_missing_menu_keys(text: str, trans: dict[str, str], menu_keys: list[str]) -> str:
    lines = text.splitlines()
    last_menu_idx = None
    present = set()
    for i, ln in enumerate(lines):
        if ln.startswith("menu.settings.language."):
            last_menu_idx = i
            present.add(ln.split("=", 1)[0])
    if last_menu_idx is None:
        raise RuntimeError("No menu.settings.language.* anchor in template")
    to_add = [mk for mk in menu_keys if mk not in present]
    if not to_add:
        return text if text.endswith("\n") else text + "\n"
    new_lines = list(lines)
    for offset, mk in enumerate(to_add):
        if mk not in trans:
            raise KeyError(f"Missing menu label for {mk!r}")
        new_lines.insert(last_menu_idx + 1 + offset, f"{mk}={escape_property_value(trans[mk])}")
    return "\n".join(new_lines) + "\n"


def build_output(swift_loc: str, source_lines: list[str], trans: dict[str, str], cfg: dict) -> str:
    header = f"# {swift_loc} — from VisuStruct-swift ({cfg['swiftLocaleDirs'][swift_loc]})"
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
        if key not in trans:
            raise KeyError(f"Missing translation for {key!r} ({swift_loc})")
        out.append(f"{key}={escape_property_value(trans[key])}")
    return append_missing_menu_keys("\n".join(out) + "\n", trans, all_menu_keys(cfg))


def patch_bundle_menu_keys(path: Path, labels: dict[str, str]) -> None:
    lines = path.read_text(encoding="utf-8").splitlines()
    existing_keys = set()
    last_menu_idx = None
    for i, line in enumerate(lines):
        s = line.strip()
        if s.startswith("menu.settings.language.") and "=" in s:
            existing_keys.add(s.split("=", 1)[0])
            last_menu_idx = i
    if last_menu_idx is None:
        return
    to_add = [(k, v) for k, v in labels.items() if k not in existing_keys]
    if not to_add:
        return
    new_lines = list(lines)
    for offset, (k, v) in enumerate(to_add):
        new_lines.insert(last_menu_idx + 1 + offset, f"{k}={escape_property_value(v)}")
    path.write_text("\n".join(new_lines) + "\n", encoding="utf-8")


def resolve_map_path() -> Path | None:
    for path in MAP_CANDIDATES:
        if path.is_file():
            return path
    return None


def main() -> int:
    map_path = resolve_map_path()
    if map_path is None:
        print(
            "Map not found. Expected one of:\n"
            + "\n".join(f"  - {p}" for p in MAP_CANDIDATES)
            + "\nClone VisuStruct-swift next to this repo, or run:\n"
            "  python3 tools/resync_message_bundles.py",
            file=sys.stderr,
        )
        return 1
    if not SWIFT_ROOT.is_dir():
        print(
            f"VisuStruct-swift not found at {SWIFT_ROOT}\n"
            "Clone the iOS repo as a sibling, then re-run this script.\n"
            "Until then: python3 tools/resync_message_bundles.py",
            file=sys.stderr,
        )
        return 1
    cfg = json.loads(map_path.read_text(encoding="utf-8"))
    source_lines = SOURCE.read_text(encoding="utf-8").splitlines()
    targets = cfg.get("javaTargetLocales", {})

    en_strings, en_root = load_swift_locale(cfg, "en")
    en_menu_labels: dict[str, str] = {}
    for tag in cfg["javaUiLanguageTags"]:
        label = menu_label_for_lang(tag, en_strings, en_root, cfg)
        if label:
            en_menu_labels[menu_property_key(tag)] = label

    for ref in ("Messages_en.properties", "Messages.properties"):
        patch_bundle_menu_keys(JAVA_I18N / ref, en_menu_labels)
        print(f"Patched menu language names in {ref}")

    for swift_loc, java_file in targets.items():
        if swift_loc not in cfg["swiftLocaleDirs"]:
            print(f"Unknown swift locale {swift_loc!r}", file=sys.stderr)
            return 1
        trans = build_translations_for_locale(cfg, swift_loc, java_file)
        content = build_output(swift_loc, source_lines, trans, cfg)
        out_path = JAVA_I18N / java_file
        out_path.write_text(content, encoding="utf-8")
        mapped = sum(1 for item in cfg["entries"] if item["swiftKey"] in load_swift_locale(cfg, swift_loc)[0])
        print(f"Wrote {out_path.name} ({len(content.splitlines())} lines, {mapped}/{len(cfg['entries'])} mapped)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
