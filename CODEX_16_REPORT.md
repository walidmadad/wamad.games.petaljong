# CODEX 16 — Level start screen and premium icon controls

## A. Executive summary

Continued the existing native Kotlin Android project. Added a botanical level-start
screen, an explicit Play gate, original vector controls and a compact Undo credit badge.
No domain engine, generator, tier policy, symbol catalog, persistence codec/repository
or wallet logic was changed. CODEX 15's tray is present in production source and remains
active. No advertising, external UI/icon libraries, publishing or pushes were introduced.

## B. New level-start screen

A fresh Activity entry without restored navigation shows TileBloom, Mahjong Garden
Puzzle, original static botanical artwork, the authoritative LEVEL X and a prominent
rounded gold PLAY button with a filled triangle. The level binding is a Long from the
loaded `ProgressController.session.level`; Level 1 is not hardcoded. A contextual short
hint distinguishes a new attempt, unfinished attempt, saved defeat and pending victory.
The screen waits for validated progress loading before exposing Play.

## C. Play and resume

Play changes only `LevelNavigation` to GAME. It creates the gameplay view hierarchy
once; subsequent Home/Play cycles reuse the same board and game instance. No seed,
generator version, history, tray, matching state or credit balance is changed. A hidden
Play button's repeated callback is guarded, so duplicate clicks cannot initialize an
additional board. Saved defeat opens its retained defeat state, and saved victory
opens its completed outcome; neither is automatically restarted.

## D–F. Icon system, Undo badge and Restart

Five original 24 dp VectorDrawables: Play, Next, curved Undo, circular Restart and Home.
Outline icons share 1.8-unit round strokes; Play uses a solid triangle. Ivory/gold icons,
rounded green surfaces and ripple pressed feedback share `GardenStyle`. No emoji/font
icons or dependencies are used. `GardenIconButton` remains one accessible Button target;
its badge draws the actual wallet balance, including zero, without a second focus node.

Undo is disabled when no valid action or credit exists and while advancing. Successful
Undo invokes the existing wallet/controller then clears visual feedback. Restart invokes
the existing unfinished-session restart, keeps level/seed/credits, and clears board/tray
effects. Completed levels cannot restart. Footer controls are 64×56 dp, with consistent
spacing; primary buttons are at least 56 dp, with Play at least 64 dp high.

## G. Screen navigation and lifecycle

`presentation.board.LevelNavigation` stores only a level ID and START/GAME. It owns no
puzzle. Navigation is saved in the Activity Bundle, separate from durable gameplay.
Recreation/process restoration retains the screen when the authoritative level agrees;
stale or malformed UI state falls back to START at the actual current level. Background
and foreground of the same Activity do not change screens. A genuine new entry without
restored Activity state starts at START, even when the process-owned session is reusable.
Home saves without resetting and is unavailable on victory. System Back retains normal
Activity exit behavior; no level selection or completed-level replay is introduced.

## H. Victory and defeat

One centered Garden Cleared presentation with completed level and vector Next Level
button. The existing completion guard and ordered durable transition run before the new
level is installed. `GameStore.onChanged` synchronizes the current Activity to START for
the committed new identity, including rotation during generation. The next puzzle waits
for Play. Double taps are blocked by the process transition flag and exact-next guards.

Defeat keeps all four symbols visible above the outcome area. One footer Undo icon and
one Restart icon provide recovery; the old duplicate Restart action in the outcome card
was removed. Next is visible only on advanceable victory. Home from defeat preserves the
failed attempt and Play returns to that same outcome. Credits are never replenished.

## I. Four-slot tray compatibility

CODEX 15 was inspected: production `Game.select` transfers one legal tile, resolves an
identical pair before evaluating four distinct held symbols, and retains exact original
IDs. Replay reconstructs tray and Undo. This engine and its tests were preserved. The
existing Canvas board geometry, topmost hit testing, `MatchEffects`, tray insertion and
clearing effects remain active. Only descriptive accessibility text in board/tray views
was moved into string resources, using existing symbol labels.

