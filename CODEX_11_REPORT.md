# CODEX 11 — Unlimited Undo and deadlock recovery

## A. Executive summary

Added unlimited Undo during play and deadlock recovery, using the existing validated engine history. Preserved all five levels, Level 1 version 3, seeds, Mahjong rules, botanical artwork, 150 ms match effects, saves and completion records. Nothing was published or pushed.

## B. Initial project audit

Started from clean commit `33501b4`. Inspected Game, Levels, PuzzleAnalyzer, PlayerProgress, ProgressRepository, MainActivity, MahjongBoardView, MatchEffects, unit/instrumentation suites, save codec and activity restoration. Game already records only successful matches and restores by candidate replay before atomic replacement. GardenSession validates level ID, seed and version. ProgressController retains completion independently of attempts. The repository uses AtomicFile and one ordered writer with activity-owner tokens. Board cleanup already cancels gestures, blocked feedback callbacks and match ghosts. No separate state/history framework was needed.

## C. Undo architecture

`Game.canUndo` requires accepted history and a non-WON board. `Game.undo()` passes history minus its final pair and null selection to existing `restore()`. All replay occurs on a candidate built from the immutable initial definition; failure returns false without committing any candidate state. The activity logs a reconstruction failure and retains the active board. No duplicate legality implementation or renderer tile mutations were added.

## D. Undo behavior

Exact IDs, symbols, coordinates and layers come from the original definition. Blocking, legal pairs, count and status are derived through production rules. Undo restores exactly two tiles, clears selection and supports repeated actions back to the initial board. Invalid moves and selections never become history. Victory disallows Undo; Replay starts fresh and retains recorded completion.

## E. Deadlock recovery

NO_MOVES displays “No More Matches” and “Try another approach.” with Undo Last Move and Restart Level. Successful Undo recomputes status and returns normal instructions when playable. Further Undo remains possible if another deadlock persists. No shuffle, regeneration or hint occurs. WON presentation and the deadlock footer are mutually exclusive.

## F. UI and visual design

Side-by-side weighted footer buttons preserve board space, with forest-green surfaces, gold borders, ivory labels, rounded corners and ripple feedback. Minimum touch height is 48 dp. Undo uses a readable muted disabled label and Android enabled state. It is disabled without history and hidden at victory; existing Next Level and Replay Level remain. Compact screens and large fonts still require device inspection.

## G. Animation integration

After a successful Undo, existing `board.clearFeedback()` clears ghosts, pending blocked-feedback callbacks and gesture state before refresh. Restoration is immediate. Existing match effects remain 150 ms and never affect engine availability or touch geometry. Main-thread event serialization prevents concurrent match/Undo mutations. Automated model coverage passes; Android animation timing is not executed here.

## H. Persistence integration

After Undo, `update()` captures the shortened authoritative history and submits it through the existing repository. Schema 1 and filename are unchanged; no second history exists. A single executor preserves submission order, so older submitted writes finish before the Undo write. Ownership tokens reject stale activity submissions. onStop drains submitted writes without recapturing old state. AtomicFile failures retain existing diagnostics and `lastWriteFailed`; no UI claims a confirmed durable write. Codec/reload tests confirm the removed move stays removed from the snapshot on a second restoration. Real process relaunch/storage durability needs Android execution.

## I. Level compatibility

Undo uses each active Game's exact initial definition, never another level's tiles. Restart/Replay clear history; advance/select create the destination game. Existing level selection starts other levels fresh. Level 1 keeps winding layout, seed 123, version 3 and five witness unlocking moves. Levels 2–5 keep their definitions and v2 identities. Old Level 1 snapshots still use existing migration protection; valid completions survive.

## J. Automated test results

JVM: **149 executed, 149 passed, 0 failed** (139 existing plus 10 Undo methods). Host-workflow: **10 executed, 10 passed, 0 failed**. Cross-layout Undo tests inspect every pre-victory witness prefix in reverse, including exact tiles, geometry, availability, legal pairs, counts and uniqueness. Other tests cover rejected moves, selection, deadlock, durable history truncation, completion preservation, navigation, atomic invalid restoration, rapid repetition/effect cleanup and unchanged deterministic witnesses.

Standard `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` stopped before any task at a read-only Gradle wrapper lock. Gradle build and Lint are **NOT VERIFIED**. No identical sandbox retry was made. The diagnostic pipeline initially encountered default Java 25 incompatibility; switching to installed JDK 17 resolved that compiler startup problem.

Evidence: `app/build/codex11/diagnostic.log`, `host-tests.log`, `gradle.log`, `adb.log`.

## K. Android instrumentation

