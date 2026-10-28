# CODEX 20 — Completed-level restoration

A. Root cause: ProgressController deliberately restores an active WON board at the completion frontier so a visible victory can survive recreation. MainActivity treated that pending victory as its home preview and PLAY simply exposed the same process-owned session. No application-entry finalization distinguished it from recreation.

B–D. Persistence and finalization: capture retains the authoritative highestCompleted frontier. Victory persistence now waits for the ordered AtomicFile writer. finalizeCompletedEntry uses the same prepareNext / nextProgress / commitNext gates as manual advancement. Only an actual WON session at the frontier can advance, strictly by one with the existing Long boundary guard. It validates the deterministic next puzzle and saves its identity, empty history, completion frontier and both wallet balances as one atomic envelope before installing it. A failed save leaves the victory intact. Termination before atomic replacement retains the victory; termination after replacement restores the next unfinished board. Neither case skips a level on retry.

E. Startup: GameStore.enter distinguishes a cold process from a live process with saved GAME navigation. Fresh Activity entry finalizes a pending victory before publishing the controller to PLAY. A cold process discards stale Activity navigation and opens HOME. A live Activity recreation keeps the visible victory. A failed entry uses the existing retry UI; recreation at the home/retry screen retries finalization.

F. Next Level: the existing durable transaction still installs N+1 directly in gameplay. Retained workspace navigation changes preserve GAME through advancement and Activity replacement, handle Android Back for unfinished sessions, and keep pending victory visible. These related pre-existing changes and their tests are included in this commit so the recorded source matches the verified app.

G. Compatibility: schema remains 5; the codec and schema 1–4 adapters are unchanged. A valid existing completed Level 6 save is restored, validated and finalized to Level 7 without deleting storage. Schema-4 pending-victory migration is covered. Missing/incompatible completed histories continue using the existing frontier-based recovery rather than replaying a completed puzzle.

H–I. Unfinished PLAYING and LOST sessions never trigger finalization. Their deterministic identity, selections, tray reconstruction and history remain unchanged. New levels have empty trays/history and fresh board instances. Undo and Hint balances are copied exactly; advancement neither grants nor spends credits. No gameplay, economy, illustration or rendering code changed.

J. Automated tests: diagnostic pipeline passed all 200 JVM tests, including existing deterministic puzzle witnesses. Added transition coverage for levels 6, 20 and 100, repeat entry, failed save, death after save, exact next identity, empty next history, old-schema migration, unfinished/LOST preservation and both wallet balances. Existing navigation tests also cover direct Next Level, stale recreation, Back and double advancement rejection. Full working-tree and staged diff reviewed; diff whitespace checks pass.

K. Android: all instrumentation sources compiled. Updated saved-victory expectations and added actual coordinate completion of Level 6 followed by three isolated disk/controller relaunches. Existing tests cover unfinished restoration and victory recreation. Instrumentation was NOT executed: ADB cannot install its listening socket (Operation not permitted). No real player data was accessed, cleared or migrated on a device here.

L–M. Fresh diagnostic debug APK: app/build/outputs/apk/debug/app-debug.apk
- Package: com.tilebloom.game
- Size: 795450 bytes
- Timestamp: 2026-10-28 19:39:10.594910 UTC
- SHA-256: daa1f0fb6c84ae0ed7d0d366a18d276fa36e73d5d9c67751ea2dfb7686e0fd67
- zipalign check passed.
- apksigner verification passed, v2 and v3, one RSA-2048 Android Debug signer.
- Certificate SHA-256: fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6
Exact verification output is in docs/codex20/apk-verification.txt; build/test output is in docs/codex20/offline-build.log.

N. Git: existing .tilebloom-git history retained; commit message: fix: advance completed levels safely after app relaunch. No push, publication or history rewrite. The pre-existing untracked CODEX_18_REPORT.md remains untouched.

O. Limitations: standard Gradle verification could not start because the Gradle wrapper lock path is read-only (docs/codex20/gradle.log). The established JDK-17 diagnostic builder generated this APK; this is not a Gradle/Lint pass. Actual Activity lifecycle execution, physical interruption during AtomicFile replacement, and the player's specific Level 6 save still require host/device testing. Storage failures remain logged; victory remains visible in memory if its durable write fails. Long.MAX_VALUE retains the existing terminal-limit behavior.

Host verification (preserves saved data):
1. Run ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest on an unrestricted Android host. UI tests use test-prefixed storage, separate from player-progress-v1.
2. Install with adb install -r app/build/outputs/apk/debug/app-debug.apk. Keep the existing signing identity; if Android reports a signature mismatch, obtain the matching signed build rather than uninstalling.
3. Force-stop and launch com.tilebloom.game with an existing Level 6 victory. Confirm HOME shows LEVEL 7; PLAY opens an empty Level 7 board. Repeat force-stop/launch three times; it must remain 7.
4. Collect tiles, relaunch, press PLAY and confirm the exact tray/board resumes. Check both credit balances.
5. Complete another level, rotate/recreate the Activity while victory is visible, then press Next Level: the following gameplay must open directly. Separately close at victory and relaunch: the following HOME must wait for PLAY. Check LOST/restart never advance.
