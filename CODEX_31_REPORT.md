# CODEX 31 — harmonized themes and Mahjong visibility

## A. Executive summary

Gameplay now uses a coordinated full-screen garden gradient and theme-derived status, tray, footer, tile and outcome colors. The isolated board panel is removed. Lower botanical artwork is painted before opaque upper pieces, so actual geometry determines what is visible. Side-blocked symbols remain recognizable with muted styling. No generator, gameplay, persistence, credit or advertising policy source changed.

This is a source-verified iteration, pending Android visual acceptance. A fresh standard APK could not be built in this sandbox. Nothing was published or installed; player data was not touched.

## B. Root cause

Previously only MahjongBoardView and TileTrayView received GardenTheme. GardenStyle supplied fixed forest-green colors to the rest of MainActivity, while the board painted a large independently colored rounded rectangle. TileVisibility also suppressed an entire lower symbol on any overhead logical intersection, even when a substantial portion of its face was physically exposed. The five user screenshots are described in the request but are not attached in this session; direct comparison with those image files was unavailable.

## C. Unified theme architecture

GardenTheme retains its existing stable IDs and deterministic level modulo selection. Its immutable GardenPalette supplies all sixteen requested screen tokens. Related surfaces are restrained blends of existing theme colors; ivory/gold remain shared botanical design constants. MainActivity applies the palette to the root gradient, system bars, gameplay text, status panel, control dock, buttons and outcome panel. GardenStyle accesses the active theme for controls and dialogs. Board and tray retain explicit theme properties. Theme application is cached by equality and never invokes generation.

## D. Garden palettes

| Theme | Background top | Background bottom | Ceramic base | Secondary accent |
|---|---|---|---|---|
| Spring | #173F35 | #38745B | #B9D5B9 | #E6B6C7 |
| Sunrise | #343B24 | #756044 | #E2C28D | #EFB270 |
| Lotus | #124B49 | #388D82 | #9DCFC6 | #E4AEC4 |
| Twilight | #25233E | #665C85 | #C1B4D5 | #C9B1DF |
| Autumn | #3C4430 | #735344 | #D6B590 | #E1A173 |

Available faces start at #FFF5DD, with theme ceramic shading; primary gold is #D9B96F. Blocked faces blend ceramic with ivory before theme-side shading and a muted overlay. Main surfaces remain dark enough for light text. JVM brightness checks are a limited mathematical safeguard, not device readability or formal contrast certification.

## E. Board background

MahjongBoardView no longer draws any full-view surface. The root gradient remains visible through the board surroundings and margins. The board retains its existing measured bounds for layout and touch. There is no board border or new opaque local panel.

## F. Available tiles

Available tiles retain original botanical vectors, ivory faces, inset highlight, border, physical edge and side. The existing engine availability decides their styling and selectability. Hint retains a bright gold stroke, dark theme outline and restrained brightness pulse, above the board.

## G. Blocked visible tiles

Symbols are drawn for every remaining tile in depth order, including side-blocked tiles. Their theme-derived ceramic faces and translucent darkening preserve recognizable botanical colors with a muted appearance. Existing corner marks reinforce blocked status. No random visibility decision exists.

## H. Partial and full occlusion

The painter order is layer, y, x, ID. Each lower shadow/body/edge/face/symbol finishes before the next upper piece. Opaque upper bodies, edges and faces hide the covered portion; exposed portions remain. Fully covered symbol pixels cannot appear through these opaque surfaces. Match-removal ghosts keep their existing depth-order fade after a legal removal. Hint remains a separate foreground pass validated against engine availability.

TileVisibility.faceVisibleAt is a read-only diagnostic query using the same rounded face/edge/body geometry and ordering. It reports physical point visibility independently of logical availability. It does not affect engine rules or choose whether to emit artwork. The obsolete all-or-nothing symbol guard and its test were replaced with geometric visibility assertions.

## I. Depth

Two-band contact and soft shadows now use theme-derived darkness instead of a fixed green-black. Side surfaces use coordinated darker ceramic instead of bright accent colors. Lit rims, gold intermediate edges and existing layer elevation preserve a consistent light direction. Geometry dimensions deliberately remain unchanged to preserve established targeting. Geometry is cached across frames and invalidated when loading a board or changing view size. Existing bounded painter loops and animation lifetimes remain; no graphics framework was added.

## J. Layout compatibility and responsive behavior

All v8 layout families, novelty checks, winning witnesses, frozen historical generators and difficulty boundaries remain unchanged: 1–7 Normal, 8–15 Hard, 16–30 Very Hard, 31+ Expert. Existing open/dense/terraced geometry naturally supplies visibility variation. No generator version bump is needed. New JVM checks use 320/360/400/600 pixel canvases for transform consistency; the Android capture harness uses actual dp at those widths. Device fit, touch comfort, insets and symbol readability remain NOT VERIFIED.

## K. Touch and Hint

BoardGeometry and Rules are unchanged. Topmost face/edge hit testing still precedes availability validation; shadows never become targets. Visible blocked tiles cannot be selected. HintPolicy, HintPresentation validation and credit consumption remain intact. New tests exercise blocked selection, Hint legality, topmost hits and reveal/Undo. Android pixel tests compare two underlying identities and require identical opaque-upper pixels plus differing exposed lower artwork in every theme; these tests are compiled but not executed.

## L. Saves, credits and ads