## J. Persistent state integration

Schema 4, gameplay rules 2, generator versions, accepted selections and completion
frontier remain compatible without another migration. `ProgressRepository` ordered
AtomicFile saving and process-owned `GameStore` remain unchanged. UI navigation is not
serialized into gameplay saves and cannot erase them. Pending completed snapshots retain
WON, saved defeat retains LOST, and invalid gameplay recovery uses the existing policy.
Navigation itself neither regenerates levels nor performs puzzle analysis.

## K. Responsive layout

Welcome uses a fill-viewport ScrollView, centered content, a 420 dp composition cap on
wide devices, auto-sized title/level labels and up to two lines for large level IDs.
The artwork is shorter on compact/large-font configurations. Gameplay has a compact
serif title, stacked level/pair status, 58/72 dp tray, flexible board and aligned icon
footer. Compact mode applies below 740 dp height or above 1.3 font scale. System-bar and
cutout insets wrap both screens. Outcome content scrolls independently. Actual 320/360/
400 dp and large-font visual/touch usability remain NOT VERIFIED without Android access.

## L. Accessibility

String resources supply all new player-facing labels and descriptions. Home/Restart
have descriptions/tooltips; Undo describes the actual credit balance. Play and Next
retain short text labels plus icons. Decorative hero art is excluded from accessibility.
Disabled icons use a muted color while the badge remains high contrast. Full custom-board
screen-reader navigation, TalkBack focus behavior and contrast on a device are not claimed.

## M–N. Automated tests and Android status

**PASS: 186 JVM tests**, including all 180 existing regressions. Final test time:
30.672 seconds. Full evidence: `docs/codex16/offline-verification.txt`.

Six new combined JVM tests exercise authoritative large IDs, exact same-instance resume,
board/tray/history/seed/credits preservation, same-level recreation, pending victory,
exact-next/double-commit guards, stale restoration across advancement, saved defeat and
explicit Undo/restart. All prior geometry, engine, tray, credit, migration, generation
and difficulty coverage remains in the suite.

`LevelStartTest` adds seven Android tests for the gate, icon/badge/touch targets, exact
Home resume, coordinate victory and Next start, recreation/fresh relaunch, saved defeat,
pending victory and Long.MAX_VALUE label/Play access. The 32 existing Android tests are
retained and adapted to press Play and inspect badge values. Actual board actions use
geometry-derived screen coordinates. UI compilation: **PASS — all Android test sources compiled** (39 test methods; NOT EXECUTED). Android execution:
**NOT EXECUTED** — ADB cannot bind its local socket. Android Lint: NOT EXECUTED. Standard
Gradle fails on the read-only wrapper lock; the established cached Java 17 offline tool
provides regression execution, Android source compilation and debug APK packaging.

## O. APK output and verification

**PASS.** `app/build/outputs/apk/debug/app-debug.apk`

- Timestamp: 2026-10-28 19:44:01 +01:00.
- Size: 783,092 bytes.
- SHA-256: `7c261fc59eb9496417d7b3c96f7acc097b76823490429accde62acb5eebd0104`.
- Package: `com.tilebloom.game`; versionCode 1, versionName 0.1.
- ZIP alignment: PASS. APK v2/v3 signatures: PASS, existing local debug certificate.
- Freshness: newer than every production Kotlin/XML source.
- DEX: LevelNavigation, GardenIconButton, GardenWelcomeArt, createStartScreen,
  createGameplay, badge accessor and preserved TileTrayView verified present.
- All five vector drawable files are packaged; aapt2 confirms vector/path contents.
  Startup/Play/level/Home IDs and descriptive string resources are in the resource table.
- Detailed verification: `docs/codex16/apk-verification.txt`.
- Android installation/launch: NOT EXECUTED because ADB is blocked.

