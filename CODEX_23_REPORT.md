# CODEX 23 — Rewarded ad discoverability

A. UI changes: retained HOME | HINT | UNDO | RESTART, existing gold-bordered ripple surfaces and 56dp footer targets. No board, tray, rules or puzzle-generation changes.

B. Ad indicators: zero-credit Hint and Undo draw their action vector above a small gold play vector and resource-backed AD label, alongside the zero badge. Positive balances omit the advertising indicator. The indicator is part of the same clickable control. Reduced zero-badge footprint avoids crowding the action icon at normal font scale; emulator and large-font review remain pending.

C. Reward dialogs: custom forest-green panel with a 48dp Hint/Undo vector, warm-ivory +1 Hint/+1 Undo, gold play + Watch Ad button and vector close button. Loading shows a small spinner and Loading ad…; ready shows Ad ready; unavailable shows Ad unavailable and Retry. Retry requests loading through the existing manager, respecting its backoff and consent gates. Loading becoming ready never queues playback. Only a subsequent explicit Watch Ad click presents an ad. The native AlertDialog window/button machinery is retained with custom content and themed controls.

D. Accessibility: resource-backed zero-credit descriptions explicitly name the action, zero balance, advertisement and one-credit reward. Watch Ad/Retry and close have accessible labels. Availability changes use a polite live region, refreshed only on state changes. AD text and play shape supplement color. Positive descriptions remain unchanged.

E. Credits/rewards: SDK-earned callback, duplicate protection, durable independent wallets, initial three credits, persistence schema, UMP, Google debug test identifiers and dependencies remain unchanged. The only manager addition exposes its existing load method through a retry hook. Loading, retry, dismissal and failure cannot grant credits. Existing GameStore refresh updates the badge and removes AD immediately after the confirmed reward.

F. Tests: all 211 existing JVM tests pass using Java 17 and tools/verify_offline.py --jvm-only, including domain reward/persistence/gameplay regressions. Android resource compilation via aapt2 passes; git diff --check passes. Extended RewardInteractionTest covers positive/zero AD indicator states for both controls, clickable zeros, reward labels, accessibility descriptions, explicit confirmation, cancellation, loading without automatic playback, failure/retry without credits, exact one/duplicate callback behavior, immediate indicator removal and stored balances/recreation. Android tests have NOT compiled or executed here. Recreation is not proof of process-death/relaunch behavior.

G. Android verification: BLOCKED. ADB cannot create its local server socket (Operation not permitted). No emulator screenshots, live visual review, real Google test playback, reward delivery or install verification is claimed. No app data was cleared and no app was uninstalled.

H. APK: NO fresh APK produced. Standard requested assembleDebug/testDebugUnitTest/lintDebug/assembleDebugAndroidTest invocation fails at the read-only default Gradle wrapper lock. A Java-17 attempt with writable /tmp Gradle home and installed Gradle 8.13 fails before tasks: Could not determine a usable wildcard IP. No unrestricted host is accessible through this session. No signing keys/configuration were changed; installed-certificate compatibility is unverified. Existing APKs must not be treated as this iteration's artifact. Use tools/host_verify.sh on the unrestricted host, then connectedDebugAndroidTest and the real-ad checklist in ADMOB_VERIFICATION.md. Install only after comparing certificates; retain application data.

I. Git: changes committed on the existing .tilebloom-git history with message feat: improve rewarded ad discoverability and icon UI. Exact commit is available through git --git-dir=.tilebloom-git --work-tree=. log -1 --oneline. Existing untracked CODEX_18_REPORT.md excluded. No reinitialization, push or publication.

J. Remaining limitations: full Android Kotlin compilation, Lint, instrumentation, screenshots at normal/large font sizes, actual relaunch persistence and real test-ad flows require an unrestricted host/device. This is a source implementation with passing domain/resource checks, not a verified Android delivery.

Evidence: docs/codex23/jvm.log, gradle.log, gradle-writable.log and adb.log.
