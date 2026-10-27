# CODEX 12 — Current status

CODEX 12 replaces unlimited free Undo with three global initial credits. Each successful Undo costs one; zero-credit players can voluntarily watch a Google test rewarded ad for one credit. Restart stays free. Credits migrate with schema 2 and remain across levels and relaunch. Ads 25.5.0 and UMP 4.0.0 are integrated in source with test IDs only. **159 JVM tests and 10 host tests pass; SDK compilation, new APK, instrumentation and real ads are NOT VERIFIED.** See [CODEX_12_REPORT.md](CODEX_12_REPORT.md) for implementation, migration, consent and release requirements. Older unlimited-Undo/build statements below describe previous iterations.

---

# TileBloom — Mahjong Garden Puzzle

CODEX 11: unlimited Undo and deadlock recovery; redesigned Level 1 and five deterministic introductory Mahjong Solitaire levels with local progression, automatic continue, and a minimal level selector. Offline native Kotlin Android; no accounts, network permissions, advertising, or analytics.

Tap the level number to select a level. Current level continues unchanged. Another unlocked level starts fresh after confirmation and replaces the sole active attempt. Completion records remain saved. Locked levels cannot be selected.

Accepted moves, victories, advancement, replay, restart and level changes submit compact saves to an ordered background writer using Android AtomicFile in private app storage. Activity departure drains queued writes. Durable state takes precedence over the activity Bundle; only an exact-board selection highlight may come from the Bundle. Restored counters and status are recomputed by the engine.

Schema 1 stores a contiguous validated completion prefix, active level ID, exact seed, puzzle-definition version, and ordered accepted pair moves. Highest unlocked level is derived. Selection and animations are not stored permanently. Invalid board histories reset atomically to a verified board while retaining valid completion records. Unsupported schemas cannot be migrated yet. Uninstalling or clearing app data removes saves; ordinary updates preserving app data retain them.

Level 1 is now a 24-tile winding terrace over five stepped rows, with six caps. Its two safe opening pairs unlock different groups; the complete solution is verified through the production engine. Level 1 uses puzzle version 3, seed 123. CODEX 09 Level 1 histories restart on the new board because their tile geometry is incompatible; valid completions and unlocks survive. Levels 2–5 retain version 2 and compatible histories. Schema and storage filename remain unchanged.

The engine, artwork, renderer and match animations are unchanged. Sixty deterministic candidates were audited; see [CODEX_10_REPORT.md](CODEX_10_REPORT.md), [geometry comparison](docs/level1-layout.svg) and [candidate measurements](docs/codex10-candidates.csv). Human difficulty is unverified. See [PLAYTEST_01.md](PLAYTEST_01.md) for human testing, and [HOST_VERIFICATION.md](HOST_VERIFICATION.md) for host commands.

Validation: 149 JVM tests (139 existing + 10 Undo regressions) and 10 host-workflow tests pass using diagnostic tools. 18 Android instrumentation methods (15 existing + 3 Undo) are compiled only, not executed here. Standard Gradle and Lint are blocked by sandbox restrictions. No current APK has been visually validated here. See [CODEX_11_REPORT.md](CODEX_11_REPORT.md) and [VALIDATION.md](VALIDATION.md).

```sh
bash tools/host_verify.sh
python3 tools/test_host_verify.py
# Restricted-environment diagnostic compiler, separate from Gradle:
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/verify_offline.py
```

Stack: Kotlin 2.2.10 / AGP 8.10.0 / Gradle 8.13 / Java 17 / SDK 36, min SDK 26, Build Tools 35.0.0. Diagnostic builds use cached Kotlin 1.9.24. Source: domain engine and PlayerProgress; storage/ProgressRepository; presentation/MainActivity and MahjongBoardView. No new external dependencies. Historical iteration reports remain in the repository.

Undo replays the accepted history minus its final pair through validated engine restoration, then saves the shorter history. Selection and transient match effects are cleared. Repeated Undo returns to the initial board; rejected matches never count. The existing save schema also supports Undo after relaunch. No Undo action is exposed after victory; Replay starts fresh while completion and unlock records remain saved. Switching levels starts the selected puzzle fresh, as before.

See [CODEX_11_REPORT.md](CODEX_11_REPORT.md) for current verification and the full acceptance assessment. Android runtime and human difficulty feedback remain pending.
