#!/usr/bin/env python3
"""Report Java i18n keys without Swift map entries and unused Swift keys."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SWIFT_ROOT = REPO_ROOT.parent / "VisuStruct-swift"
MAP_PATH = SWIFT_ROOT / "tools" / "java_swift_i18n_map.json"
JAVA_EN = REPO_ROOT / "src/main/resources/de/visustruct/i18n/Messages_en.properties"
SWIFT_EN = (
    SWIFT_ROOT
    / "VisuStruct-iOS"
    / "VisuStructXcodeOpen"
    / "VisuStruct"
    / "en.lproj"
    / "Localizable.strings"
)
ENTRY_RE = re.compile(r'^"([^"]+)"\s*=\s*"(.*)"\s*;\s*$')


def parse_properties(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if not s or s.startswith("#") or "=" not in s:
            continue
        key, value = s.split("=", 1)
        out[key] = value
    return out


def parse_strings(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        m = ENTRY_RE.match(line.strip())
        if m:
            out[m.group(1)] = m.group(2)
    return out


def main() -> int:
    if not MAP_PATH.is_file():
        print(f"Map not found: {MAP_PATH}", file=sys.stderr)
        return 1
    cfg = json.loads(MAP_PATH.read_text(encoding="utf-8"))
    java = parse_properties(JAVA_EN)
    swift = parse_strings(SWIFT_EN)

    mapped_java = {e["javaKey"] for e in cfg["entries"]}
    mapped_java |= {e["javaKey"] for e in cfg.get("settingsBundleEntries", [])}
    menu_keys = {f"menu.settings.language.{tag}" for tag in cfg["javaUiLanguageTags"]}

    used_swift = {e["swiftKey"] for e in cfg["entries"]}
    used_root = {e["rootKey"] for e in cfg.get("settingsBundleEntries", [])}

    unmapped_java = sorted(k for k in java if k not in mapped_java and k not in menu_keys)
    unused_swift = sorted(k for k in swift if k not in used_swift)

    print(f"Java keys (en): {len(java)}")
    print(f"Mapped entries: {len(cfg['entries'])} swift + {len(cfg.get('settingsBundleEntries', []))} root")
    print(f"Unmapped Java keys: {len(unmapped_java)}")
    print(f"Unused Swift keys (en): {len(unused_swift)}")
    print()
    print("--- Unmapped Java (desktop-only / no Swift source yet) ---")
    for key in unmapped_java:
        print(key)
    print()
    print("--- Unused Swift keys (candidates for future map) ---")
    for key in unused_swift:
        print(f"{key}\t{swift[key][:72]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
