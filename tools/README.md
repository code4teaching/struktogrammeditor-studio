# VisuStruct Java — maintenance scripts

## i18n (current)

| Script | Purpose |
|--------|---------|
| `sync_java_messages_from_swift.py` | Regenerate `Messages_*.properties` from VisuStruct-swift (`java_swift_i18n_map.json` in sibling repo). |
| `analyze_i18n_gaps.py` | List unmapped Java keys and unused Swift keys. |

Requires a local clone: `../VisuStruct-swift` (see root `.gitignore`).

```bash
python3 tools/sync_java_messages_from_swift.py
python3 tools/analyze_i18n_gaps.py
```

## Other

| Script | Purpose |
|--------|---------|
| `generate_logostr_png.py` | Regenerate logo PNG assets (rare). |

## Legacy

Superseded by Swift sync — kept for reference only: `legacy/`.
