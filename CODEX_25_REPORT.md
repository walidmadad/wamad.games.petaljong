# CODEX 25 — TileBloom

## A. Executive summary

Implemented validated online gameplay, a connection-required panel, restrained garden UI refinements, and four original Canvas symbols. Existing engine rules, four-slot tray, SDK integrations, rewards and progression remain. Standard Android delivery is blocked by the host environment: **no fresh APK was generated and no emulator test ran**. Nothing was published, pushed, uninstalled or cleared.

## B–D. Connectivity, suspension and recovery

`ConnectivityMonitor` uses ConnectivityManager's default route, INTERNET and VALIDATED capabilities, and registerDefaultNetworkCallback with a worker Handler. It registers once per started Activity, unregisters on stop, rejects stale UI deliveries and shuts down its HandlerThread on destruction. Initial network reads and Retry run off the UI thread. ACCESS_NETWORK_STATE and INTERNET were already present; permissions are unchanged.

Input closes as soon as a negative connectivity result reaches the UI. A tested 600ms handover window delays only the panel; repeated callbacks cannot extend its deadline. Validated connectivity removes the panel automatically. Wi-Fi being enabled, transport type, consent and ad inventory are never substitutes for validation. Android validation is a local connectivity signal, not proof that every service is reachable, remote authentication, or tamper resistance.

The full-screen gameplay shade intercepts touches; board dispatch and action listeners also guard input. Hint results pending at disconnection are invalidated. Board/tray visual feedback is cleared without changing authoritative selections, tray contents, credits or game status. Back, Home, Hint, Undo, Restart and Next are gated. The regular repository persists the accepted state. Reconnection updates presentation without restarting or reconstructing the board. An already accepted asynchronous level transition retains its existing durable transaction behavior; disconnection prevents new transition requests.

The home page retains the current level, illustration, disabled Play and privacy entry. Its connection card provides Retry and Open Network Settings. Offline cold entry does not automatically finalize a pending victory into the next level. Settings remain usable. Activity recreation reuses process-owned game state and creates a replacement lifecycle monitor.

## E–F. Visual refinements

Reduced home illustration/card/button gaps, refined level typography, retained the centered inline Play icon/text and common content width. Advertising privacy is now a smaller labeled shield action at the secondary lower-right position; it still opens the official UMP form when required and remains discoverable offline.

Gameplay status spacing is more consistent. The four portrait tray slots retain their dimensions and miniature scaling, with a subtle container outline and quieter empty-slot borders. Credit and AD capsules have separating outlines for readability. Existing circular controls, descriptions, touch targets, reward dialogs, board hit geometry and garden palette remain. The connection panel uses original vector artwork, ivory text, forest surfaces and muted-gold borders. Human visual review on compact/large-font screens is **NOT VERIFIED**.

## G–H. Catalog and generation

Existing IDs 0–15 remain unchanged. Appended IDs:

| Symbol | Stable ID | First new generated level |
|---|---:|---:|
| Cactus | 16 | 6 |
| Lavender | 17 | 11 |
| Ladybug | 18 | 21 |
| Snail | 19 | 41 |

Cactus uses arms and a pot; lavender uses repeated purple flowering sections; ladybug has a divided red body, head, legs and spots; snail has a spiral shell, body and feelers. All are Canvas paths/shapes, with accessible localized-resource labels; no emoji or downloaded artwork.

