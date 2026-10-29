# CODEX 49 — Larger controls and readable interface

Implementation is complete; Android visual acceptance and APK delivery remain blocked by the host environment. No successful device review is claimed.

| Requested item | Implementation / verification |
| --- | --- |
| A. Gameplay controls | Shared 60dp circles (previously 56dp), equal weighted columns, 1dp side gaps, capped 368dp dock on larger devices. Insets are still applied by the existing root. Dock height follows scalable badge/ad text. |
| B. Icons | Gameplay icons increased from 24dp to 28dp. Settings uses a 28dp image area in its existing 48dp target. |
| C. Credits | Minimum 24dp rounded gold badges, 14sp numerals, dark ink, dynamic width and font height. Existing balances and reward logic untouched. Added Android measurement/drawing coverage for 0/1/3/10/99/999. |
| D. Rewarded ads | Existing localized AD/PUB resources retained; label increased to 12sp; the tiny play glyph is replaced by a text-only indicator. Long localized labels wrap at the system font size within the control column. Dedicated bottom strip separates it from the circle and credit badge. No ad behavior or placements changed. |
| E. Typography | Level 20sp, instructions 14sp, Home subtitle/help 16sp, reward loading/error status 15sp. Home level remains 26sp and can wrap rather than autoshrink to 12sp. Existing outcome heading/body are already 26/15sp and primary actions 17sp. No gameplay title or remaining-pairs view restored. |
| F. Settings | Both screens use the same background-null ImageButton, gold normally and ivory while pressed, 48dp invisible touch area, localized description and existing click behavior. No ripple surface/ring/shadow. Gear asset does not auto-mirror; positioning follows RTL. |
| G. Dialog | Sound label 17sp, Language/privacy headings 16sp, close action 16sp with 48dp minimum height. Existing native switch and floating rounded dialog rendering retained. |
| H. Language picker | Native language names 17sp, minimum 56dp rows, flexible labels, existing flags/checkmarks/scroll container and Automatic row retained. Eight translations untouched by this iteration. |
| I. Responsive checks | New Android tests exercise production controls at 320/360/400/800dp, eight locales, font scales 1/1.3/1.5/2. Tests require an Android host and have not been executed here. Existing header/interaction/regression tests retained. |
| J. Screenshots | Unavailable: ADB fails to install its smartsocket listener with Operation not permitted. Normal/large-font screenshots and full board/layout visual review remain required. No mock image is presented as an Android screenshot. |
| K. Automated checks | Final main + Android-test diagnostic Kotlin compilation PASS (diagnostic-compile-wrapped.log). Resource compilation/linking PASS within that diagnostic. Android tests compiled, not executed. JVM regression result recorded below. git diff --check PASS. |
| L. APK | All three required Gradle commands attempted; wrapper lock is read-only. Installed Gradle with a writable temporary home also fails to obtain a wildcard IP for its locking service. No fresh APK produced; timestamp, size, hash, alignment, package and certificate checks cannot be reported for this iteration. Existing APKs are stale. Signing configuration untouched; expected certificate remains fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6. |
| M. Git | Commit uses existing .tilebloom-git history because the mounted .git directory contains no usable repository metadata. Pre-existing eight locale modifications and unrelated untracked work excluded. No push or publication. |
| N. Limitations | Device UI tests, screenshots, full screen large-font/RTL acceptance, Lint, standard unit-test task and signed APK delivery need a host permitting Gradle/ADB sockets. Extreme credit counts wider than their column need device review. |

Preservation audit: no changes against HEAD in gameplay/domain, persistence/storage, ads, audio, board geometry, MahjongBoardView, TileTrayView or drawable artwork. The board still uses its original weighted area and tile geometry; the taller dock must be visually accepted on small devices before claiming unchanged on-screen tile sizing. No uninstall, player-data clearing, signing-key changes or publishing.

Evidence is in docs/codex49. Required commands were run independently as requested with --no-daemon --console=plain. The writable-cache fallback is also logged. Diagnostic Kotlin/JVM checks are separate from Gradle and do not replace Android tests or Lint.

Final JVM diagnostic regression result: **PASS — 310 tests, 290.549 seconds** (docs/codex49/jvm-tests.log). This is the full existing JVM suite run using the cached JDK 17/Kotlin toolchain, not the blocked Gradle task.