No serialization, seeds, versions, move history, tray rules, progression, credit wallets, UMP, AdMob IDs, connectivity or ad policy changed. Theme is reconstructed from the restored level ID rather than persisted as a new field. Saved puzzle identity and geometry therefore remain stable. Original signing configuration and keys were untouched. Device ad restoration and installed-save restoration require host verification.

## M. Automated results

**PASS: 248 JVM tests in 380.773 seconds**, including all 244 existing regression tests and four new rendering tests. The strengthened restart/relaunch assertions were then checked in a focused rerun: **4/4 PASS in 8.318 seconds** (reruns, not additional unique tests). Final main and Android test sources compile successfully with the cached diagnostic toolchain. Android pixel tests are compiled, NOT EXECUTED. Logs: [full JVM suite](docs/codex31/jvm-tests.log), [focused rerun](docs/codex31/focused-tests.log), [Android compilation](docs/codex31/android-final-compilation.log), [Gradle blocker](docs/codex31/gradle-cached.log). Standard requested Gradle test, assemble and Lint commands were attempted. The default wrapper fails on its read-only lock location. A writable home cannot download the distribution because socket creation is denied. Invoking the existing cached Gradle 8.13 directly with a writable home fails creating FileLockContentionHandler: no usable wildcard IP. Diagnostic compilation uses cached Kotlin 1.9.24/JDK 17/Android SDK/dependency jars; it does not substitute for Gradle/Lint or a packaged APK.

## N. Android visual verification

**NOT VERIFIED.** ADB cannot create its local server socket (`Operation not permitted`). No Android captures were generated or inspected. AdvancedBoardVisualTest now captures themed backgrounds at four dp widths with initial, one-upper-removed and opened board states. Its captures isolate games from player storage. Full-screen header/tray/footer screenshots, dense/blocked/covered details, brightness checks, device interaction and original-image comparison remain pending. Exact commands and limitations: [host verification](docs/codex31/HOST_COMMANDS.md).

## O. APK

**FAIL: no fresh CODEX 31 APK generated.** The pre-existing artifact is explicitly not this implementation. Its baseline details are preserved in [pre-existing APK evidence](docs/codex31/preexisting-apk.txt): 7,564,187 bytes, timestamp 2026-10-29 00:25:57 +0100, SHA-256 `da4fc009b3bfde5920313ec54e0cd0a1ab56ea75572ede4166bfc5408eddddc7`, package `com.tilebloom.game`. That old APK verifies its v2 signature and alignment; certificate matches `fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6`. This establishes baseline identity only. The fresh artifact's timestamp, hash, signature, alignment and included renderer are NOT VERIFIED. Nothing was uninstalled or overwritten on a device.

## P. Git

Existing `.tilebloom-git` history is used because this workspace's `.git` is a read-only placeholder. Changes are committed without reinitializing, rewriting or pushing. Unrelated pre-existing untracked CODEX_18_REPORT.md and docs/codex26 are excluded. Final commit identifier is supplied in the delivery message.

## Q. Acceptance matrix

PASS below denotes source/regression evidence where applicable; it does not claim device visual approval.

| # | Criterion | Result |
|---|---|---|
| 1 | Theme controls gameplay screen | PASS |
| 2 | Header and board harmonious palette | PASS |
| 3 | Tray theme | PASS |
| 4 | Footer theme | PASS |
| 5 | Disconnected board rectangle removed | PASS |
| 6 | Five distinct themes | PASS |
| 7 | Available botanical artwork retained | PASS |
| 8 | Visible blocked artwork retained | PASS |
| 9 | Blocked artwork darkened | PASS |
| 10 | Partial geometric occlusion | PASS |
| 11 | Fully covered symbols concealed | PASS (source geometry; Android pixels pending) |
| 12 | Upper layers opaque | PASS (source; Android pixels pending) |
| 13 | Theme depth/shadow treatment | PASS (appearance pending) |
| 14 | Deterministic rendering | PASS |
| 15 | Touch geometry preserved | PASS |
| 16 | Hint legality preserved | PASS |
| 17 | Procedural layouts valid | PASS |
| 18 | Difficulty progression | PASS |
| 19 | Four-slot tray | PASS |
| 20 | Save compatibility | PASS |
| 21 | Hint/Undo credits unchanged | PASS |
| 22 | AdMob functional on device | NOT VERIFIED (sources unchanged) |
| 23 | Existing JVM regressions | PASS |
| 24 | New rendering tests | PASS (JVM); Android pixels NOT VERIFIED |
| 25 | Android screenshots reviewed | NOT VERIFIED |
| 26 | Fresh APK generated/verified | FAIL |
| 27 | Signing compatibility | NOT VERIFIED for new APK; old signer matches |
| 28 | Git history preserved | PASS |
| 29 | Nothing published | PASS |

Acceptance totals: **25 PASS, 3 NOT VERIFIED, 1 FAIL**, with the device limits explicitly attached to the source-based visual entries.

Remaining limitations: no device screenshots, full-screen measurements, brightness playtest, performance benchmark, standard Lint or fresh APK. Shared GardenStyle holds the active screen theme; the immutable palettes themselves contain no mutable colors. No claim of endless layout variety or fairness improvements is made by this visual iteration.

## R. CODEX 32 recommendation

Run the documented standard host build and Android capture/pixel tests first. Review full gameplay composition at four widths, side-blocked recognition and upper-removal/Undo on Levels 17–20. Verify the new APK against the expected certificate before updating in place. Tune gradients or muted-symbol strength only from actual device images. Preserve the current generator and saves during that review.
