# CODEX 10 — five-level human playtest

No human results have been provided for this iteration. Leave blanks until observed. Automated solver outcomes do not establish enjoyment or fairness. Use the new APK identified in CODEX_10_REPORT.md and record device, Android version, APK hash/build, date and tester alias (no account required).

## CODEX 10 device and upgrade checklist

All items below are pending. Record APK SHA-256 from current VALIDATION.md, device/API/density/font scale and screenshots. Do not inspect the solution witness during the unaided difficulty session.

1. On compact (~320×568 dp), medium (~360×800 dp) and large (~412×915 dp) portrait phones, launch the winding board. Confirm five stepped base rows, two cap rows, no clipping, readable botanical symbols and comfortable taps. Record actual board viewport size; a short board viewport may shrink faces below 48 dp.
2. Observe the two opening associations. Did the player notice that each releases a different group? Did the middle caps and lateral blocks read clearly? Record blind matching versus deliberate choices, and any concealed information that made a failure feel unfair.
3. Remove several pairs; note exactly which IDs/positions and count. Press Home, force-stop via the commands below, relaunch. Confirm exact restored board, counter, no stale effects and functional touch targets.
4. Complete Level 1 through UI taps. Confirm one victory panel and Level 2 unlocked. Exit/force-stop/relaunch; confirm completion retained. Replay/restart Level 1, then select unlocked Level 2 and confirm the correct board. A replay must not revoke completion.
5. For an actual upgrade, first use CODEX 09 on a development device with the same signing key. Complete Levels 1 and 2, replay Level 1 and remove several pairs. Preserve the save file and app data. Install CODEX 10 with `adb install -r`. Confirm Level 1 starts fresh v3, completed {1,2} and Level 3 unlock survive. Relaunch again; no repeated recovery. Never uninstall or clear data to bypass a defect or signature mismatch.
6. Repeat update with an unfinished Level 2 attempt. Its exact board/history must survive. Confirm stale v2 Level 1 activity Bundle does not resurrect selection or moves after migration.
7. Complete all five levels through geometry-derived instrumentation. Check Next, Replay, selector confirmation/cancellation, deadlock Restart, recreation mid-selection, effect positions after matches and effect cancellation on restart/level change. Use current definitions rather than historical coordinate lists below.
8. Ask the player whether new Level 1 still feels trivial, whether Level 2 is harder, and whether any restart resulted from hidden information. Record the answer even if it contradicts analyzer targets.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.tilebloom.game/.presentation.MainActivity
# After several pairs and Home:
adb shell am force-stop com.tilebloom.game
adb shell am start -W -n com.tilebloom.game/.presentation.MainActivity
adb shell run-as com.tilebloom.game cat files/player-progress-v1
```

| Check | Device/build | Observed result | Screenshot/log |
|---|---|---|---|
| Compact / medium / large fit | | | |
| Several pairs survive process restart | | | |
| Completion / unlocking survives restart | | | |
| Actual CODEX 09 Level 1 upgrade | | | |
| Compatible Level 2 upgrade | | | |
| Unassisted Level 1 difficulty | | | |
| Level 1 versus Level 2 | | | |

Play Levels 1–5 in order without following a solution witness. Record a restart when Restart or Replay begins a new attempt; record every deadlock. Time only if measured, noting interruptions. Do not change seeds during this evaluation.

Measurable observations:

| Level | Completed (yes/no) | Time (if measured) | Restarts | Deadlock count | Restoration matched after closing? |
|---|---|---|---|---|---|
| 1 | | | | | |
| 2 | | | | | |
| 3 | | | | | |
| 4 | | | | | |
| 5 | | | | | |

Player opinions (ask after each level):

| Level | Available tiles understandable? | Symbols distinguishable? | Needed planning? | Felt fair? | Difficulty 1–5 | Wanted to continue? |
|---|---|---|---|---|---|---|
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |
| 4 | | | | | | |
| 5 | | | | | | |

Difficulty anchors: 1 very easy, 2 easy, 3 moderate, 4 hard, 5 very hard. Record exact player comments separately; do not infer opinions from speed or completion.

For Level 1, specifically ask whether the revised symbol arrangement requires meaningful choices compared with the earlier easy version, if the tester played it. For a deadlock ask whether its cause was understandable and whether restarting felt acceptable. Avoid teaching a solution before recording that answer.

Multi-session checks: pause mid-Level 3 after five pairs, leave and fully close, then relaunch; compare tiles and counter. Close on Level 2 victory before Next and verify completion/unlock. Close on Level 5 victory and verify final panel and replay access. Revisit a completed level through the level-number selector and confirm the replacement wording is clear. Cancel once before accepting. Test a locked row and system Back. Record any save warning in Android logs separately from player opinion.

Do not fabricate feedback or rebalance from metrics alone. CODEX 10 decisions should follow the actual five-level results and deadlock observations.

---

Historical playtest instructions follow; their earlier observations do not validate CODEX 09.

# TileBloom — first Android playtest

Status: NO COMPLETED HUMAN PLAYTEST RECEIVED. User-reported emulator screenshots establish earlier-build rendering and progression (10 then 7 pairs), not a completed playtest or validation of the CODEX 06 APK. Leave feedback empty until a person plays on Android.
Install and launch using HOST_VERIFICATION.md. This is a debug prototype with one fixed level; choices can lead to no more matches. No hints, undo or shuffle are available.

Tester: __________  Date: __________
Device / Android version: __________  Font size: __________
APK / commit: __________  Session length: __________

Mark each item after trying it; record unexpected behavior below. Let the player
try unaided first. The explanations and winning walkthrough are tester aids;
record any help given so it is not mistaken for immediate understanding.

- [ ] 1. Open TileBloom. It stays open without an error.
- [ ] 2. Observe the initial board: 24 tiles, two overlapping layers, 12 remaining pairs.
- [ ] 3. Identify available tiles: uncovered, with an open left or right side. Blocked tiles have corner bars.
- [ ] 4. Tap one available tile. A gold outline appears.
- [ ] 5. Tap it again. The outline disappears.
- [ ] 6. Tap two available tiles with different symbols. Selection moves to the second; neither disappears.
- [ ] 7. Tap two available identical symbols.
- [ ] 8. Observe both tiles fade and shrink briefly (150 ms); with system animations disabled they disappear immediately. Check that no other tile disappears and the counter updates immediately.
- [ ] 9. Check Remaining Pairs decreased by one.
- [ ] 10. Tap a blocked tile. Brief border feedback appears; selection and count remain unchanged.
- [ ] 11. Continue playing. Try tile centers, edges, overlap regions and adjacent boundaries. A visible upper tile should receive the touch.
- [ ] 12. Complete the level. If stuck, Restart and use the walkthrough below.
- [ ] 13. Observe Garden Cleared and Play Again, with zero pairs remaining.
- [ ] 14. Tap Play Again / Restart.
- [ ] 15. Check the exact original board and 12 pairs return, with no selection.
- [ ] 16. Try rapid touches, dragging away and back, and two fingers. Check there are no accidental or duplicate matches. Restart during blocked feedback, then match again.
- [ ] 17. Make a match, select a tile and leave using the Home button.
- [ ] 18. Return using Recents.
- [ ] 19. Check removed tiles, selected outline, symbols and counter remain consistent.

Winning walkthrough: remove upper-layer pairs left to right, pairing the two rows in each column. Then remove lower-layer columns left to right; in each column pair rows 1/2, then rows 3/4. Restart first if needed. There are twelve matches.

Repeat on compact, typical and large portrait phones when available. At larger system font size, check all tiles, counters, instructions and Restart remain visible and usable. Record device details separately for each run. Do not mark these checks from mathematical viewport tests alone.

## Human feedback

Objective understood without explanation? __________
Available versus blocked tiles understood? __________
Symbols easy to distinguish? __________
Tiles comfortable to touch? __________
Selected tile obvious? __________
Matching satisfying? __________
Board easy to understand? __________
First level requires meaningful decisions? __________
Game feels relaxing? __________
Would you willingly play another level? __________
Deadlock message understood? __________
Restart intuitive? __________
Help / walkthrough used? __________

## Objective technical observations

Unexpected behavior / reproduction steps: __________
Screenshots / device logs: __________

## Subjective player impressions

Most confusing moment: __________
Suggested improvement: __________

## CODEX 06 targeted checks (not yet performed)

- [ ] Inspect the larger serif title, separate status strip, ivory tile rims and gold-bordered Restart on the newly built APK.
- [ ] Check compact portrait, typical and large screens at normal and increased font scale; record actual dp, density, API and font scale. Check terminal messages too.
- [ ] Make rapid successive matches during fading; check legal new selections and correct counts. Restart during a fade, then match again.
- [ ] Recreate/background during a fade; no old visual copies should reappear.
- [ ] Disable system animator duration in developer settings, match again and confirm immediate disappearance; restore the previous setting.
- [ ] Capture initial, selected, victory and deadlock screens; record animation smoothness and player impressions separately from engineering checks.

## CODEX 07 — five-garden progression (2026-10-27)

This section supersedes earlier single-level status descriptions. The production `Game.kt`, original LevelOne assignments, solution and deadlock witnesses, and all 67 original JVM tests remain unchanged.

### Architecture and guarantee

`domain/Levels.kt` contains TileSlot, BoardLayout, LevelDefinition, LevelGenerator, LevelValidator, Levels and GardenSession. Android dependencies remain outside this domain. A definition records level ID, seed, generator version, layout, assigned tiles, complete witness and attempt count.

Generation chooses pairs of geometrically available slots using production `Rules.available`, then assigns a balanced shuffled bag of the six existing botanical symbols to those pairs. Validation checks slot identity, finite coordinates, unique IDs, even counts, same-layer non-overlap, supported upper tiles and genuine interlayer overlap. Every witness is replayed using production `Game.match`; acceptance requires an empty board and WON. Every pair must therefore match symbols, contain distinct IDs and be legal before removal. Initial solvability guarantees at least one complete solution; player choices can still deadlock.

Version 1 uses java.util.Random seeded by seed XOR level ID, ordered slot lists and explicit Fisher-Yates library shuffle. The same ID, seed, layout and version reproduce assignments and witness on JVM/Android. Changes to layouts, ordering, random consumption or algorithm require a version change for existing content. This is a bounded foundation, not an unlimited production generator.

There are at most 64 attempts and at most 16 pair removals per attempt for accepted input sizes of 2–32 slots. There is no recursion, exhaustive solver or frame-time generation. Each attempt discards a stuck ordering or invalid candidate. Exhaustion raises a controlled error; there is no unrelated fallback board. The Next action retains the current completed board if preparation fails and shows an error. The verified original level is the restoration fallback. Tiny supported definitions are prepared synchronously; phone timing remains unmeasured.

### Introductory definitions

| Level | Seed | Version | Layout | Tiles | Design intent |
|---|---:|---:|---|---:|---|
| 1 | 0 | 0 (fixed) | Original | 24 | Unchanged introductory reference |
| 2 | 202 | 1 | Terrace | 24 | Wider stepped rows, shallow upper patch |
| 3 | 303 | 1 | Woodland | 28 | Asymmetric rows and distributed upper blocking |
| 4 | 404 | 1 | Courtyard | 30 | Broader upper cover and more spatial observation |
| 5 | 505 | 1 | Woodland | 28 | Return to asymmetric geometry with different pairing decisions |

Four silhouettes including the original are implemented. All use two layers. Counts and cover patterns are difficulty proxies; human difficulty and monotonic strategic progression are NOT established. Level 5 deliberately reduces tile count. No timer, shuffle, undo, fake locked content or durable progression storage is added.

### Navigation, restoration and presentation

Only winning enables explicit Next Level. A completed board remains until Next, Replay Level or the existing restart action. The fifth victory says Garden Journey Complete and hides Next. Restart and replay retain the exact definition and seed. NO_MOVES retains the level, explains that no legal matching pairs remain and offers Restart.

The centered forest-green victory card uses the existing leaf artwork, ivory serif title, encouragement and accessible standard buttons with a restrained 200 ms entrance fade. Existing 150 ms match effects remain, with ghosts outside hit testing. Replacing the board clears effects, pending touch and blocked feedback before binding the next game. Drawing and hit testing use the same geometry, fitted to all initial slots including negative coordinates; removed tiles do not change the transform.

Activity state stores ID, seed, version, accepted move history and selection. Restoration first reconstructs the catalog definition, verifies seed/version and atomically replays moves in Game. Status is derived from the restored engine rather than trusted serialized input. Invalid state falls back to a fresh Level 1. Legacy bundles without identity keys migrate to fixed Level 1 where their history is valid. This is session-based progression with activity recreation support, not durable completion saving.

### Verification scope and limitations

The original nine Android test methods remain; a tenth uses coordinate taps derived from actual view geometry to complete all five levels, advance, restart each new board and check terminal navigation/effect clearing. It does not directly match through the domain engine. Runtime verification is still required for visual balance, compact screen readability, accessibility, centered panel placement, multi-level activity recreation and touch behavior on physical devices.

JVM coverage adds seed determinism/variation, invalid candidates, bounded impossible geometry, all five witnesses, transition/deadlock/final state, restoration, renderer/hit geometry across three viewport sizes and a 120-board matrix (40 seeds × three generated layouts). Every accepted matrix candidate is replayed fully. Existing host scripts are preserved.

Next iteration should prioritize measured puzzle quality: bounded branch analysis, deadlock-risk proxies, representative human playtests, gentler seed curation and phone touch-size measurements before adding more content.

### CODEX 07 executed verification

- Diagnostic production JVM suite: **89 passed, 0 failed** (67 preserved + 22 new); JUnit execution 0.394 s on this host.
- Representative matrix: 120 accepted boards, all replayed through Game; 89 ms total on this host, maximum two attempts. These are JVM host timings, not Android device measurements.
- Initial available tiles / matching choices: L1 6/3, L2 11/6, L3 10/5, L4 9/5, L5 10/6. These counts do not prove human difficulty or a monotonic challenge curve.
- Host verification script regression suite: **10 passed, 0 failed**.
- Standard Gradle request (`:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`) could not start: wrapper lock under ~/.gradle is read-only. Standard tests, assemble and lint are **NOT EXECUTED**; the wrapper invocation failed before task execution.
- ADB device discovery failed because its server cannot bind a socket (Operation not permitted). Updated Android launch, installation, connected instrumentation and visual review are **NOT EXECUTED**. Earlier user screenshots cover CODEX 06 only.
- An initial overlapping diagnostic build caused shared-output errors; that result is discarded. A later compact-scroll compile error was corrected; only the final successful serial diagnostic build is artifact evidence.
- Victory content is scrollable within the play area for compact displays; actual phone readability remains NOT VERIFIED.

### Final CODEX 07 artifact

Final serial `python3 tools/verify_offline.py` completed with exit code 0. All 89 JVM tests passed (0 failures; 0.394 s JUnit execution), all 10 instrumentation methods compiled (0 executed), and the 120-board seed matrix took 89 ms on this host with maximum two attempts.

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Size: **721,299 bytes**
- SHA-256: `87672898c9e7ca4a705755ccc2701dbd9e7a375791735709e94cd5e8e14c0964`
- Method: diagnostic offline pipeline, **not standard Gradle**.
- Alignment: PASS. Debug signing: PASS, APK signature schemes v2 and v3 verified, one signer. v1/v3.1/v4 and SourceStamp are absent; this does not establish publisher provenance.
- Android installation/launch/instrumentation/visual review: **NOT EXECUTED**.
- Full A–Q report and all 24 acceptance criteria: `CODEX_07_REPORT.md`.
- Local diagnostic evidence: `app/build/codex07/` (ignored build output).
