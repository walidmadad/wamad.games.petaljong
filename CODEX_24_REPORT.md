# CODEX 24 — Bottom controls and reward dialogs

A. Bottom navigation: four centered forest-green circles with thin muted-gold outlines in a compact dark-green dock. Existing root system insets and bottom padding are retained. Pressing changes the circle surface.

B. Sizing: 56dp visual circles, 24dp ivory icons, identical 64×76dp targets. The extra target area contains attached badges without parent clipping. Numeric badges have a 20dp minimum diameter and 12sp dark numbers.

C. AD markers: zero-credit Hint/Undo have a gold capsule attached to the lower circle edge, with a 12dp play icon and 11sp AD label. Positive credits remove the marker. Main icons remain centered.

D–E. Reward dialogs: custom vertical content replaces the platform action bar. A 48dp Close target sits upper-right, followed by centered 48dp reward art, +1 Hint/Undo, a compact reserved status row and full-width gold Watch Ad button. Existing inline icon span centers icon and label together. Button height grows with text, with at least 52dp height. Loading/unavailable states use the same layout; ready omits redundant status text. No explanatory paragraphs.

F. Accessibility: existing action/credit descriptions remain; Close is described, reward art is decorative, and Watch Ad names the selected reward. Badge text scales with font size. Existing polite loading announcements remain. Large-font visual review is pending.

G. Responsive review: calculated dock width is 280dp, fitting 288/328/368dp content areas at 320/360/400dp screen widths with existing 16dp side padding. Dialog width is screen width minus 40dp, capped at 360dp. These are source measurements, not emulator verification. No board geometry changes; dock consumes more vertical space than the previous footer and compact-height review remains required.

H. Tests: 211 JVM tests pass (docs/codex24/jvm.log). Resource compilation and git diff whitespace checks pass. Android tests now use custom dialog action IDs and add equal-target dimensions, descriptions, centered Watch Ad gravity/span, Close placement, and badges 0–3. Existing deterministic cancel/loading/reward/duplicate/persistence coverage remains. These Android tests have not run. App and Android test sources also pass diagnostic Kotlin compilation against cached Android/SDK libraries with metadata-version checking bypassed for the newer ads dependency; this is not a standard Android build or executed UI test. See kotlin-diagnostic.log.

I. Emulator: blocked. ADB cannot install its server socket: Operation not permitted. No screenshots, live visual success, ad delivery, installation or device persistence claims. No uninstall or data clearing occurred.

J. APK: no fresh APK produced; path, size and SHA-256 are unavailable. Standard Gradle fails at the default read-only wrapper lock. Installed Gradle with a writable /tmp user home fails before tasks: Could not determine a usable wildcard IP. See docs/codex24/gradle*.log.

K. Signing: requested ~/.tilebloom/debug.keystore is absent. Existing app/build/offline/debug.keystore has the exact required SHA-256 fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6 (local-certificate.log). Added optional signing.local.properties loading to Gradle, ignored by Git. A mode-0600 local file points to that existing key; credentials and key are not staged. Actual signed APK/update compatibility remains unverified because build/ADB are blocked.

L. Git: committed on the existing .tilebloom-git history with message feat: polish bottom navigation and rewarded ad dialogs. No history rewrite or publication. Resolve the hash with git --git-dir=.tilebloom-git --work-tree=. log -1 --oneline.

M. Limitations: standard Android compilation, lint, instrumentation, screen-width/font-scale visual checks, APK signing and update installation require an unrestricted environment. Gameplay, reward manager, SDK callbacks, UMP, wallets, saves and progression code were not edited.
