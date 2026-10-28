# CODEX 22 — Optional rewarded ads (Android validation BLOCKED)

A. Executive summary: source integration adds voluntary Google rewarded ads for
one Undo OR one Hint, with UMP consent and debug-only test identifiers. Full
acceptance is **BLOCKED**: no standard Android compilation, instrumentation or
real test-ad playback could execute in this restricted environment. This is not
a verified ads-enabled APK delivery. Nothing was pushed or published.

B. Existing project audit: inspected MainActivity navigation, PlayerProgress,
ProgressController, UndoCreditWallet, ProgressRepository, GameStore, schema 5,
manifests, Gradle settings, verification scripts, instrumentation and Git history.
The actual CODEX 21 commit is e4ed3f3 (high visibility Hint). Existing navigation
uses START/GAME plus forward-only LevelNavigation. GameStore owns the live
controller across Activities; ProgressRepository has a single ordered writer,
AtomicFile and a synchronous durable transition method. Schema 5 stores frontier,
board identity/seed/version, accepted selections, Undo and Hint separately. Hint
balances are bounded 0..3. Existing initial balances are three each. The old Undo
reward ticket/gate existed as domain scaffolding without an Android ad SDK.
Repositories already include google() and mavenCentral(). Normal .git is an empty
read-only directory; the intact working history is in .tilebloom-git, used with
explicit --git-dir and --work-tree. No Git reinitialization or history rewrite.

C. AdMob integration: adds official legacy play-services-ads:25.5.0. Google release
notes and migration documentation confirm this version; min API 24 is compatible
with existing minSdk 26, compile/target 36. No Next-Gen classes, mediation or
Firebase. Manifest declares INTERNET, network state and placeholder application
metadata. Debug uses exactly Google's provided test App ID and rewarded unit.
Release has no usable ad unit and fails preReleaseBuild rather than falling back
to test monetization. Dependency resolution/SDK Kotlin linkage remain unverified
until standard Gradle compiles. Source: https://developers.google.com/admob/android/rel-notes
and https://developers.google.com/admob/android/migration .

D. UMP: adds user-messaging-platform:4.0.0. On Activity creation, requests updated
consent, loads/shows required form and uses canRequestAds before initialization,
loading and showing. A process guard prevents overlapping consent forms and
duplicate SDK initialization; cached consent eligibility follows Google's flow.
Privacy button on Home opens UMP privacy options when required, otherwise explains
that no form is currently required. Privacy changes invalidate cached requests.
No fabricated eligibility or forced debug geography. Actual region/form behavior
is unverified. Reference: https://developers.google.com/admob/android/privacy .

E–F. Undo and Hint: zero-credit controls remain enabled accessible buttons and open
a small confirmation dialog identifying exactly one selected reward. Cancel does
nothing. Watch Ad only shows an already-ready ad, never queues an automatic show.
A confirmed SDK reward increases only the selected zero wallet to one, updates
its badge and never performs Undo/Hint. Nonempty controls retain existing actions.
No initial allocation or saved balance migration is introduced.

G. Callback protection: explicit RewardPurpose is immutable for each AdReward;
AdPresentation serializes presentations. One-shot tickets accept the official
OnUserEarnedRewardListener at most once. Load/show/dismiss do not invoke earned.
Display failure cancels the ticket. Dismissal never revokes credit; callbacks keep
their ticket across Activity recreation and reference application context, not
an obsolete Activity controller. Manager host uses WeakReference. No server-side
verification exists. Modified clients can tamper with local saves; process death
before callback/durable write can lose an uncommitted reward. Failed persistence
shows failure and does not retry the same callback or falsely report saved credit.
Reference: https://developers.google.com/admob/android/rewarded .

H. Persistence: GameStore.reward requires the main thread and rejects pending
level transitions. ProgressController captures authoritative current state and
prepares a copy changing one balance. Existing ordered saveDurably commits that
entire state using AtomicFile before either live wallet changes. Earlier async
writes drain first; refreshing the process controller cannot submit an obsolete
Activity snapshot. Current board, seed, generator version, tray history/frontier
and the other wallet are preserved. The schema stays at 5. This follows the
existing synchronous durable-transition pattern: measure slow-storage UI latency
on devices. There is no durable failed-reward retry queue.

I. Loading/errors: explicit IDLE/LOADING/READY/SHOWING/FAILED states, one loading
request, stale-generation rejection, one-hour cache expiration and 30-second
failure backoff. Reload after dismissal; no tight retry loop. Consent restrictions,
no ready ad and load/presentation errors produce no reward or debit. SDK exceptions
are caught during initialization/load/show. SDK network/Play services failures are
handled as unavailable, without resetting the puzzle. Recovery still needs real
Android network and services testing.

