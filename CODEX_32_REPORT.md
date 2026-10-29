# CODEX 32 — level preparation and botanical loading

Implemented in the existing Kotlin/Android application. Nothing published. No install, uninstall, player-data deletion, signing change, or advertising-frequency change performed.

## Investigation and measurements

The previous Next Level implementation already generated off the main thread, but owned its thread and completion callback in an Activity, started work only on tap, and used the victory button as its waiting presentation. The expensive measured phase is difficulty analysis, followed by tray replay. Novelty checking repeatedly reconstructed fingerprints, and availability checks allocated filtered lists at every solver node. This is not evidence that generation alone explains every device wait.

Raw reproducible host-JVM measurements: `docs/codex32/baseline-phases-jvm.txt`, `final-phases-jvm.txt`; initial summary: `docs/codex32/performance.md`; final sequential fixed-heap summary: `docs/codex32/isolated-performance.md`. Run with JDK 17: `python3 tools/benchmark32.py`; summarize with `python3 tools/summarize32.py`. Three fresh generations per target: 8, 16, 30, 31, 51, 101, 501, 1001. These are desktop JVM measurements, not Android timings. Cold/JIT effects and concurrent validation workloads affect them; three samples are insufficient for a stable population p95. No two-second Android target is claimed.

Instrumentation separates configuration, recent geometry, novelty selection, witness construction, full verification, difficulty analysis, tray replay and total generation. Android logs additionally record durable-save and board/presentation submission durations. Witness construction currently includes the legacy symbol construction; assignment, final selection, and first rendered frame do not yet have independently measured device spans. Baseline phases are from the original algorithm with instrumentation only. Attempt counts describe the selected candidate's geometry attempts; candidate count and aggregate analyzer node count are reported separately. `attempts=0` denotes a selected deterministic peel fallback, not every fallback attempted internally.

## Optimizations

- Availability scans the same authoritative overlap/side relationships without intermediate lists. An exhaustive subset comparison tests equivalence against the previous implementation.
- Solver move enumeration preserves order and node budgets while avoiding repeated `Game` construction and board validation at every node.
- Novelty geometry and fingerprints are computed once per generation and reused in bounded local collections. No cross-version/global geometry cache.
- Six candidates, scoring, seeds, version 8, all eight families, tiers, symbol assignment, full witness replay and final validation are retained. No quality requirement is relaxed. Analyzer UNKNOWN outcomes still mean unknown alternative difficulty, not an unverified accepted witness.

## Loading and lifecycle

`LevelPreparation` owns a single in-flight/reusable result keyed by target within the fixed generator implementation. Fake executor and clock tests exercise duplicate requests, retry, stale results and the 250 ms threshold. It has no Android dependency. `GameStore` owns one executor, durable-save sequencing, identity checks and main-thread publication; it retains no Activity. Generation starts after a confirmed durable player victory. Pre-generation never advances the frontier. Next Level authorizes exactly one +1 transaction. Failed generation/save leaves the won level intact. Successful preparation is reusable after a save failure. Existing atomic storage records the completed frontier and credits; process death can reconstruct the pending victory/next identity without old move history being applied to a new puzzle.

Generation remains bounded by existing candidate/attempt/node budgets and adds a cooperative 30-second preparation deadline checked between phases and periodically in search. Exhaustion produces Retry, never an unchecked board. This deadline is not a hard disk-I/O timeout. A process-owned accepted task may finish while backgrounded; animation stops on pause/destruction. This avoids wasting the result or attaching completion to an obsolete Activity.

`LevelLoadingScreen` overlays gameplay directly, using the upcoming theme, serif TileBloom title, existing original botanical tile composition, progress indicator and actual upcoming level. Artwork gently rises/fades in a 1.2-second easing cycle using property updates. Disabled system animations show static artwork/text. No animation minimum duration. Already-ready results skip loading. Errors show “Garden not ready.” and Retry. Root system-bar insets and scrollable centered content accommodate narrow screens. Victory/ad eligibility sequencing and active-gameplay exclusion during transitions are preserved; loading never triggers ads.

## Verification and limitations

See raw logs in `docs/codex32`. Standard wrapper unit/build/lint commands were attempted. Default Gradle home is read-only; a writable home/direct cached distribution also fails because sandbox networking cannot provide a wildcard IP for Gradle's lock service. These failures cannot establish a build or Lint pass. Diagnostic Kotlin compilation with cached Android/AdMob/UMP dependencies checks source types only; it is not a standard Gradle APK build.

No fresh APK can be claimed. The pre-existing `app/build/outputs/apk/debug/app-debug.apk` predates this work and does not contain CODEX 32. Timestamp/size/signature/alignment/hash of that old artifact would not verify this change. Expected certificate remains `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`; compatibility of a fresh build is NOT VERIFIED. Signing configuration/keys untouched; no keys committed.