Host installation without clearing data:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tilebloom.game/.presentation.MainActivity
```

Run UI tests on a host with normal Gradle/ADB access:

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:connectedDebugAndroidTest
```

The offline tool compiles instrumentation but does not package or execute a test APK.
A separately signed installation may require the original signing identity for an
in-place upgrade; do not uninstall or clear data as a workaround.

## P. Git

Existing `.tilebloom-git` metadata and all history are preserved. Changes are reviewed
with `diff --check`, regression verification and APK evidence before commit. Commit
message: `feat: add level start screen and premium icon controls`. The final delivery
includes the resulting identifier. Nothing is pushed or published.

## Q. Acceptance criteria

PASS below denotes verified source/build/JVM evidence where applicable. Android-specific
appearance and interactions are NOT VERIFIED, even though code and compiled tests exist.

| # | Criterion | Result / evidence |
|---|---|---|
| 1 | Level-start screen exists | PASS — production implementation |
| 2 | Correct level displayed | NOT VERIFIED on Android; authoritative ID covered by JVM |
| 3 | Play starts/resumes correct level | NOT VERIFIED on Android; navigation invariants pass JVM |
| 4 | Unfinished progress preserved | PASS — exact board/tray/history/seed replay and navigation tests |
| 5 | Zen Garden visual identity | NOT VERIFIED visually; prescribed palette/art implemented |
| 6 | Prominent readable Play | NOT VERIFIED on device |
| 7 | Undo text replaced by icon | PASS — vector control implementation |
| 8 | Numeric Undo badge stays visible | NOT VERIFIED on device; bound to saved wallet |
| 9 | Restart text replaced by icon | PASS — vector control implementation |
| 10 | Icon controls understandable | NOT VERIFIED through device/user testing |
| 11 | Accessibility descriptions implemented | PASS — resource-backed descriptions/tooltips |
| 12 | Compact portrait usability | NOT VERIFIED on Android |
| 13 | Board geometry correct | PASS — unchanged geometry and existing regression suite |
| 14 | Existing animations preserved | NOT VERIFIED visually; effects logic unchanged/tests pass |
| 15 | Four-slot tray preserved | PASS — production rules unchanged, regressions pass |
| 16 | Victory screen correct | NOT VERIFIED on Android; pending-victory logic passes JVM |
| 17 | Defeat screen correct | NOT VERIFIED on Android; saved LOST/Undo logic passes JVM |
| 18 | Next advances exactly one | PASS — controller/transition navigation regression |
| 19 | Completed levels inaccessible | PASS — forward-only guards and regression coverage |
| 20 | Persistent progress intact | PASS — unchanged codec/repository, save replay tests |
| 21 | Navigation preserves Undo credits | PASS — navigation/credit tests |
| 22 | JVM suite passes | PASS — 186 tests |
| 23 | Android tests updated | PASS — sources compiled (execution NOT EXECUTED) |
| 24 | Fresh verified APK | PASS — freshness, package, alignment, signatures and content verified |
| 25 | Git history preserved | PASS |
| 26 | Nothing published | PASS |

## R. Known limitations

No emulator/device installation, screenshots, TalkBack, gesture interaction or actual
small-screen/font-scale validation occurred here. Icon recognition and visual balance
need real Android review. System Back exits the Activity; Home is the explicit in-game
return-to-start action. The save format does not persist presentation navigation across
a genuine fresh entry; this intentionally returns to the level-start screen. No settings
interface, currency systems, ads or new difficulty tuning were added. Platform test APIs
remain the existing deprecated instrumentation stack; no large test/UI dependency was
introduced for this iteration.

## S. Recommendation for CODEX 17

Run the compiled UI scenarios on real Android at 320/360/400 dp and font scales up to
200%, including recreation during Next Level. Capture welcome/gameplay/outcome screens,
review board touch comfort and TalkBack focus/announcements, then make evidence-based
spacing refinements. Keep gameplay rules and deterministic saves stable during that pass.
