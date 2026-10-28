# CODEX 15 — Four-slot tile tray

## A. Executive summary

The existing Kotlin Android project now uses a four-slot collection tray above the
Mahjong board. A legal single tap transfers a tile. Identical held symbols clear in
pairs; four different held symbols cause LOST. No ads, dependencies, publishing,
remote pushes, project recreation or data clearing were introduced.

## B–E. State architecture, selection, matching and defeat

`Game` owns initial tile identities, active/removed board flags, ordered `tray`, derived
`clearedTiles`, completed pairs and accepted individual `selections`. `traySlots` always
contains exactly four nullable positions. `select(id)` returns acceptance and updates
all logical state synchronously. Duplicate, removed, covered and blocked IDs are rejected.
The existing geometric `Rules.available` and topmost Canvas hit testing remain the source
of legality. Removal immediately exposes newly legal board tiles.

Matching compares symbol identity (whose explicit stable IDs remain catalog contracts).
The oldest matching held tile and incoming tile become one cleared pair; remaining held
tiles retain insertion order. Matching finishes before status evaluation. Four occupied
slots therefore contain distinct symbols and cause immediate LOST. The four tiles remain
visible outside the defeat overlay. The centered message offers Restart Level; global
Undo remains visible in the footer. No automatic restart or advancement occurs.

The pair `match` adapter remains for historical generator analysis/regression tools. It
validates an available pair then calls the same two production selections. Android taps
use only `select`; no alternative gameplay implementation bypasses the tray.

## F. Victory and deadlocks

WON requires an empty board and empty tray. An exhausted board with unmatched held tiles
is LOST. A board with no legal individual tile is also LOST. Absence of an available
matching board pair does not stop play. NO_MOVES is retained as a source-compatible
historical enum value but is never emitted by live tray rules.

`PuzzleAnalyzer.analyzeTray` delegates to bounded `TrayAnalyzer`: available tiles,
unmatched occupancy, immediate fourth-symbol risks, newly exposed matching symbols,
proven safe/losing/unknown first continuations and memoized order-dependent search.
Search keys combine remaining IDs and held symbol multiset. Budget exhaustion stays
UNKNOWN and never proves loss. Live play conservatively permits legal actions rather
than running expensive search after each tap. Some doomed attempts remain playable
until an explicit terminal condition is reached.

## G. Undo

Undo truncates one accepted tile selection and validates complete replay on a fresh
candidate before committing. This restores exact board identity, tray ordering, cleared
pairs and legal availability, including a matching selection or fourth-symbol defeat.
The existing wallet charges one credit only after successful replay. Zero credits,
failed Undo, restart, advancement, recreation and relaunch never allocate credits.
Undo after WON remains disabled to preserve the permanent completion frontier.

## H. Saves and migration

Schema 4 stores frontier, level, seed, generator version, gameplay rules version 2,
individual IDs and global credits. Tray, cleared pairs and status are derived by replay;
no transient animation is saved. Ordered AtomicFile persistence is unchanged.
Schemas 1–3 are explicitly parsed as pairs and expanded into ordered individual taps.
Replay validates the exact compatible puzzle. Invalid history or incompatible identity
uses the existing conservative recovery at the current unfinished frontier. Credits and
completed levels survive; completed boards never reopen. Legacy generator v4 restoration
and current v5 identities remain supported. Activity Bundle selection restoration was
removed because a selection is now a durable action, not transient highlight state.

## I–J. Generator and difficulty

Every `LevelValidator` call now replays the complete pair witness as individual production
selections, checks an empty tray after every pair and checks WON at the end. Candidate
rejection remains in the existing generator. All tier profiles, 20-level boundaries,
seeds, layouts, capped board sizes and sixteen symbol types remain unchanged.
The historical pair-quality score remains frozen to preserve deterministic versions;
tray analysis is an additional advisory API. Human difficulty calibration under the
new rules is pending playtesting; symbol count alone is not claimed to measure difficulty.

## K. UI and responsive layout

`TileTrayView` is presentation only: four centered rounded forest-green slots, muted-gold
borders and warm ivory tiles using the same Canvas botanical renderers as the board.
The tray sits between status and flexible board, outside the centered outcome overlay.
Compact screens hide the decorative subtitle, shorten status/footer spacing and use a
58 dp tray; larger layouts use 78 dp. System bars retain existing safe-inset handling.
Board transfer ghosts fade using existing `MatchEffects`; tray insertion pulses and
clearing symbols briefly fade. Fourth-slot borders change color. Undo/restart cancel
feedback; recreation restores only logic. Animations never perform engine mutations.
Small-phone tap comfort and final animation appearance require actual Android review.