Generator **6** preserves geometry, quality budgets and 20-level tier policy. Eligible new symbols are explicitly included in the active set (and each milestone's newly introduced old symbols are prioritized), then assigned complete witness pairs. Each eligible new symbol continues to appear in subsequent v6 boards. They are not inserted into handcrafted/frozen levels 1–5 or historical saved boards. Active type counts and existing difficulty ranges remain unchanged. Additional types alone are not claimed as greater human difficulty.

## I. Save compatibility

Save schema stays 5. Version 5 construction is frozen in `VersionFiveGenerator`, filtering its catalog to the original 16 symbols. Version 4 restoration remains supported. Restore dispatches on generator version and validates replay history; it never replays v5 selections against v6 assignments. Level numbers, frontier, Hint and Undo balances retain their existing codec contracts. Six v5 fingerprints (levels 6, 11, 21, 41, 101 and 10000) match the pre-change compiled implementation exactly; see historical-v5.txt and preserved-v5.txt. New tests restore partially collected trays and balances for both versions.

## J. Ads and economy

RewardedAdManager, Google Mobile Ads 25.5.0, UMP 4.0.0, test IDs, official consent forms, earned-reward callback and duplicate protection are preserved. Gameplay connectivity does not read canRequestAds, consent or ad availability. Initial 3 Hint/3 Undo and +1 earned rewards are unchanged. Offline UI closes a pending reward invitation; an earned SDK reward keeps its original delivery contract. Live SDK behavior is **NOT VERIFIED** here.

## K. Automated verification

**216/216 JVM tests PASS** (30.099 seconds), using Java 17 and cached Kotlin 1.9.24/JUnit 4.13.2. See docs/codex25/jvm.log for the final cached-compiler JUnit run. The regression suite exercises deterministic generation, full production-controller witnesses, new-symbol matching/Undo/Hint, both historical and current saves, progression, credit economy, and the connectivity capability truth table and handover timing. Existing expectations for the expanded catalog and generator version were updated; tests were retained.

Android tests added for offline home and input guards, exact tray/credit preservation, reconnection with consent unavailable, offline victory gating, Activity recreation and every renderer at board/tray scale. They are **compiled diagnostically but not executed**. Capability tests are transport-independent; real cellular, captive Wi-Fi and handovers still require device execution.

Main and Android test Kotlin sources and Android resources pass diagnostic compilation against cached SDK libraries. This diagnostic uses Kotlin's metadata-version check bypass for the newer Ads dependency and is **not** an authoritative Gradle build or Lint result.

## L. Android runtime

ADB cannot start its server: `could not install smartsocket listener: Operation not permitted`. No launch, radio toggle, reconnect, screenshot, compact-screen inspection, ad display or upgrade installation was performed. The new Android tests and manual checklist remain pending.

## M–N. APK and signing

All three requested normal commands were attempted:

- `./gradlew --no-daemon --console=plain :app:testDebugUnitTest`
- `./gradlew --no-daemon --console=plain :app:assembleDebug`
- `./gradlew --no-daemon --console=plain :app:lintDebug`

They fail before tasks at the read-only default Gradle wrapper lock. Installed Gradle 8.13 with Java 17 and a writable `/tmp` user home also fails before tasks: `Could not determine a usable wildcard IP for this machine`. Logs are in docs/codex25.

**Fresh APK: FAIL.** New timestamp, size, SHA-256, packaged connectivity/symbol/SDK checks, package ID, alignment and signature are **NOT VERIFIED**. Any existing APK is stale evidence and is not delivered as CODEX 25.

The existing local signing configuration and key are unchanged. The configured certificate was rechecked and matches **fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6** (local-certificate.txt). Fresh APK signature and installed-app update compatibility remain unverified. No keys or credentials are committed.

## O. Git

Existing `.tilebloom-git` history is preserved. Use `git --git-dir=.tilebloom-git --work-tree=. log -1 --oneline` to identify this change's commit. Existing unrelated untracked CODEX_18_REPORT.md is left untouched. No history rewrite, push or publication.

## P–Q. Limitations and CODEX 26

Run the normal Gradle tasks in a host permitting Gradle/ADB sockets. Produce and inspect a fresh APK, confirm the exact signing digest, then update-install without uninstalling or clearing data. Execute the new instrumentation and the Wi-Fi/mobile/captive-network/recreation matrix, inspect compact and large-font layouts, and verify optional test rewarded ads. Treat that Android verification and APK delivery as the first CODEX 26 task. Connectivity enforcement remains local and subject to Android validation latency.

## Acceptance criteria

PASS below identifies source/JVM verification where applicable; it does not imply unexecuted Android runtime success.

| # | Requirement | Result / evidence |
|---:|---|---|
|1|Internet required|NOT VERIFIED on Android; source gate implemented|
|2|Validated connectivity|PASS source and capability tests|
|3|Wi-Fi without Internet blocked|NOT VERIFIED on real network; unvalidated capabilities rejected|
|4|Loss suspends gameplay|NOT VERIFIED runtime; guarded implementation/UI test added|
|5|Reconnection restores gameplay|NOT VERIFIED runtime; UI test added|
|6|Offline cannot mutate puzzle|NOT VERIFIED runtime; listener/board guards and UI test added|
|7|Saves remain valid|PASS v4/v5/v6 replay tests and historical fingerprints|
|8|Undo balance preserved|PASS save/regression tests; offline UI test pending|
|9|Hint balance preserved|PASS save/regression tests; offline UI test pending|
|10|Consent independent|PASS source; deterministic UI test pending|
|11|Ad availability independent|PASS source; deterministic UI test pending|
|12|AdMob functional|NOT VERIFIED live; SDK integration unchanged|
|13|Home refined|PASS source; visual review pending|
|14|Play alignment retained|PASS existing inline centered span; runtime pending|
|15|Gameplay spacing|PASS source; visual review pending|
|16|Tray refined|PASS source; visual review pending|
|17|Controls accessible|PASS descriptions/target sizes retained; TalkBack pending|
|18|Four symbols implemented|PASS exhaustive renderer compilation|
|19|Visually distinguishable|NOT VERIFIED human visual review|
|20|Symbols in generation|PASS deterministic pair/witness tests|
|21|Existing IDs stable|PASS catalog tests|
|22|Initially solvable|PASS full production witness replays|
|23|Forward-only endless progression|PASS regression suite|
|24|Difficulty tiers correct|PASS milestone/profile regression suite|
|25|Existing JVM regressions|PASS cached-compiler JUnit; Gradle unavailable|
|26|New regressions|PASS JVM; Android tests not executed|
|27|Android UI tests updated|PASS source/diagnostic compilation|
|28|Fresh verified APK|FAIL environment blocks Gradle|
|29|Signing compatibility|NOT VERIFIED new artifact; configured local certificate matches|
|30|Git history preserved|PASS existing history retained|
