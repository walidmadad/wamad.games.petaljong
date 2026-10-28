# CODEX 27 — Dedicated offline presentation

A. Root cause: the old connection panel was reparented into `home_actions`, below the still-visible botanical art and level card. It composed an offline message with an online home rather than presenting an independent state.

B. Architecture: `offline_home` is a dedicated fill-viewport ScrollView with a vertical composition, title, flexible spacing, status/actions and bottom privacy access. The normal home is hidden whenever home lacks validated connectivity, including initial resolution. Existing ConnectivityMonitor, validated policy and handover timing are unchanged. Online home and offline home are mutually exclusive. The shared connection content moves into a scrolling gameplay shade only on the game route.

C. Artwork: original 96dp VectorDrawable, forest disc, inner green highlight, thin muted-gold ring, ivory Wi-Fi arcs/dot and a separate gold diagonal slash with a forest separation stroke. No emoji or external asset.

D. Actions: primary gold Retry uses a centered inline icon/text run. It only asks the existing monitor to recheck; it never generates a puzzle or changes progress. A one-second guard and disabled state prevent repeated taps; a polite accessibility announcement reports checking. Secondary Network Settings has a transparent outlined surface and centered gold icon/text. It tries the Internet panel on API 29+, then wireless/general settings, handling unavailable activities without crashing.

E. Privacy: required UMP entry is centered below flexible spacing at the offline screen bottom, outside the status/actions content, with a gold shield/label and at least 48dp target. Official UMP handling remains intact, including its existing failure toast. Online placement is retained. Consent and ad availability do not determine connectivity.

F. Online home: existing title, illustration, level card, icon-only Play behavior and privacy entry remain. Reconnection reveals the saved session level. No domain, generation, progression, repository, credit or gameplay rules were changed.

G. Gameplay: existing input/listener guards and shade remain. Connectivity changes update presentation without selecting tiles, resetting boards, tray contents, credits or progression. The active game route is retained through reconnection and Activity recreation. The overlay reuses offline artwork, heading and actions, with scrolling for constrained heights.

H. Responsiveness: bounded 420dp composition on large screens, flexible vertical spaces, wrapping text, scroll overflow and existing system-bar/cutout insets. Instrumentation assertions cover 320/360/400/600dp portrait widths, centered panel/privacy and visible action bounds. These assertions have NOT been executed. Large-font, short-screen and human visual results remain unverified.

I. Accessibility: visible “YOU'RE OFFLINE” heading, API-guarded heading semantics, polite status region, Retry announcement, descriptive action labels, decorative artwork excluded from duplicate announcements, and accessible button targets. No flashing or added animation. TalkBack and increased-font runtime inspection remain pending.

J. Tests: existing JVM regressions retained. Android presentation tests now assert absent level/art/Play offline, visible icon/Retry/settings/privacy, reconnection restoring normal home and saved level, rapid Retry suppression, level-four/credit preservation across home recreation and online Play navigation. Existing gameplay tests cover blocked tile taps/actions, exact session/tray/credit preservation, victory gating and gameplay recreation. Diagnostic compilation is not instrumentation execution.

K. Android screenshots: unavailable. ADB fails to start its server (`could not install smartsocket listener: Operation not permitted`). No device was uninstalled or cleared. No offline/reconnected/gameplay screenshots were captured. Visual success is not claimed.

L. APK: all three exact requested Gradle commands were attempted; each failed at the default read-only wrapper lock. Installed Gradle 8.13 with Java 17 and writable `/tmp` user home also failed before tasks (`Could not determine a usable wildcard IP for this machine`). No fresh debug APK could be generated. New timestamp, file size, SHA-256, packaged identifier, alignment and APK signature are therefore NOT VERIFIED; no old APK is represented as this delivery. Configured application ID remains `com.tilebloom.game`.

M. Signing: local configured certificate SHA-256 rechecked and matches `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. Signing configuration and credentials were not changed or committed. Installed-app/new-APK compatibility cannot be verified without a fresh build and device access.

N. Git: existing `.tilebloom-git` history retained; no reinitialization, rewrite, push or publication. Related pre-existing uncommitted home/layout test refinements are integrated with this redesign. Unrelated pending Ads changes and reports remain outside this commit. See `git --git-dir=.tilebloom-git --work-tree=. log -1` for the implementation commit.

O. Limitations: standard Gradle unit/build/lint and actual Android instrumentation, screenshots, font scaling, real network transitions and APK delivery remain blocked by this host. Run the standard tasks and connected instrumentation in a host permitting Gradle/ADB sockets, then capture offline/reconnected home and gameplay recovery and verify/update-install the signed artifact without uninstalling or clearing data. No publishing occurred.

Verification logs: `docs/codex27/`. Diagnostic cached-compiler results are recorded below when complete; they do not replace Gradle or device evidence.

Cached-compiler JVM result: **216/216 PASS**, 27.267 seconds under Java 17. These are production-domain regression tests, not Android presentation tests. Resource linking and diagnostic main/instrumentation Kotlin compilation also pass using cached dependencies and the diagnostic metadata compatibility flag. Android tests were compiled, not executed.

Implementation commit: `4f50b14` — `fix: create dedicated premium offline screen`.
