# CODEX 09 — persistent progression and reliable saves

## A. EXECUTIVE SUMMARY

Added durable local progress, exact move-history reconstruction, sequential unlocking and a five-level selector. The existing engine, puzzle definitions, analyzer, artwork and match-effect code remain unchanged. Human testing and Android runtime verification remain outstanding.

## B. PERSISTENCE ARCHITECTURE

PlayerProgress and ProgressController are pure Kotlin. ProgressRepository uses Android AtomicFile at private files/player-progress-v1. One process-wide executor orders all writes. Captures occur after board callbacks and every restart/replay/advance/level change. Identical snapshots are deduplicated, so selection changes do not cause disk writes. Nothing is serialized per animation frame. Activity-owner tokens reject submissions from obsolete activities. onStop drains already queued writes without submitting state.

## C. SAVE FORMAT

UTF-8 schema 1, four newline-separated fields: schema number; comma-separated completed IDs; active ID, signed Long seed, generator version; semicolon-separated pairs of integer tile IDs. Optional move field defaults empty. Selection, removed-board copies, counters and status are excluded. Active level and highest unlocked are derived from validated records and identity, avoiding contradictory duplicated counters. Reads are capped at 8192 characters. Typical records are a few hundred bytes at most.

## D. PROGRESSION SYSTEM

Only the contiguous completed prefix beginning at Level 1 grants unlocks. Unknown IDs and completion gaps do not unlock arbitrary levels. A production-engine WON capture adds completion before the victory UI updates. Level 5 completion retains final victory and all five unlocked levels. NO_MOVES never adds completion. Replay/restart leave completion records intact.

## E. LEVEL SELECTION

Tap the level number and downward marker. Forest-green/ivory/gold panel lists exactly five levels with completed/current/unlocked/locked labels. Locked rows are disabled and the controller independently rejects them. Current level returns unchanged. Selecting another level requires an explicit confirmation that the single current attempt is replaced with a fresh deterministic attempt; completed achievements remain. Return to game and system Back dismiss the panel. New board loading clears effects, touch state, selection and feedback through existing board.load behavior. Actual layout/insets require Android inspection.

## F. GAME RESTORATION

Load durable state, normalize completions, validate active access and exact puzzle identity, replay every accepted pair on a candidate Game, then apply the candidate only if the whole history succeeds. Engine recomputes counter, WON and NO_MOVES. No animations are resumed. Winning history remains on its victory screen without automatic advancement. Invalid active snapshots restart the same supported level where possible, otherwise Level 1, retaining independent valid completions. Malformed board records without a readable ID fall back to Level 1.

## G. GENERATOR COMPATIBILITY

Exact Levels.get definition, version and seed must match. No move is replayed against a new arrangement. Current definitions remain unchanged from CODEX 08. Incompatible versions reset only the attempt; no historical-definition migration is invented. Existing generator-v1 fixtures remain available to engine tests, but are not accepted as current persisted introductory identities.

## H. SAVE RELIABILITY

AtomicFile protects replacement writes; ordered writes prevent an older queued snapshot overwriting a newer one. Rejected histories never partially mutate the engine. Missing files start fresh; malformed envelopes and unsupported schemas recover fresh with development logs. Independently parseable completion records survive board corruption. Unsupported envelope schemas cannot safely preserve completion records yet. Errors log under TileBloomSave, set lastWriteFailed, and do not show a false success message. Failed writes are eligible for retry on the next meaningful capture; no retry loop exists.

An abrupt kill or power failure before the asynchronous writer finishes can lose the latest queued action while retaining the previous atomic file. Normal onStop drains writes, but is not guaranteed on force-kill. Storage exhaustion can prevent durability. This implementation does not promise synchronous per-touch fsync or protection from app-data clearing/uninstall. No device crash/power-loss experiments were possible.

## I. ANDROID LIFECYCLE

Durable state (including newest process-memory queued snapshot) is authoritative, rather than a competing Bundle move history. The Bundle can restore a selection only when level, seed, version and full move list exactly equal durable state. Obsolete callbacks cannot submit saves due to owner tokens. onStop waits for queued small writes; slow or failing storage could delay departure and requires device measurement. Ordinary app updates preserve this private file if app data and signing identity are retained. The first CODEX 09 launch cannot migrate a closed CODEX 08 session because it never had a permanent file.

## J. GAMEPLAY REGRESSION

No engine, generator, analyzer, five puzzle identity, witness, symbol, board geometry or match-effect implementation changed. Existing victory panel/footer visibility and restyled Next/Replay remain. Existing deadlock identity and restart behavior remain. All original JVM tests pass. Visual/runtime claims remain unverified for the new APK.

## K. AUTOMATED TEST RESULTS

Diagnostic Kotlin 1.9.24/JUnitCore: 127 tests PASS (104 original + 23 new). Includes original 120-board generation regression and new exact Level 3 history/counter/identity, unlocks, completion reload, replay/restart retention, deadlock, final victory, schema/defaults, malformed identity, seed/version mismatch, duplicate/invalid moves and repeated reconstruction. tools/test_host_verify.py: 10 tests PASS. These fake-tool workflow checks do not establish Android behavior.

Standard tools/host_verify.sh: FAILED at gradle-version, read-only ~/.gradle wrapper lock. Prerequisite-dependent testDebugUnitTest, assembleDebug and lintDebug NOT EXECUTED. No standard Gradle or Lint success claimed. Diagnostic tools/verify_offline.py built the updated APK separately.

## L. ANDROID INSTRUMENTATION

