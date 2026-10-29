# CODEX 50 — Stable Home and shared continuation card

Implementation is complete. Android visual acceptance and fresh APK delivery remain blocked by the restricted host.

| Item | Result |
| --- | --- |
| A. Root cause | `applyGameplayTheme` changed the shared root gradient/system bars, but returning to Home never reset them. Home/card construction also read mutable `GardenStyle.theme`. |
| B. Permanent Home | Immutable `HomeTheme.palette` uses forest #173F35 through jade #38745B. Home restores its gradient and system bars on every return. Subtitle and card tokens are independent of gameplay. |
| C. Gameplay | `GardenTheme.forLevel`, procedural generators, board/tray themes and saved puzzle identities unchanged. Returning to the same gameplay level restores its colors even when the theme cache matches. |
| D. Shared component | `ContinueLevelCard` owns both labels, typography, spacing, fixed green surface/gold border, radius, ripple, pressed scale, single accessibility target and localized description. |
| E. Victory | Old Button/icon rendering removed. Existing `next_level` view ID retained for navigation tests; it now identifies a unified card. Gameplay tint traversal skips the shared card. Existing outcome panel/title/detail/audio retained. |
| F. Navigation | Card uses `progress.highestUnlocked` after existing victory persistence; completed 37 displays 38. Tap calls existing `advance`, guarded by connectivity, ads, transition and `canAdvance`. Existing loading/retry path retained. |
| G. Persistence | No storage/domain behavior or format changes. Durable victory recording and candidate-before-commit transitions retained. Existing completion-entry tests cover save failure, relaunch and exactly-once progression; level 37 added to that coverage. |
| H. Languages | Existing `start_level`, `continue_garden`, `continue_level_description` resources reused across eight locales. No translation edits in this commit. Native resource number formatting and layout direction retained; board geometry untouched. |
| I. Screenshots | No before screenshots were attached in this session. Required actual Home 37/38/39, victory 37 and gameplay 38 screenshots unavailable: ADB cannot start its socket listener (Operation not permitted). No visual acceptance claimed. |
| J. Tests | New JVM palette independence/variety regression and Android shared-card locale/width/click/accessibility checks. Existing device next-action casts updated for the new component, double-tap test now checks the next label. Cached diagnostic results below. |
| K. Android | Required Gradle unit-test, assemble and lint commands attempted independently. All fail opening the read-only wrapper lock. Writable-cache installed-Gradle attempt fails determining a usable wildcard IP. Instrumentation cannot run without ADB. |
| L. APK | No fresh APK produced. Timestamp/size/SHA-256/package/alignment/signature/updated APK UI cannot be verified. Signing configuration untouched; expected certificate fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6 remains required. |
| M. Git | Changes committed using existing `.tilebloom-git` history; mounted `.git` contains no repository metadata. Pre-existing locale changes excluded. No push or publication. |
| N. Limitations | Device screenshot review at 320/360/400dp and larger portrait, RTL/font/inset acceptance, executed Android tests, standard Gradle checks and fresh signed APK remain outstanding. |

Evidence: `docs/codex50/`. No uninstall, player-data clearing, key changes, or changes to ads/audio/artwork/gameplay/persistence.

Diagnostic main + all Android test sources: PASS using cached Kotlin/JDK 17 (docs/codex50/diagnostic-compile-final.log). Resources compiled and linked. This does not execute instrumentation or produce an installable APK.

Final cached JVM diagnostic: **PASS — 311 tests, 173.145 seconds**. Runner `tools/test37.py` compiles all JVM tests and executes its normal suite, excluding `WitnessReplayTest` as defined by that existing runner; no claim of executing that excluded test. Includes the new Home palette test and expanded level-37 completion/relaunch case. Standard Gradle tasks remain blocked. `git diff --check` passed.