## L–M. Tests and Android status

**PASS: 180 JVM tests**, final suite time 52.162 seconds. Generation matrices,
legacy fixtures, progression/credit regressions, geometry, gestures, effects and new tray
cases passed. Full log: `docs/codex15/offline-verification.txt`.

Standard Gradle is blocked by the read-only wrapper cache lock. The repository's Java 17
cached Kotlin offline verifier is used instead. Coordinate instrumentation sources are
compiled; tests use board geometry transformed to screen coordinates. Existing coverage
was updated for individual Undo and legal continuation without a board pair. New
`TrayInteractionTest` covers slots, insertion, matching, defeat, Undo and durable replay.
Android runtime/instrumentation: **NOT EXECUTED**. ADB cannot bind its socket in this sandbox.
Android Lint: **NOT EXECUTED**. No device screenshots or runtime performance are claimed.

## N. Fresh APK

**PASS.** `app/build/outputs/apk/debug/app-debug.apk`

- Timestamp: 2026-10-28 19:27:57 +01:00.
- Size: 774,547 bytes.
- SHA-256: `d8aebcc2ac8ef94f32c1f0c4aca1721b697224334187045819ae1d5e9df73527`.
- Package: `com.tilebloom.game`, versionCode 1 / versionName 0.1.
- ZIP alignment: PASS. APK signatures v2 and v3: PASS (local debug certificate).
- APK is newer than all production Kotlin/XML sources. DEX contains `TileTrayView`,
  `TrayAnalyzer`, tray-slot accessors and individual-selection history/access methods.
- Detailed evidence: `docs/codex15/apk-verification.txt`.
- Installation/launch: NOT EXECUTED because ADB socket access is blocked.

Host installation, preserving application data:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tilebloom.game/.presentation.MainActivity
```

## O. Git

Existing `.tilebloom-git` history retained (the sandbox `.git` directory is not the
repository metadata). Commit message: `feat: add four-slot tile tray and matching gameplay`.
Nothing pushed or published. Final commit identifier is supplied with delivery.

## P. Acceptance criteria

| # | Criterion | Status |
|---|---|---|
| 1 | Four-slot tray | PASS |
| 2 | Tray above board | PASS (layout code; device appearance pending) |
| 3 | One legal tap transfers tile | PASS |
| 4 | Mahjong availability | PASS |
| 5 | Automatic identical pair clearing | PASS |
| 6 | Four different symbols lose | PASS |
| 7 | Three symbols stay playable with legal board continuation | PASS |
| 8 | No duplicate tray tiles | PASS |
| 9 | Blocking updates after one selection | PASS |
| 10 | Empty board and tray required for victory | PASS |
| 11 | Tray-compatible deadlock behavior | PASS (conservative live detection) |
| 12 | Individual Undo | PASS |
| 13 | Undo after defeat | PASS |
| 14 | Persistent three-credit economy | PASS |
| 15 | Tray save replay | PASS (JVM; device relaunch pending) |
| 16 | Legacy migration | PASS |
| 17 | Endless forward progression | PASS |
| 18 | Existing tiers | PASS |
| 19 | Expanded botanical symbols | PASS |
| 20 | Generated tray winning witnesses | PASS |
| 21 | Portrait phone usability | NOT VERIFIED on device |
| 22 | Matching animations reliable | NOT VERIFIED visually; state isolation covered |
| 23 | JVM regression suite | PASS |
| 24 | Updated Android UI coverage | PASS (compiled; NOT EXECUTED) |
| 25 | Fresh verified APK | PASS |
| 26 | Git history retained | PASS |
| 27 | Nothing published | PASS |

## Q–R. Limitations and next iteration

Run the compiled coordinate tests on an emulator/device, execute Android Lint with normal
Gradle access, and playtest 320/360 dp portrait layouts with large fonts. Tune tray-aware
quality targets in a future explicitly versioned generator after measuring real player
failure rates. Current seeds and tiers deliberately retain their historical quality
ranking. No exhaustive impossibility search runs on the UI thread. Device installation
and signature continuity against a separately signed existing installation require host
verification; do not uninstall or clear data to work around a signature mismatch.
