# TileBloom — Mahjong Garden Puzzle

Native Kotlin Android Mahjong Solitaire with a Zen Garden identity. CODEX 13 adds on-demand, verified levels beyond the five preserved introductory boards. The only playable board is the current unfinished level. Victory offers **Next Level →**, advancing exactly one; completed boards cannot be selected or replayed. Restart repeats the same unfinished puzzle. No ads, accounts, publishing, or new dependencies.

Three initial Undo credits remain global and persistent. Successful Undo costs one; failed Undo costs nothing. Restart, victory, advancement, and relaunch never refill credits.

Level identities are positive Kotlin `Long` values, up to 9,223,372,036,854,775,807. The last representable identity is guarded against overflow; this is practical endless progression, not mathematical infinity. Levels 1–5 retain their seeds/versions and boards. Version 4 generates later levels deterministically using eight silhouettes, reflection and upper-terrace placement variants, 24–28 tiles, bounded quality analysis, and production-engine witness replay. Boards are generated on entry, not stored in a growing catalog.

Schema 3 stores a single completion frontier, active identity/seed/version, accepted move history, and credits. A verified winning history represents pending victory. Old schemas migrate from their contiguous completion prefix; the first missing level is never skipped. Compatible unfinished histories survive. Completed legacy selections move to the first unfinished level. Invalid histories recover without reopening a completed board. Private storage remains `player-progress-v1`; ordinary upgrades retain data, while uninstall/data clearing removes it.

Cold restoration and next-board generation run off the main thread. Next Level is guarded across Activity recreation; its AtomicFile transaction completes before the new board is installed. Failed generation/saving retains victory for retry. Botanical drawing, authentic Mahjong rules, touch geometry, and removal animations remain intact.

See [CODEX_13_REPORT.md](CODEX_13_REPORT.md), [VALIDATION.md](VALIDATION.md), and [PLAYTEST_01.md](PLAYTEST_01.md) for architecture, measured verification, limitations, and device checks. Earlier iteration reports are historical.

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
# Diagnostic builder for restricted environments (cached Kotlin 1.9.24):
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/verify_offline.py
python3 tools/test_host_verify.py
```

Normal toolchain: Kotlin 2.2.10, AGP 8.10.0, Gradle 8.13, Java 17, SDK 36, min SDK 26, build tools 35.0.0. The diagnostic builder compiles instrumentation sources but does not execute them or run Android Lint.
