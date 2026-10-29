# CODEX 48 — Minimal gameplay header and ceramic refinement

Source changes are implemented. Device verification and a fresh APK are blocked by this environment; this iteration is **not a verified Android delivery**.

| Requirement | Result |
| --- | --- |
| A–C. Header, title, pair display | Gameplay now has one 48dp row with Settings at the start and the localized level at the end. Removed the gameplay app-name TextView, the status panel and the counter TextView/update. Home retains its title. Pair-count resources remain for compatibility; gameplay does not bind them. |
| D. Layout | An 8dp gap separates the header from the original 76/84dp tray. The board receives the freed vertical space through its existing weighted container and remains centered by unchanged BoardGeometry. Tile aspect ratios, puzzle coordinates and blocking geometry are unchanged. Bottom buttons and inset handling are unchanged. RTL mirrors the header independently of the LTR board. |
| E–H. Ceramics | Cleaner ivory highlights, warm muted-ivory blocked faces, lighter ceramic side walls, a single .011-unit gold rim, and one upper-edge glint. Removed the concentric inner bevel. Reduced ordinary blocked shading from alpha 22 to 12. Lowered shadow opacity while retaining stronger contact shadows and elevated-layer separation. Existing cached gradients and Canvas paths are reused; no blur or framework introduced. |
| I. Artwork | All 23 botanical files match CODEX 47 SHA-256 values. Symbol IDs, resource mapping and artwork drawing code are unchanged. Mystery jade and emblem drawing are unchanged, including its original gold gradient; shared stacking shadows receive the refinement. |
| J. Languages | Existing level_number resources are retained for all eight languages. Added Android layout coverage for English, French, Spanish, Brazilian Portuguese, Arabic, Hindi, Japanese and German at 320/360/400/800dp. Actual execution is pending. Language picker, flags and Settings behavior are unchanged. |
| K. Screenshots | Reviewed the existing [CODEX 47 Level 37 baseline](docs/codex47/android-after.png). No new Android screenshot could be captured: ADB cannot bind its socket (Operation not permitted). No after image or successful before/after visual verification is claimed. |
| L. Tests | See verification results below. |
| M. APK | No fresh APK produced. Required Gradle tasks fail before task execution. Timestamp, size, SHA-256, package, alignment, signature and compiled-header/renderer checks for a CODEX 48 APK are therefore unavailable. Existing APKs are stale for these source changes. Signing configuration and keys are untouched; the required certificate remains fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6, but no new artifact has been signed or verified. |
| N. Git | Existing .tilebloom-git history is preserved; only iteration files are committed. Pre-existing eight locale-file edits and unrelated files are excluded. No push or publication. |
| O. Limits | Android test execution, screenshot review of the new rendering, physical-device review, fresh APK checks and Android Lint remain pending on a host that permits Gradle and ADB sockets. |

Verification:

- Preservation audit: PASS. All 23 art hashes and 50 gameplay/storage/audio/ad/tray/geometry source files are unchanged against CODEX 47. Mystery emblem and botanical renderer bodies are preserved.
- Full diagnostic JVM regression suite: **PASS, 310 tests, 211.233 seconds** ([log](docs/codex48/jvm-tests.log)). All existing tests are retained.
- Added two JVM pair-count regressions: ordinary tray matching, Undo, snapshot restoration, mystery reveal/direct/tray matches and victory without a presentation counter.
- Updated existing launch and language-switch instrumentation assertions to check internal counts and absence of the old view. Added checks for the minimal gameplay header, Home title, Settings touch target, visible tray and board/control separation.
- Added isolated production GameplayHeader layout tests for all eight locales and four portrait widths, including Arabic RTL and unclipped level text. Instrumentation is compiled only, not executed.
- Required commands attempted independently: `./gradlew --no-daemon --console=plain :app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`. Each is blocked by the read-only wrapper lock. Direct installed Gradle with a writable temporary cache also fails to create its wildcard-IP socket service. Logs are in [docs/codex48](docs/codex48).
- Final diagnostic main + all Android-test Kotlin compilation: PASS ([log](docs/codex48/diagnostic-compile-final.log)). Diagnostic Kotlin compilation uses cached dependencies and JDK 17. The initial JDK 25 compiler attempt was incompatible with cached Kotlin 1.9.24; JDK 17 resolves that toolchain issue.
- Reproduce diagnostic compilation with `PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 docs/codex48/compile_diagnostic.py`; run JVM regressions with the same PATH and `python3 docs/codex48/run_jvm.py`. These are not Gradle or Lint substitutes.

No uninstall, player-data clearing, gameplay/difficulty/generator/persistence changes, signing-key commits, advertising/audio changes or publication occurred.
