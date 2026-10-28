# CODEX 17 — TileBloom UI fixes

## A–G. Changes

- **Executive summary:** Corrected startup, centered primary button content, portrait tray holders, and defeat recovery actions. Existing botanical art, colors, vectors, and animation implementation remain in use. No engine, generator, wallet, seed, or save-format changes.
- **Startup:** The level-start composition is installed immediately after reading the bounded persisted envelope. This includes title, subtitle, botanical illustration, saved level, and Play. Board generation/restoration remains on a worker thread. Early Play queues one request, disables the button, and shows inline preparation text on the same welcome screen. The valid board opens after preparation. Pending Play survives recreation. Load errors keep the welcome screen and offer retry through Play. Android system splash behavior remains intact.
- **Play:** A centered text run includes the 24dp vector icon and spacing; the vector is vertically centered against font metrics. Existing gold button styling and full-width alignment with the Level card remain. Next Level retains its vector when its label updates.
- **Tray:** Four identical holders share `SLOT_ASPECT = 0.72`. Both available width and height constrain dimensions. Tray height is 76dp on compact layouts and 84dp otherwise. Ivory collected tiles use original botanical symbols with uniform scaling. Slot drawing and animation use the existing authoritative tray list.
- **Defeat panel:** The centered rounded card contains the actual Undo and Restart controls, their original vector icons, and the Undo badge. It occupies only the board area below the tray. All four losing tiles remain visible. The overlay consumes touches over the board.
- **Undo/Restart:** Existing listeners and domain operations are retained. Undo checks valid history and positive credits; the wallet charges only on success. Restart retains level identity, deterministic seed, progression, and credits while clearing the attempt and visual feedback. No domain or storage implementation was modified.
- **Footer:** The same controls move between footer and defeat card; no duplicate actions are created. Playing shows the footer, lost shows card actions, won shows Next Level, and start hides gameplay. Terminal feedback outside the card is hidden.

## H–I. Layout and accessibility

Layout arithmetic reviewed for 320, 360, 400, and 480dp portrait widths. At 320dp, gameplay has 288dp usable width; the compact tray uses approximately 44×61dp holders. The card has approximately 240dp inner width and its two action targets require 160dp including margins, so stacking is unnecessary at these widths. Normal tray holders are approximately 48×67dp. Footer targets remain at least 64×56dp. System-bar insets, scrollable welcome/outcome containers, and existing large-font handling remain.

Android instrumentation adds real View layout/drawing assertions across these widths, equal portrait bounds, populated versus empty rendering, centered Play content and Level alignment, defeat-card parentage, hidden footer, and zero-credit restart. These assertions are **NOT EXECUTED** here; arithmetic review is not a substitute for device screenshots, full-layout font-scale testing, or TalkBack testing.

Accessible action descriptions and remaining-credit descriptions remain localized. The outcome title is an accessibility heading on supported Android versions. The covered board is excluded from accessibility while terminal UI is visible. Disabled Undo retains its muted icon treatment. Updated defeat copy uses Android string resources.

## J–K. Verification

Final Java 17 offline verification passed: **186 JVM tests**, including gameplay/tray matching and defeat, Undo accounting, restart, progression persistence, geometry, and generated-level solvability. Updated mutually exclusive footer policy tests passed. Production and Android instrumentation sources compiled. Instrumentation runtime: **NOT EXECUTED**. Final diff whitespace check passed.

## L. Fresh APK

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Modified: **2026-10-28 19:59:19 +0100**
- Size: **787,188 bytes**
- SHA-256: `5ae27fc85780cedbc80a4a13f3288d755d12d7b63266b3210f227236dae6ca99`
- Package: `com.tilebloom.game`; min SDK 26, target SDK 36.
- Alignment: `zipalign -c 4` passed.
- Signature: `apksigner verify --verbose --print-certs` passed; APK Signature Scheme v2 and v3, one RSA 2048-bit Android Debug signer.
- Certificate SHA-256: `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`.
- Resource inspection confirms Play/Undo/Restart vectors and revised defeat detail are packaged. No new artwork was necessary.
- The fallback rebuilt classes, resources, DEX and APK; the prior APK was not presented as the final build.

Normal Gradle was attempted and failed before compilation because the wrapper cannot create its lock file in the read-only home Gradle cache. The established Java 17 `tools/verify_offline.py` diagnostic build is used instead. This fallback does not execute Android Lint.

Android instrumentation and emulator installation/visual inspection: **NOT EXECUTED**. ADB could not start its server: `could not install *smartsocket* listener: Operation not permitted`. No runtime, screenshot, or on-device font-scale result is claimed.

## M–P. Delivery and follow-up

History is preserved through the existing `.tilebloom-git` Git directory; no repository initialization, history rewrite, publication, or push. Commit details are provided in the final response.

Recommended next iteration: install this APK on a host and review cold launch, early Play, loss recovery, 320/360/400dp portrait layouts, larger phones, increased font scale, TalkBack, and activity recreation. Complete normal Gradle/Lint and connected instrumentation on that host.

Exact host commands from the project root:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tilebloom.game/.presentation.MainActivity
./gradlew :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

The offline diagnostic build compiles instrumentation sources but does not package an instrumentation APK; the host Gradle connected task builds and runs that APK.