Preserved all 15 previous methods and added three: Undo/pair/counter/selection/Restart, deadlock recovery, and fresh-activity durable restoration followed by Undo and disk reload. Tile actions use real geometry-derived MotionEvents; buttons use the presentation click path. Storage is test-isolated. Current compilation/execution result: **18 methods COMPILED BUT NOT EXECUTED**. ADB server failed to install its socket listener with Operation not permitted. **0 instrumentation tests executed; actual Android runtime NOT VERIFIED.**

## L. Performance

Host JVM measurement: 1,000 late-Level-5 prefix restore plus Undo cycles took **104.747208 ms** (~0.105 ms per combined cycle), including engine validation. This is a warm host measurement, not isolated Android Undo latency or a persistence benchmark. Reconstruction is bounded by the current accepted history (at most the small puzzle's pair count), with no new work in rendering or continuous timers.

## M. APK output

Diagnostic build **PASS**, using `tools/verify_offline.py` with JDK 17 (cached Kotlin compiler, aapt2, d8, zipalign and apksigner; not Gradle).

- Exact path: `/home/walid/Documents/projects/androidGames/TileBloom/app/build/outputs/apk/debug/app-debug.apk`
- Size: **745,875 bytes**.
- SHA-256: `f246a09c203308e1e900f2e48a92ec00f061183f144c65133701d68895a7205b`.
- `zipalign -c 4`: **PASS**.
- `apksigner verify --verbose`: **PASS**, v2 and v3 signatures, one debug signer.
- Resource inspection confirms packaged Undo and Undo Last Move strings and Undo ID.
- Standard Gradle build: blocked before task execution. Android Lint: not executed.
- Installation, launch and actual Android runtime: not executed.

## N. Git status

Existing history retained under `.tilebloom-git`; parent is `33501b4`. Final commit is the commit containing this report, subject `feat: add unlimited undo and deadlock recovery`; retrieve with `git --git-dir=.tilebloom-git log -1 --oneline`. Final hash and clean status are reported in the development response after commit. No remote push or publication.

## O. Acceptance criteria

PASS for engine criteria indicates host test/source evidence, not Android runtime validation.

| # | Criterion | Result |
|---|---|---|
| 1 | Existing Mahjong rules preserved | PASS |
| 2 | Five levels preserved | PASS |
| 3 | Redesigned Level 1 preserved | PASS |
| 4 | Complete solution witnesses valid | PASS |
| 5 | Last accepted match reversed | PASS |
| 6 | Exactly two tiles restored | PASS |
| 7 | Exact board geometry restored | PASS |
| 8 | Available moves updated | PASS |
| 9 | Repeated Undo to initial board | PASS |
| 10 | No-history Undo disabled | PASS (engine and UI binding; device pending) |
| 11 | Invalid matches excluded | PASS |
| 12 | Deadlock recovery | PASS (engine; UI execution pending) |
| 13 | PLAYING status restored when applicable | PASS |
| 14 | Version and seed preserved | PASS |
| 15 | Completion records preserved | PASS |
| 16 | Undo after durable-format restoration | PASS (codec and engine; device pending) |
| 17 | History consistent after real app relaunch | NOT VERIFIED |
| 18 | Restart and Replay | PASS (engine/controller; device pending) |
| 19 | Level transitions isolate history | PASS |
| 20 | Match animation reliability on Android | NOT VERIFIED |
| 21 | No duplicate tiles/moves | PASS |
| 22 | Compact-screen accessibility | NOT VERIFIED |
| 23 | Existing JVM tests | PASS |
| 24 | New Undo tests | PASS |
| 25 | Existing instrumentation preserved | PASS (18 compiled; 0 executed) |
| 26 | Updated debug APK generated and verified | PASS (diagnostic APK, alignment/signature) |
| 27 | Runtime status reported accurately | PASS |
| 28 | Git history preserved | PASS |

## P. Known limitations and manual instructions

No current APK was installed, launched or visually inspected on Android here. APK validation cannot establish persistence after process death, compact-screen fit, touch timing, perceived difficulty or enjoyment. Save ordering remains the existing tested/source-audited implementation; real filesystem failure and process-loss behavior require device verification. Individual TalkBack tile actions remain outside this iteration. See CODEX 11 additions in PLAYTEST_01.md and HOST_VERIFICATION.md for install-without-clearing-data, five-move relaunch/Undo/relaunch, deadlock, rapid fade input, level navigation, completion and migration checks. No human feedback was invented.

## Q. CODEX 12 recommendation

Prioritize host Gradle/Lint plus real Android execution, persistence across process relaunch and a short human Level 1 playtest. Collect observations on future unlocks, fair difficulty, recoverable mistakes and desire to continue to Level 2 before changing puzzle difficulty. The redesigned layout's enjoyment remains unverified. Keep recovery unlimited and free of monetization.