J. UI: HOME/HINT/UNDO/RESTART and compact footer remain. Zero badges advertise the
reward through accessibility descriptions/tooltips; prompts use forest panel,
gold/ivory actions and existing rounded surface styling. Privacy options add a
small Home button. No advertising panel, banner or automatic full-screen ad.
Dialog is dismissed on destruction. Visual emulator review is outstanding.

K. Gameplay preservation: no changes to Game, generators, tray, matching,
availability, defeat/victory, Undo reconstruction, HintPolicy, MahjongBoardView or
HintPresentation. CODEX 21 foreground outline/bulb and four-second animation remain
untouched. Restart, Home and Next Level have no ad-show calls. New reward domain
regressions exercise board identity and balance persistence across restart/advance.

L. JVM results: final standalone JUnit regression run recorded in
`docs/codex22/jvm.log` (see the terminal result at end of that file). It compiles
actual domain/board code and all test sources using cached Kotlin 1.9.24 and Java
17. This is **not** standard Gradle or Android validation. New deterministic fake
provider tests cover both reward purposes, independent credits, no load/show/
dismiss rewards, rapid presentation rejection, duplicate callbacks, display
cancellation, failed persistence, schema round trips, recreation-style repeated
observers, preserved zero balances/frontier and restart/next-level balances.
The 10 host script workflow tests pass; these use synthetic APK/tool fixtures
and are also not Android verification.

M. Android instrumentation: **NOT EXECUTED**. Added RewardInteractionTest with a
fake only in androidTest, isolated test-* disk save, zero prompts, both rewards,
cancel, unavailable ads, persistence, recreation, restart and privacy button.
AdSdkConfigurationTest checks actual linked SDK version, UMP access and manifest
IDs; it does not itself prove initialization. Updated existing zero Hint UI
expectation for the intentional new reward control. Real consent-gated SDK
initialization, privacy form UI, offline/network recovery and real callback UI
remain outstanding. Android test sources are not claimed to compile here.

N. Standard Gradle: **BLOCKED before any task executed**. First command:
`./gradlew --console=plain :app:testDebugUnitTest` failed opening
`/home/walid/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13-bin.zip.lck`
with `java.io.FileNotFoundException ... (Read-only file system)`.
One adapted attempt used a writable /tmp Gradle home and the installed Gradle
distribution, requesting `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`
with `--console=plain`. It failed at Gradle startup:
`Could not create service of type FileLockContentionHandler ... Could not determine a usable wildcard IP for this machine.`
Logs: docs/codex22/gradle-default-home.log and gradle-writable-home.log. No identical
restricted retries or fake replacement advertising classes were used. The default
Java was 25; the JVM checks/host instructions explicitly select required Java 17.

O. Real Google TEST ad: **NOT VERIFIED**. ADB could not start:
`could not install *smartsocket* listener: Operation not permitted`; no device could
be enumerated, installed to, or playtested. See docs/codex22/adb.log. No real
progression was cleared or edited. Detailed real-emulator checklist and consent
account limitations are in ADMOB_VERIFICATION.md.

P–Q. APK / signature / alignment: **NO NEW VERIFIED APK**. No size, timestamp,
SHA-256, signature, alignment or install success is asserted. Expected future
standard Gradle artifact is `app/build/outputs/apk/debug/app-debug.apk`; any
pre-existing artifact is stale evidence. Diagnostic APK building is disabled in
tools/verify_offline.py (JVM-only remains available). The host script now verifies
freshness, timestamp, bytes, hash, package, manifest test ID, SDK DEX descriptors,
alignment and signature after a successful standard build.

R. Git: a meaningful source-work commit is made on the existing .tilebloom-git
history with an explicit pending-Android-validation message. Obtain its exact ID
with `git --git-dir=.tilebloom-git --work-tree=. log -1 --oneline`. The pre-existing
untracked CODEX_18_REPORT.md is excluded. Nothing is pushed.

S. Remaining blockers: standard Java-17 Gradle dependency resolution/compilation,
Lint, Android test compilation/execution, actual consent/initialization, real TEST
rewarded ads and emulator visual/gameplay verification. Full user acceptance is
not met until these pass in an unrestricted host environment. SDK dependency or
Kotlin errors may still be discovered there.

T. Production monetization: **NOT READY**, deliberately blocked. Test ads earn no
production revenue. Requires configured AdMob account and release-only app/unit
IDs, applicable UMP consent messages, real privacy policy, Play Data safety/ads
disclosures, audience/age/policy review, real-device testing and signed production
build verification. No production IDs/credentials/privacy documents invented.

U. Next iteration: run the exact Java-17 standard Gradle commands and
`tools/host_verify.sh` from ADMOB_VERIFICATION.md, fix any Android-specific errors,
then run connectedDebugAndroidTest and record both real TEST reward flows on a
Google-enabled emulator without clearing existing saves. Keep release disabled
until this evidence exists. Do not publish.
