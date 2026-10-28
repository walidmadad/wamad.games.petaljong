# CODEX 13 — Endless levels and forward-only progression

## A. Executive summary

Extends the existing native game rather than rebuilding it. The five introductions remain exact definitions; Level 6 onward is generated on demand. Victory is permanently recorded and only Next Level may progress. Level selection and Replay are removed. Restart is guarded for unfinished boards. Credits, engine rules, botanical artwork and animation code are preserved. Nothing is published; no ads or dependencies added.

## B–F. Generator, layouts, solvability, reproducibility and difficulty

`Levels.get(Long)` supports positive signed 64-bit identities through Long.MAX_VALUE. Its eventual limit is explicit and overflow-safe; no arbitrary catalog ending or mathematical-infinity claim. Generator v4 delegates to the existing v1 construction/v2 quality machinery; introductions retain v3 Level 1 and v2 Levels 2–5.

The eight reusable families are winding river terraces, stepped gardens, layered crosses, asymmetric wings, split gardens, compact pyramids, offset rectangles and staggered gardens. Every eight levels the cycle changes reflections (horizontal/vertical) and upper-row offsets. Every transformed accepted board passes the existing geometry validator, including support, no same-layer overlaps, unique IDs and even symbol counts. Base shapes have 20 tiles; 4 upper tiles through Level 24, then 8. Counts stabilize at 28 for portrait play; the introductions include a 30-tile board.

Seed derivation is frozen in `EndlessGenerator.seed`: SplitMix64 on level identity using its fixed add/multiply/xor constants, then XOR version 4. Signed multiplication/addition deliberately wrap as 64-bit mixing operations; level increment never wraps. Seed, identity and generator version fully reproduce the board without time or global random state. Geometry uses `(id - 6) % 8` and `(id - 6) / 8`, safe over the supported range.

Construction finds a geometrically legal complete removal sequence, assigns matching symbols to its pairs using local `java.util.Random`, then replays the witness through production `Game.match`. Validation requires every move to succeed, zero remaining tiles and WON. Rejected candidates never enter play. Analyzer metadata records bounded consequence choices, risks, unknown alternatives, nodes and target outcome. Targets rise from 4 consequence choices before Level 25 to 6 before Level 100 and 8 thereafter; scoring also penalizes forced play and requires proven risk for preferred candidates. Quality targets are preferences: if none meets them the best verified candidate is retained. Human fairness/difficulty is not proven by these metrics.

At most six quality candidates, each at most 64 geometry attempts and 2000 analysis nodes, are tried for endless levels. Each geometry attempt removes at most 14 pairs. On a preferred-generation exception, one deterministic v1 construction (64 attempts, same geometry/seed) provides a witness-verified fallback. Total bound is 448 construction attempts plus 12000 analyzer nodes. If even fallback fails, a controlled error retains victory and allows retry. Forced fallback is covered by deterministic tests.

Novelty compares the current slot-coordinate set against four preceding deterministic layouts, with no historical boards stored or recursive generation. The eight-shape schedule makes these geometries distinct; exact boards therefore cannot repeat in that window, regardless of symbol assignment. Tests check the schedule across reflection/size changes. This is a four-level guarantee, not global uniqueness forever. Symbols and removal order additionally vary by seed.

## G–K. Progression, victory, navigation, saves and Undo

Schema 3 stores an O(1) completion frontier, one active Long identity/seed/version, current accepted moves and global credit balance. A fully replayed winning history plus completion frontier represents pending victory. It does not store completed boards. A small Set<Int> adapter remains for legacy first-five tests/callers only.

`ProgressController` validates current WON against authoritative frontier, derives exactly +1, validates the definition, prepares the next save, and rejects stale/double commits. The UI holds a process-wide transition flag across recreation. The ordered AtomicFile writer confirms the new state before board installation; failed writes retain the original pending completion. During this transaction ordinary UI saves are suppressed. Process death restores either the previous completed state or its exact successor. Numeric overflow is guarded; the final representable victory says the numeric limit is reached, retaining progress.

Cold loading/restoration and Next generation run on worker threads with a “Preparing garden…” state. New boards call the existing board load path, clearing selection/effects and refreshing counters. Activity recreation uses the same process-owned controller and refresh listener; durable state wins over Bundle selection, whose identity is now Long. Obsolete listeners are released on destruction.

The level label is passive; its selection dialog and its click action are deleted. Victory has no Replay view. Restart delegates to guarded session restart and repeats the identical initial tiles without changing identity, history from prior puzzles, or credits. NO_MOVES does not advance. Undo remains disabled after WON.

