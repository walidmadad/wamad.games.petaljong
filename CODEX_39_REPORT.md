# CODEX 39 — Centralized Settings

A. Home: retained the garden background, title, subtitle, botanical artwork and saved-level card. Removed the standalone sound control and online Home Privacy button. The required offline Privacy entry remains conditional on UMP requirements.

B. Arrow: removed the decorative arrow view entirely. The card contains only the dynamic level label and subtitle. Its existing click route, accessibility description, ripple and pressed feedback are retained; no Play button was added.

C. Settings entry: a 48 dp gear with label and tooltip “Settings”, gold tint and circular ripple sits at the inset-aware root's top/end corner. Gameplay uses the same entry in its existing header, leaving the four bottom controls unchanged.

D. Shared panel: one native AlertDialog implementation presents SETTINGS, a SOUND EFFECTS switch with ON/OFF labels, PRIVACY, Privacy Settings and CLOSE. Its body scrolls for short screens and large text. Android Back dismisses it; outside touches are consumed. Native dialog keyboard/TalkBack semantics are retained, and focus returns to the visible launching control.

E. Sound: changes call the existing GameAudioManager.preferences with the existing mute preference. The existing SharedPreferences storage and immediate effect stopping are unchanged. No duplicate audio system or music was introduced. Existing audio preference recreation tests remain in place.

F. Privacy: Privacy Settings delegates to AdvertisingService.provider.privacyOptions when required. The existing Google UMP form and its error toast remain intact. When not required, the existing explanatory toast is shown. The offline direct privacy action remains available when required. No ad provider, consent, reward or interstitial implementation was edited.

G. Gameplay: Settings opens a modal window over the existing Activity. It does not regenerate, navigate, mutate or replace the board, tray or history. Active gameplay accounting excludes time in Settings. A regression test checks the same board snapshot after Back and verifies that the window is touch-modal; the sound toggle is restored to its original value by the test.

H. Responsive design: Home retains the existing compact illustration, capped 420 dp composition, auto-sized level text and scroll layout. The gear is independent of the illustration and uses the root's existing system-bar/cutout padding. Dialog content scrolls. Actual small/large-device visual review, TalkBack and keyboard interaction could not be performed because ADB cannot start its listener in this environment. No screenshots are fabricated.

I. Automated verification: all 304 existing JVM tests passed in 180.022 seconds using cached tools and Java 17. Main Kotlin, Android instrumentation sources and Android resources compiled/linked successfully with cached tools. The instrumentation tests were compiled, not executed. git diff --check passed. See docs/codex39. All three requested standard Gradle commands were attempted and failed before tasks ran: the wrapper distribution lock is read-only. Installed Gradle with Java 17 and a temporary writable user home also fails before tasks run because no usable wildcard IP is available. Cached-tool diagnostic compilation and JVM test results are recorded separately; these do not represent Gradle or Android Lint success. The fallback runner initially treated WitnessReplayTest.kt (a helper-only file) as a JUnit class; the final runner selects actual compiled test classes.

J. Screenshots: unavailable. ADB fails with “could not install smartsocket listener: Operation not permitted”. Home, Settings and gameplay-with-Settings screenshots and actual visual review remain required on an Android host.

K. APK: no fresh APK was generated. Do not treat the pre-existing APK as this change. Signing configuration and keys were not modified. Existing artifact certificate evidence is recorded separately; required certificate is fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6. No uninstall, app-data erasure or installation was performed.

L. Git: changes committed locally using the existing .tilebloom-git history. The environment provides an empty read-only .git directory, so commands explicitly select the existing repository. Unrelated pre-existing untracked files are excluded. Nothing pushed or published.

M. Remaining limitations: standard Gradle unit tests/build/lint, a fresh signed APK, executed Android regression tests, real UMP interaction, screenshot review and device accessibility/size checks require a host with working Gradle and ADB. Diagnostic source compilation and JVM tests cannot substitute for these checks.