Android instrumentation sources include a new 320/360/400/720 dp loading-view fixture, but device execution, controlled transition/failure/recreation scenarios, animation responsiveness and reduced-motion runtime checks remain pending. The emulator is connected; the missing fresh standard APK prevents honest runtime claims. All five requested screenshots are pending. AdMob source preservation does not substitute for device/ad-SDK validation.

The existing history is in `.tilebloom-git` (the sandbox exposes an empty read-only `.git`). Use `git --git-dir=.tilebloom-git --work-tree=.`. No reinitialization, rewrite, push or publication.

## Acceptance status

| # | Criterion | Status |
|---|---|---|
|1|Bottlenecks investigated|PASS (host phases; full device path pending)|
|2|Baseline timings recorded|PASS (JVM)|
|3|Generator optimization implemented|PASS|
|4|Expensive generation off main thread|PASS (source; device responsiveness pending)|
|5|Dedicated loading screen|PASS|
|6|Garden identity|PASS (implementation; visual review pending)|
|7|Botanical animation|PASS (implementation)|
|8|Correct upcoming level|PASS (dynamic target)|
|9|Fast transitions skip screen|PASS (policy test; device pending)|
|10|No artificial minimum delay|PASS|
|11|Safe background preparation|PASS (implementation; lifecycle runtime pending)|
|12|Duplicate preparation prevented|PASS (unit test)|
|13|Advancement exactly +1|PASS (guards/tests)|
|14|Recoverable failures|PASS (implementation/tests; Android pending)|
|15|No indefinite generation without recovery|PASS (cooperative production budget; hard I/O stalls excluded)|
|16|Completed progress survives termination|NOT VERIFIED on device; existing durable protocol preserved|
|17|Undo unchanged|PASS (source/tests)|
|18|Hint unchanged|PASS (source/tests)|
|19|AdMob intact|NOT VERIFIED at runtime; ad implementation unchanged|
|20|Difficulty unchanged|PASS (policy unchanged)|
|21|Layout variety unchanged|PASS (families/scheduling unchanged)|
|22|Accepted boards solvable|PASS (full witness validation retained)|
|23|Existing JVM suite|PASS — clean host run, 252 tests|
|24|New transition tests|PASS — 4 targeted tests, also included in full suite|
|25|Android presentation tested|NOT VERIFIED (source fixture only)|
|26|Before/after benchmarks|PASS (JVM only)|
|27|Fresh standard APK verified|FAIL (environment blocks Gradle)|
|28|Signing compatibility|NOT VERIFIED (configuration untouched)|
|29|Git history preserved|PASS|
|30|Nothing published|PASS|

## Final source/test evidence

Clean full JVM regression run: **252 tests passed**, 255.162 seconds (`jvm-final-tests.txt`). Targeted preparation run: **4 tests passed** (`transition-tests.txt`). Final diagnostic Android main + instrumentation source compilation: **PASS** (`android-delivery-compile.txt`). Neither is an executed Android instrumentation run. The earlier full-suite run (`jvm-tests.txt`) is invalid: launching another verifier deleted its class output while tests were running, causing 68 class-loading failures. The clean rerun supersedes it.

Initial measured matrix: median 592.1 → 556.8 ms; observed p95 1512.6 → 2256.7 ms; maximum 1543.4 → 2731.4 ms. These runs overlapped validation workloads; they do not prove consistent speedup. Follow-up runs in `followup-performance.md` also remain affected by concurrent compilation/validation. Selected fallback frequency is 9/24 both before and after (37.5%); all sampled generations evaluate six candidates. Candidate attempt/node/family records are in raw logs. A selected peel witness still receives full varied botanical symbol assignment and validation; fallback does not imply a single-symbol accepted puzzle.

## Final sequential fixed-heap comparison

After compilation and the regression workload completed, ran `python3 tools/run32_matrix.py`: committed original then optimized code, separate JVM processes, identical JDK 17 and `-Xms256m -Xmx512m`, three generations per target. Summary: `docs/codex32/isolated-performance.md`; raw records: `isolated-original-jvm.txt`, `isolated-optimized-jvm.txt`. Aggregate median **276.7 → 166.0 ms**, observed p95 **1025.0 → 503.4 ms**, maximum **1066.5 → 1417.7 ms**. Level 30 median **638.5 → 324.5 ms**. Maximum regression is explicitly retained; no claim that every generation improved. These limited host samples establish less typical host CPU work, not Android latency, device percentiles, transition-save latency, or satisfaction of the two-second device target. Final selected fallback frequency remains **9/24 (37.5%)**; six candidates each. All 24 initial matrix metadata records match before/after (`candidate-equivalence.txt`).
