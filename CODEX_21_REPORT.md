# CODEX 21 — High visibility Hint

A. Root cause: the previous low-contrast gold stroke resembled the ceramic rim, scaled with tile size, lasted only two seconds, and was painted during individual tile rendering where subsequent neighbors could obscure it. Its animation also requested frames with system animations disabled.

B–C. Rendering: one authoritative target receives a 3.5 dp warm gold rounded outline over a 6 dp dark contrast stroke. The foreground pass follows all ordinary tiles and match ghosts. The botanical artwork is not repainted, enlarged or exposed. Other tiles retain their normal appearance.

D. Animation: a cosine easing cycle gently changes gold brightness every 1.4 seconds for a total of 4000 ms. The outline remains opaque at both endpoints. Expiration clears transient state and invalidates once. No credit operation runs in frames or expiration.

E. Indicator: an original Canvas lightbulb badge with globe, neck and base connects to the same face corner. Placement evaluates four corner candidates against botanical drawing envelopes, prefers above the tile, and clamps the badge within the board view. Placement is cached during the presentation. No emoji, library or image asset is used.

F. Layering and targeting: HintPresentation keeps exactly one engine-returned ID, validates PLAYING status and legal availability, and resolves its face through BoardGeometry. The foreground pass prevents neighboring tile shadows covering the primary border. Only strokes and an indicator are drawn: no hidden symbols, selection, availability, hit bounds or draw order for actual tiles change.

G. Economy: existing asynchronous MainActivity guard rejects pending and already-visible Hint requests. ProgressController.acceptHint remains the sole persistent debit and existing failure Toast remains in use. Three initial Hint credits, Undo wallet, restart, level advancement, tray and save schema are unchanged. The new presentation object cannot access either wallet.

H. Accessibility: thick contrasting border, recognizable bulb shape and gentle motion provide redundant cues beyond color alone. ValueAnimator.areAnimatorsEnabled() gives a bright static treatment when animations are disabled. Static presentation still expires after four seconds without requesting animation frames. Existing localized Hint accessibility description and remaining-credit badge remain intact.

I. Performance and cleanup: uses the existing Canvas and Paint, a reusable RectF, and a cached indicator position. No large per-frame allocations or animation library. No frame requests after expiration, or for static Hint feedback alone. Any tile tap clears feedback before normal processing, including blocked tiles; existing restart, Home/Back, load, size change and detach cleanup cancels the callback. Refresh/draw also invalidates terminal or no-longer-available targets. Recreation starts with fresh ephemeral state. Existing pending-request tokens protect against obsolete asynchronous results.

J. Regression coverage: five new JVM tests exercise authoritative ID, single target, legal availability, geometry, compact board indicator bounds, unchanged hit testing, duplicate rejection, expiration, wallet independence, covered/removed targets, victory/defeat invalidation, static brightness and fresh-state behavior. Existing Hint/Undo/persistence/generated-puzzle tests remain included. Final diagnostic suite: OK (205 tests), including all existing regressions and five new presentation tests.

K. Android instrumentation: extended existing real Hint interaction test with engine-ID equality. Added expiration, balance preservation, restart, level replacement and Home cleanup checks. Existing Activity recreation coverage remains. Sources compile in the diagnostic pipeline; execution is pending.

L. Actual Android screenshot verification: PENDING. ADB cannot create its listening socket in this sandbox (Operation not permitted). No updated APK installation, emulator screenshot, compact-screen visual inspection, device reduced-motion test or runtime instrumentation execution is claimed. Placement around dense layered boards needs visual review on an unrestricted host.

M. Fresh diagnostic APK: app/build/outputs/apk/debug/app-debug.apk; package com.tilebloom.game; 795450 bytes; modified 2026-10-28T20:29:14.731461 UTC. SHA-256: 564ca9d718b95284d791c57384c59d914af3a9914ae61dcb343d5699f3042a74. zipalign passed; v2/v3 signature passed; RSA-2048 Android Debug certificate SHA-256: fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6. APK DEX contains HintPresentation, drawHint, hintIndicator and brightness. Full verification: docs/codex21/apk-verification.txt. Built with JDK 17 and tools/verify_offline.py. Standard Gradle was attempted but could not acquire the read-only wrapper-cache lock; therefore Gradle/Lint verification remains pending. Build log: docs/codex21/offline-build.log.

N. Git: meaningful changes committed using the existing .tilebloom-git history with message `fix: improve Mahjong hint visibility and accessibility`. No history rewrite, push or publication. Pre-existing untracked CODEX_18_REPORT.md is untouched.

O. Remaining limitations: actual player visibility and icon placement require device screenshots; source compilation is not visual proof. Corner placement minimizes potential symbol overlap, but dense layouts require the requested visual inspection. Automated JVM tests cover presentation contracts; Activity lifecycle assertions require instrumentation execution. The APK uses the existing local Android Debug signing identity.

Host follow-up: run standard Gradle JVM/Lint/connected tests; install the APK with `adb install -r` preserving player data. On Level 7 and a compact portrait device, press Hint repeatedly and capture the active effect, confirm the pointer and botanical symbol, wait four seconds, check one-credit debit, then exercise blocked/available taps, Home, restart, advancement, recreation, terminal states and system animations disabled.
