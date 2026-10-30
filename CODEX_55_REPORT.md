# CODEX 55 — Happy Garden Mystery

Implementation is ready for device review. This iteration is **not visually approved or APK verified**: Gradle and ADB are blocked by the current sandbox.

## Artwork

The old dark jade back (`#50967B → #185C48 → #103D32`) had a small engraved laurel and central seed. The new ceramic uses spring highlight `#62D99B`, emerald `#28B879`, and deep green `#168A62`, with warm gold `#E7C66F` and ivory `#FFF3D5` accents. A new balanced emblem replaces the entire seal: six broad shaded golden petals around a raised ivory-gold center, above two curved ivory-to-pale-green leaves with green veins. Cached vector paths and gradients provide original illustrated artwork without raster assets.

The fine gold perimeter, translucent inner highlight, upper glint, pale green bevel and green side wall retain physical ceramic depth. Ordinary ivory tile drawing and botanical illustrations are unchanged. Available backs receive the bright green gradient and soft glaze; blocked backs use a separate muted green gradient (`#75B997 → #489776 → #337B60`) without glaze, plus existing blocking shade. Both retain the same flower. Existing depth order physically occludes lower tile art.

Green identity is fixed across spring/forest, sunrise/gold, lotus/turquoise, twilight/purple and autumn palettes. Pale rims and gold accents are designed to separate green tiles from backgrounds. Actual contrast and blocked-state readability remain pending Android screenshots.

## Preserved behavior

Only the board renderer and Android visual fixtures changed. No domain rules, symbol assignments, generator/version, geometry, hitboxes, persistence, progression, ordinary artwork, localization, branding, music, UMP or ad configuration changed. First reveal, second collection, switching, direct matches, mismatch behavior, Hint and Undo retain their existing production paths. No installation, uninstallation or player-data clearing occurred. Nothing was published or pushed.

## Evidence and limitations

- Pre-change renderer: `docs/codex55/MahjongBoardView.before.kt.txt`.
- New Android fixtures cover five themes, lateral/top blocking, physical overlap, ivory neighbors and revelation. Existing concealed-symbol pixel equality and interaction tests are retained. Fixtures are not executed here.
- Standard Gradle requested tasks: assembleDebug, testDebugUnitTest, lintDebug, assembleDebugAndroidTest. Default wrapper fails on read-only cache lock; temporary home cannot create a network socket; installed Gradle fails obtaining a usable wildcard IP. See `docs/codex55/gradle*.log`.
- ADB server cannot create its socket listener (`Operation not permitted`). No before/after images or actual gameplay screenshots were obtained. No claim of phone-size visual review or performance measurement is made.
- No fresh APK exists for this iteration; any existing APK predates this work. Application ID remains `com.tilebloom.game`. The required signing fingerprint is unchanged as a requirement but cannot be verified against a new artifact.
- Host/device completion steps: `docs/codex55/DEVICE_VERIFICATION.md`.

Automated diagnostic results and commit are recorded below after completion. Diagnostics do not substitute for Gradle, Lint or Android execution.

- Final production + androidTest Kotlin diagnostic compilation: PASS (`docs/codex55/compile-final.log`), with existing deprecation warnings. This includes the new five-theme fixture.
- Focused mystery regression/save compatibility suite: **19 tests passed** (`docs/codex55/mystery-tests.log`).
- `git diff --check`: PASS.
- Full cached JVM regression suite: **333 tests passed**, 233.073 seconds (`docs/codex55/jvm-tests.log`). All 39 test source files compiled; the existing WitnessReplayTest helper object is excluded from JUnit class discovery by the established runner.
- Commit message: `style: redesign mystery tiles with cheerful spring green artwork`. Only this iteration's renderer, fixtures and evidence are included; unrelated pre-existing changes remain untouched.
