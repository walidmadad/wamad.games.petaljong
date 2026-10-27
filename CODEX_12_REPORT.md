# CODEX 12 — Three-credit Undo and test rewarded ads

## A. Executive summary

Implemented the credit economy, schema migration, optional rewarded-ad UI, Google Mobile Ads integration source, and UMP integration source. Independent logic is verified; Android SDK compilation and runtime are **NOT VERIFIED**. No new APK is claimed. Nothing was published or pushed.

## B. Three-credit economy

`UndoCreditWallet` starts at three. This is a global, one-time local allocation, not a per-level allowance. Explicit zero remains zero. Restart, replay, advancement and restoration retain the wallet. There is no daily or lifetime ad quota.

## C. Undo credit consumption

`ProgressController.undo()` gates the existing validated engine Undo through the wallet. Failed restoration and empty history cost nothing. A successful Undo deducts one. The UI immediately captures board history and wallet together; the existing ordered AtomicFile writer commits a complete snapshot. A crash during disk replacement leaves either the old complete transaction or the new complete transaction. Disk failures retain the existing repository error reporting; durability cannot be promised when the filesystem fails.

## D. AdMob integration

Dependencies: `com.google.android.gms:play-services-ads:25.5.0` and `com.google.android.ump:user-messaging-platform:4.0.0`. Existing min SDK 26, compile SDK 36 and Java 17 targets were preserved. AndroidX dependency support is enabled in gradle.properties. Google documents Ads prerequisites as minimum API 24 and compile API 35. No Firebase, mediation, banners, interstitials or app-open ads were added.

Only Google's test IDs are configured:

- Manifest placeholder App ID: `ca-app-pub-3940256099942544~3347511713`.
- BuildConfig rewarded unit: `ca-app-pub-3940256099942544/5224354917`.

`RewardedAdManager` uses application context for loading and accepts an Activity only for consent forms and explicit presentation. It consumes each loaded ad object once, blocks concurrent presentations, discards expired inventory, and reloads after dismissal. Load/display errors use a 30-second cooldown; subsequent requests require a user retry or normal permission refresh, with no tight retry loop.

## E. Rewarded ad callback handling

Only the listener passed to `RewardedAd.show` (the official `OnUserEarnedRewardListener`) calls the single-use reward ticket. SDK reward amount is deliberately ignored: each ticket adds exactly one credit. A duplicate earned event cannot add another credit. Dismissal never grants or revokes a reward. Credit is not automatically spent.

The process-owned GameStore keeps current board and wallet together across Activity replacement. The callback captures the ticket and application state, not an Activity or its old controller. It submits the current snapshot and drains the ordered writer promptly. Activity UI callbacks detach on destruction. Local client callbacks are the trust boundary; server-side verification is **not implemented**.

## F. Ad failure and cancellation

Load failure, unavailable inventory, display failure and cancellation grant nothing. Loading does not freeze the board. Unavailable feedback is resource-backed and Restart remains available. Failure never restarts or regenerates the puzzle. The actual SDK failure behavior needs Android verification.

## G. Consent and privacy

At activity launch, request updated UMP information, then invoke `loadAndShowConsentFormIfRequired`. Both initialization and ad loading check `canRequestAds()`. An initialization guard prevents duplicate initialization from consent callbacks. Privacy options appear only when UMP reports REQUIRED and invoke `showPrivacyOptionsForm`. No fabricated consent or privacy policy is supplied. Account-side messages and real consent behavior remain unverified with the sample application ID.

## H. Persistent save integration

Credits exist in one persistent location: the existing player-progress file. Its filename remains `player-progress-v1` deliberately so existing installations are found; the payload schema is now 2. Ordered writes and repository owner tokens remain. GameStore owns the current process controller so an old screen cannot submit an old wallet. Instrumentation storage switching resets this process controller for test isolation.

## I. Save migration

Schema 1 migrates to schema 2 with three credits, preserving completed levels, active level/seed/generator version and accepted move history. A schema-2 zero is initialized data. Schema-2 invalid or missing balances are rejected by the codec; repository recovery conservatively substitutes zero while retaining parseable progression and board data. Unsupported/corrupted envelopes retain the previous fresh-progress recovery policy (three credits); this is recovery from unreadable data, not a normal refill mechanism. Existing invalid-board recovery still validates through the production engine.

## J. User interface

Zen Garden controls show `Undo (N)` or `Undo Last Move (N)`. With zero credits and undoable history, Undo is disabled and `Watch Ad +1 Undo` appears. Loading is visible, unavailable ads explain themselves, earned credits produce a short confirmation, and Restart stays free. New player-facing text uses string resources. Live region feedback and existing touch-target styling are preserved. Visual fit, accessibility and animation behavior require runtime review.

## K. Deadlock recovery

The same credit-aware action operates in NO_MOVES. Positive credits allow Undo; zero offers the voluntary ad action. Restart remains accessible. No automatic ad, credit, restart or puzzle regeneration is introduced.

## L. Automated test results

**PASS: 159 JVM tests**, including the previous 149 regressions and 10 new tests containing multiple state invariants. One old byte-envelope assertion was updated to expect schema 2 plus its credit field; its level identity and accepted-history assertions remain. Wallet tests cover 3→2→1→0, rejection, failed Undo, exactly-once tickets, independent voluntary presentations, navigation, migration and repeated reloads. Deterministic fake-provider tests cover load/display failure, unearned dismissal, duplicate rewards/taps and replacement-screen continuity. These tests do not verify the Google SDK adapter.

**PASS: 10 host-workflow tests.** Logs: `docs/codex12/jvm.log` and `docs/codex12/tilebloom12-host.log`.

