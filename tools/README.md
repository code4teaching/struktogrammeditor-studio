# VisuStruct Java — maintenance scripts

## i18n (current)

| Script | Purpose |
|--------|---------|
| `sync_java_messages_from_swift.py` | Regenerate `Messages_*.properties` from VisuStruct-swift (`java_swift_i18n_map.json`). |
| `resync_message_bundles.py` | Rebuild bundles from `Messages_en.properties` (keeps translations, applies `translation_aliases.json` for new keys). |
| `analyze_i18n_gaps.py` | List unmapped Java keys and unused Swift keys. |

Requires a local clone: `../VisuStruct-swift` (see root `.gitignore`) for full Swift sync.

```bash
# Full Swift parity (preferred when sibling repo is present):
python3 tools/sync_java_messages_from_swift.py
python3 tools/analyze_i18n_gaps.py

# Without VisuStruct-swift — reorder keys, drop duplicate EN blocks, alias new keys:
python3 tools/resync_message_bundles.py
```

## Other

| Script | Purpose |
|--------|---------|
| `generate_logostr_png.py` | Regenerate logo PNG assets (rare). |

## Legacy

Superseded by Swift sync — kept for reference only: `legacy/`.