Schemas 1/2 migrate using the contiguous completed prefix beginning with 1. Missing/out-of-order completions do not invent wins or skip gaps. All five completed enters 6; compatible active history at the first unfinished level is replayed and retained. A valid pending victory remains pending. A legacy selection of a completed board advances to the first unfinished level. Corrupt/incompatible active history resets only the unfinished attempt while retaining frontier and credits. A corrupt pending history cannot reopen the completed board; it recovers to the first unfinished board. At the numeric ceiling recovery reconstructs the recorded completed state instead of reopening it. Invalid stored balances conservatively recover to zero under schemas 2/3. Unsupported/malformed whole envelopes retain the existing conservative recovery limitations.

Three initial credits remain in `UndoCreditWallet`. Successful Undo spends one; failed Undo spends none. Transitions, restart and relaunch never grant credits. Production wallet and reward-free build dependencies are unchanged.

## L–O. Performance, tests, Android and APK

JVM **PASS: 168 tests**, 14.658 seconds, with a 49-identity matrix including 1–40, 50, 100, 500, 1000, 10000, 2147483648, 1000000000000, Long.MAX_VALUE−1 and Long.MAX_VALUE. Every accepted board in the matrix is validated and its full witness replayed. Tests also cover deterministic equality, four-level geometry novelty, unique IDs/even symbols, rejected corruption/impossible geometry, verified fallback, hit testing at 284×300 / 360×520 / 600×260, exact progression, stale commits, restart/deadlock, schema migration, pending victory and global credits. Existing terminal/replay expectations were replaced with forward-only assertions; engine regression suites remain.

Host workflow **PASS: 10 tests**, 0.803 seconds. Standard Gradle tasks were attempted but blocked at the wrapper lock in the read-only home directory. The Java 17 diagnostic builder succeeded: main/resource compilation, JUnit, Android test compilation, D8, ZIP alignment and signing. Android Lint is not executed by that builder.

Android instrumentation: **27 test methods compiled, NOT EXECUTED**. Real coordinate-tap coverage now runs beyond Level 5 through generated Levels 6/7, checks Next double taps, counts, same-level restart, generated history/credit restoration and pending victory recreation. ADB failed to create its socket listener (`Operation not permitted`); no device installation, launch, screenshots, Android timings or runtime PASS is claimed. Source-level artwork/effects preservation and JVM geometry checks cannot substitute for device visual evidence.

Final representative **host JVM** generation observations (one measured generation per row, warm process; not Android performance):

| Level | Generation ms |
|---|---:|
| 6 | 93 |
| 10 | 20 |
| 25 | 28 |
| 50 | 61 |
| 100 | 28 |
| 500 | 70 |
| 1000 | 23 |
| 10000 | 226 |
| 2147483648 | 283 |
| 1000000000000 | 48 |
| 9223372036854775807 | 17 |

The matrix includes both target-met and verified best-candidate outcomes, retaining honest quality metadata. No host timing is an Android responsiveness guarantee. Next generation logs actual worker elapsed time under `TileBloomGeneration` when run on Android.

New APK: `app/build/outputs/apk/debug/app-debug.apk`.

- Size: 754,067 bytes.
- Modification: 2026-10-28T17:41:31.176300+00:00 (newly built this iteration).
- SHA-256: `92219f6b2146eef2ad06e3611f3f676163e2b108fe9a954d3317d290fb7b6438`.
- Package: `com.tilebloom.game`, min SDK 26, target/compile SDK 36, version code 1 / name 0.1.
- `zipalign -c -v 4`: PASS.
- `apksigner verify --verbose --print-certs`: PASS, v2/v3 signatures, one RSA Android Debug signer.
- Signing certificate SHA-256: `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6` (existing diagnostic debug key retained).

Evidence: [offline build](docs/codex13/offline-build.txt), [host tests](docs/codex13/host-tests.txt), [Gradle restriction](docs/codex13/gradle.txt), [ADB restriction](docs/codex13/adb.txt), [package](docs/codex13/package.txt), [alignment](docs/codex13/alignment.txt), [signature](docs/codex13/signature.txt).


## P. Git

Uses existing `.tilebloom-git`, preserving parent faf28a0 and all history. No reinitialization, rewriting, push or publishing. Commit: `feat: add endless Mahjong levels and forward-only progression`.

## R–S. Limitations and next iteration