Run with JDK 17:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/verify_offline.py --jvm-only
python3 -m unittest discover -s tools -p 'test_*.py'
```

## M. Android instrumentation

The original 18 methods are retained (Undo labels intentionally updated). Eight added methods cover credit consumption/reward, cancellation and repeated taps, rewarded deadlock recovery, zero-credit relaunch, recreation during loading/after reward, stale-owner writes and invalid-balance recovery. FakeGameAds lives only in androidTest. Debug-only provider injection is ignored by release builds; the default provider is always the actual Google adapter.

**26 methods present; none compiled or executed in this iteration.** Consent UI and real SDK lifecycle require device tests. Do not carry forward the previous iteration's compilation result as evidence for these changed sources.

## N. Real test-ad verification

**NOT VERIFIED.** ADB failed to start its server: socket listener operation not permitted. No emulator observation, actual Google test-ad playback, consent form observation or reward-delivery observation is claimed.

## O. APK output

**No new ads-enabled APK generated.** Standard wrapper attempt failed on a read-only Gradle distribution lock. A second attempt invoked the installed Gradle 8.13 distribution with writable `/tmp` Gradle home; it failed creating the lock contention service because no usable wildcard IP was available. See both Gradle logs in `docs/codex12/`.

The standalone diagnostic tool now explicitly refuses APK construction: it cannot package the transitive Google SDK AAR dependencies. `--jvm-only` remains supported. Existing APKs, if present, belong to earlier builds and must not be treated as CODEX 12 artifacts. No fake SDK classes were introduced.

## P. Git status

Continued from `2c8630d` using `.tilebloom-git`. No history rewrite, repository initialization, remote push or publishing occurred. The implementation commit is recorded in the final response; its message explicitly identifies Android verification as pending.

## Q. Acceptance criteria

PASS for independent behavior means JVM evidence; SDK/runtime-dependent behavior is NOT VERIFIED even where source is implemented.

| # | Criterion | Result |
|---|---|---|
| 1 | Existing engine preserved | PASS |
| 2 | Five levels preserved | PASS |
| 3 | Solution witnesses preserved | PASS |
| 4 | Three credits allocated once | PASS |
| 5 | Successful Undo costs one | PASS |
| 6 | Zero prevents Undo | PASS |
| 7 | Invalid Undo costs nothing | PASS |
| 8 | Real rewarded ad adds one | NOT VERIFIED |
| 9 | Official SDK reward event required | NOT VERIFIED |
| 10 | Duplicate rewards prevented | PASS (ticket/fake tests; SDK runtime unverified) |
| 11 | Dismissal without reward grants nothing | PASS (fake tests; SDK runtime unverified) |
| 12 | SDK load failure preserves state | NOT VERIFIED |
| 13 | SDK display failure preserves state | NOT VERIFIED |
| 14 | Advertising entirely optional | NOT VERIFIED (source implemented) |
| 15 | Free Restart with no ads | NOT VERIFIED (UI source implemented) |
| 16 | Credits persist across relaunch | PASS (codec/controller); Android NOT VERIFIED |
| 17 | Global credits across levels | PASS |
| 18 | Restart does not refill | PASS |
| 19 | Completion records remain valid | PASS |
| 20 | Previous valid saves migrate | PASS |
| 21 | Deadlock uses wallet | PASS (controller); Android NOT VERIFIED |
| 22 | Undo restoration remains correct | PASS |
| 23 | Match animations intact | PASS (regressions); Android visuals NOT VERIFIED |
| 24 | SDK integration uses test IDs | NOT VERIFIED (configured; no APK) |
| 25 | Consent requirements | NOT VERIFIED |
| 26 | Required privacy options | NOT VERIFIED |
| 27 | Fake providers restricted to tests | PASS (source placement and release guard) |
| 28 | Credit economy tests | PASS |
| 29 | Existing JVM regressions | PASS |
| 30 | Instrumentation accurately reported | PASS (not compiled/executed) |
| 31 | New ads-enabled APK | NOT VERIFIED (build blocked) |
| 32 | No stale APK passed off as new | PASS |
| 33 | Git history retained | PASS |
| 34 | Nothing published | PASS |

## R. Known limitations

SDK dependencies and Android sources have not passed a standard Gradle compilation in this environment. The fake adapter exercises the wallet/presentation contract; it cannot establish real SDK correctness. Android callback ordering, process termination during a reward, initialization failure behavior, UMP configuration, offline retries and visual layout remain host/device verification tasks. Reward persistence waits for a small disk write on the main thread, consistent with the existing departure drain; measure latency on devices. Corrupt-envelope recovery is local-only and cannot prevent save tampering. No server-side verification exists.

## S. Production monetization readiness

**Not ready for public release.** Before monetization: configure an AdMob account, replace the manifest placeholder and rewarded BuildConfig test ID through reviewed release-only configuration, configure consent messages, supply a real privacy policy, complete applicable Play data-safety/advertising disclosures and policy review, test privacy options and consent choices, verify real Android devices, and design server-side verification infrastructure if stronger fraud protection is required. Do not activate production identifiers as part of host verification. No privacy policy URL is invented here.

Official references checked during implementation:

- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/rewarded
- https://developers.google.com/admob/android/privacy
- https://developers.google.com/admob/android/migration

## T. CODEX 13 recommendation

First run the standard Gradle build, all 26 instrumentation methods, and one actual Google test rewarded ad on an unrestricted host/device. Exercise UMP geography/required privacy options, dismissal, offline loading/display failure, repeated taps, activity replacement and background/process termination around reward delivery. Measure save latency and review small-screen/deadlock layouts. Keep this iteration's optional one-credit economy; add no other ad formats.
