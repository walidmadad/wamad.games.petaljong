# CODEX 56 — natural medium-green mystery tiles

Production renderer updated in `MahjongBoardView.kt`. Available faces use #409A65 → #2F8B57 → #2F8B57 → #267347, with the main green spanning gradient positions 0.18–0.78. A dedicated subtle green glaze replaces the broad 40%-white glaze on mystery faces only. The bevel is #409A65 and the side wall #267347. Golden flower shading and trim use #E6C875; all existing emblem paths, rounded corners, tile sizes, ceramic thickness and cast shadows are retained.

Blocked backs use #378456 → #2B7D4F → #2B7D4F → #266E45. They retain the golden botanical emblem and omit the available glaze. The former theme-colored blocking overlay is bypassed for mystery backs, keeping the same green identity on every theme. Ordinary tile rendering is unchanged.

No gameplay, progression, branding, icon, localization, sound, advertisement or signing configuration edits were made. Pre-existing unrelated working-tree edits remain untouched. Nothing was installed, uninstalled, cleared, pushed or published.

## Verification status

- Standard Gradle tasks requested: testDebugUnitTest, lintDebug, assembleDebug and assembleDebugAndroidTest. Default cache is read-only; writable wrapper cache cannot download because sockets are denied; the installed Gradle 8.13 distribution also fails before executing tasks because it cannot determine a usable wildcard IP. Logs: `docs/codex56/gradle*.log`.
- ADB screenshot attempt failed to start its socket listener (`Operation not permitted`), recorded in `docs/codex56/adb.log`. No Android screenshot was captured and no visual approval is claimed.
- The user’s Level 45 before screenshot was not attached or found locally. The previous production source is preserved in `docs/codex56/MahjongBoardView.before.kt.txt`.
- Existing Android production-Canvas fixtures now include Level 45 and output to `files/codex56-visual`; all five theme fixtures retain available, blocked, occluded, ivory-neighbor and revealed states. Device execution remains pending.
- No fresh APK was produced. Application ID remains `com.tilebloom.game`; the required signer SHA-256 `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6` cannot be verified against a new artifact.
- Remaining build/device steps: `docs/codex56/DEVICE_VERIFICATION.md`.

Cached diagnostic compilation and JVM regression results are recorded below when complete. These do not substitute for Gradle or Android execution.

- Cached production + androidTest Kotlin diagnostic compilation: PASS, with existing deprecation warnings (`docs/codex56/compile.log`).
- Focused mystery rules/save-compatibility regressions: 19 tests passed, 25.561 seconds (`docs/codex56/mystery-tests.log`).
- `git --git-dir=.tilebloom-git diff --check`: PASS.
- Full cached JVM regression suite: **333 tests passed**, 230.55 seconds (`docs/codex56/jvm-tests.log`).