Local app data is the source of truth; uninstall, data clearing, external backup rollback or total save-envelope corruption cannot provide tamper-proof monotonic progress. Legacy noncontiguous records conservatively retain only the contiguous prefix. New generator versions must keep v4 available or explicitly migrate compatible histories. Bounded quality analysis does not prove all choices safe or a universally increasing human difficulty curve. Layout cycles are finite and long-range repeats are possible. Android runtime verification and a physical-phone playtest are the next priority; tune quality targets only after recording actual enjoyment, fairness and generation latency. No ads are recommended for this iteration.

## Q. Acceptance criteria

PASS entries for domain behavior are backed by JVM tests/source review. They do not imply Android execution. NOT VERIFIED explicitly identifies runtime-dependent checks.

| # | Criterion | Status | Evidence / limit |
|---|---|---|---|
| 1 | Mahjong rules preserved | PASS | Production Game/Rules unchanged; witness and regression tests |
| 2 | Botanical artwork preserved | NOT VERIFIED | Drawing source unchanged; Android visual check pending |
| 3 | Match animations preserved | NOT VERIFIED | Effects source unchanged; JVM effects tests pass; device observation pending |
| 4 | Undo economy preserved | PASS | Wallet unchanged; persistence/transition regressions |
| 5 | Five introductions valid | PASS | Versioned fixtures and full production witness replay |
| 6 | Level 6 available | PASS | Generated and replayed in JVM matrix |
| 7 | Beyond Level 5 progression | PASS | 5→6, 99→100, 999→1000, 9999→10000 tests |
| 8 | No artificial final level | PASS | Long identities; only explicit representational ceiling |
| 9 | Exact +1 increment | PASS | Authoritative validation and stale commit tests |
| 10 | Completed levels cannot replay | PASS | Session guards; Replay view removed; pending state tests |
| 11 | Completed levels cannot select | PASS | Selection API rejects other identities; no selector UI |
| 12 | Previous navigation removed | PASS | Dialog/click action deleted; no previous action |
| 13 | Restart only unfinished current | PASS | Session guard and exact board/identity tests |
| 14 | Deterministic generation | PASS | Independent repeated generation throughout matrix |
| 15 | Verified witness on every acceptance | PASS | Production validator at generation/install; replay matrix |
| 16 | Bounded generation | PASS | Explicit attempt/node/tile bounds; impossible layout test |
| 17 | Layout variety | PASS | Eight families, reflection/offset cycle, tested geometry |
| 18 | Recent exact duplicates controlled | PASS | Four preceding generated geometries compared; bounded cycle tests |
| 19 | Meaningful difficulty analysis | PASS | Consequence/risk/forced-choice targets; human fairness unverified |
| 20 | Android rendering adapts to layouts | NOT VERIFIED | JVM bounds/hit testing pass at three sizes; real UI unexecuted |
| 21 | Durable progress monotonic | NOT VERIFIED | Frontier/transaction logic tested/reviewed; AtomicFile interruption runtime pending |
| 22 | Existing saves migrate | PASS | Schema 1/2 contiguous/noncontiguous/compatible/corrupt tests |
| 23 | Current unfinished Android restoration | NOT VERIFIED | Exact JVM histories/counters pass; process relaunch unexecuted |
| 24 | Pending victory Android restoration | NOT VERIFIED | JVM pending history passes; recreation test compiled only |
| 25 | Credits preserved on transition | PASS | Global balance and history isolation regressions |
| 26 | Double taps cannot skip | PASS | Process gate + stale commit rejection; UI test compiled, runtime pending |
| 27 | JVM regressions | PASS | See final verification results |
| 28 | Instrumentation coverage updated | PASS | Compiled; actual coordinate taps through Levels 1–7, generated restart/save/Undo, pending recreation, double taps |
| 29 | New verified APK | PASS | Diagnostic build, package, size/hash, alignment and signing checks |
| 30 | Git history preserved | PASS | Existing repository and parent retained; no push |

## Unrestricted-host installation and instrumentation

```sh
# Keep the installed player data. Stop on a signing mismatch; do not uninstall.
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.tilebloom.game/.presentation.MainActivity
# Standard build of the isolated instrumentation APK on the unrestricted host:
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.tilebloom.game.test/android.test.InstrumentationTestRunner
adb logcat -d -s TileBloomGeneration:I
```

The installed app must match the instrumentation compile target/signature. If the standard Gradle build produces a differently signed application, install its matching debug app with `-r` only after confirming certificate compatibility. Never erase user data to force installation. Instrumentation uses `test-smoke-progress`, not the real save file. Diagnostic output compiles test classes but does not package an executable test APK; the host Gradle step is required.