14 methods compiled (all original 10 retained + 4 new); 0 executed. New tests cover real AtomicFile reload with process cache cleared, completion after replay, storage-failure reporting, and finish/startActivitySync relaunch without a Bundle. Existing tests cover geometry-derived board taps, deadlock, victory, recreation and all-five progression. Storage is isolated from player data and cleaned. Fresh-process relaunch, selector dialog interaction, update preservation and fault-injection execution still require host/manual verification. ADB server startup failed with Operation not permitted; no repeated sandbox attempts. No current APK screenshot or CODEX 08 UI runtime evidence was fabricated.

## M. PERFORMANCE

No device persistence latency, frame-time, or lifecycle-wait measurements available. Snapshot format is bounded and small; writes run serially off the touch thread and are deduplicated. Serialization and existing game-status computation run on the UI thread; onStop waits for pending writes. Measure these on device before claiming no noticeable interruption.

## N. APK OUTPUT

Updated debug APK: `app/build/outputs/apk/debug/app-debug.apk` — **745,875 bytes**. Build: `python3 tools/verify_offline.py`, cached Kotlin 1.9.24 diagnostic compiler, SDK 36 / Build Tools 35.0.0; not a standard Gradle build. Package: `com.tilebloom.game`, versionCode 1 / versionName 0.1, min SDK 26 / target 36 (aapt2 badging verified). zipalign 4-byte check PASS; apksigner verification PASS, one signer, v2 and v3 signatures. SHA-256: `4e112e3896c5844f7aac2b0afb48f32782aba94d3dffb4eb448ecf84468b29a7`. No installation or runtime verification performed.

## O. GIT STATUS

Parent: c35bc84ab340de5178ddd2038baea646f368f68d. Existing .tilebloom-git metadata used without reinitialization, history rewriting, pushing or publishing. Final commit hash is reported in the delivery message (this report belongs to that commit). Working tree verified after commit.

## P. ACCEPTANCE CRITERIA

PASS on implementation/JVM criteria is scoped to that evidence; Android-specific outcomes remain NOT VERIFIED.

| # | Result | Evidence / limits |
|---|---|---|
| 1 | PASS | Existing Mahjong rules unchanged; Game.kt untouched, engine regressions pass. |
| 2 | PASS | All five complete witnesses and 120-board generator regressions pass. |
| 3 | NOT VERIFIED | Match-effect implementation unchanged and JVM regressions pass; runtime animation behavior unverified. |
| 4 | PASS | All original 104 JVM tests pass. |
| 5 | PASS | Permanent AtomicFile storage implemented; Android durability execution pending. |
| 6 | PASS | Save schema 1 explicitly encoded and checked. |
| 7 | PASS | Exact unfinished reconstruction verified through production engine JVM tests; Android pending. |
| 8 | NOT VERIFIED | Completion reload rules pass; application relaunch not executed on Android. |
| 9 | NOT VERIFIED | Unlock persistence logic passes; Android accessibility/navigation pending. |
| 10 | PASS | Controller rejects locked levels and adapter disables locked rows; UI execution pending. |
| 11 | NOT VERIFIED | Level selector implemented; actual Android panel/touch behavior unverified. |
| 12 | PASS | Replay retains completion in JVM tests; Android pending. |
| 13 | PASS | Restart retains progression in JVM tests; Android pending. |
| 14 | PASS | Production deadlock path retains locks in JVM regression. |
| 15 | PASS | Version/seed mismatch resets active attempt and retains valid completion prefix. |
| 16 | PASS | Corrupt/duplicate/unknown move histories rejected atomically in JVM tests. |
| 17 | NOT VERIFIED | Parser/controller recovery tested; actual Android corrupted-file recovery not executed. |
| 18 | NOT VERIFIED | Exact-board Bundle selection policy implemented; recreation test compiled only. |
| 19 | PASS | Victory/final victory reconstruct through engine in JVM tests; Android panel pending. |
| 20 | NOT VERIFIED | No transient effects serialized; Android animation/persistence interaction pending. |
| 21 | NOT VERIFIED | Ordered background writes implemented; no device latency measurements. |
| 22 | NOT VERIFIED | Single victory panel/footer logic preserved; current APK not visually inspected. |
| 23 | PASS | 23 new JVM persistence/progression regressions pass. |
| 24 | PASS | 14 instrumentation methods compiled; zero executed, explicitly reported. |
| 25 | PASS | New diagnostic debug APK generated and alignment/signatures verified. |
| 26 | PASS | Diagnostic build clearly separated from blocked standard Gradle/Lint. |
| 27 | PASS | Structured five-level observation/opinion worksheets updated; no invented feedback. |
| 28 | PASS | Existing .tilebloom-git history preserved; descendant commit only. |

## Q. KNOWN LIMITATIONS

Android, Lint, fresh-process durability, storage-failure behavior and selector presentation await real execution. Only one active attempt is stored. No schema migration beyond v1, no historical puzzle restore for changed live identities, and no cloud backup. Async crash window and onStop storage wait are described above. Storage failures use development diagnostics; no dedicated player-facing recovery UI. Device tests are compiled, not executed. Selection-only changes are intentionally not permanent.

## R. HUMAN PLAYTEST READINESS

PLAYTEST_01.md now separates five-level measured outcomes (completion/time/restarts/deadlocks) from opinions (clarity, symbol distinction, planning, fairness, difficulty 1–5, desire to continue). Includes multi-session and victory relaunch scenarios. No human feedback supplied; no puzzle seed or difficulty changes made. HOST_VERIFICATION.md contains exact host install, connected-test, force-stop/relaunch and log commands.

## S. CODEX 10 RECOMMENDATION

First execute standard Gradle/Lint and all instrumentation on an authorized Android host, visually review selector/victory/animations, and measure write/drain latency. Run the five-level human worksheet across several sessions. Prioritize any observed save defect and use real fairness/planning/deadlock feedback to decide whether clearer availability teaching or future deadlock recovery is needed. Do not rebalance seeds from automated metrics alone; do not add monetization.
