# CODEX 19 — Hint credits and tile depth

A. Executive summary

Added three introductory Hint credits, independent of Undo, and stronger ceramic depth cues. Existing production tile availability, tray actions, generated levels, witnesses, progression, artwork, and matching effects remain authoritative. No advertising, publishing, or pushing.

B. Hint credit architecture

ProgressController owns a separate hintCredits balance. A successful suggestion is revalidated against the exact accepted-selection snapshot and the current production Game before debit. Invalid IDs, stale states, exhausted wallets and terminal states cannot charge. Hint requests do not select tiles or resolve pairs in the live game. All wallet commits and normal save submissions occur on the UI thread through the existing process-owned controller and ordered atomic-file writer.

C. Persistence and migration

Schema 5 appends the Hint balance to the schema-4 envelope. Schemas 1–4 receive three credits on migration. Explicit schema-5 zero stays zero; malformed schema-5 credit fields recover conservatively without granting more credits. Level identity, seed, generator version, accepted selections, tray reconstruction, Undo balance and completion frontier retain their existing representation. Normal initial controller publication persists the migrated schema. No navigation, restart, tier change or level advancement refills either wallet.

D. Selection algorithm

HintPolicy restores the actual current board and tray, enumerates legally available individual tiles using Game.isAvailable, and simulates Game.select on isolated copies. Deterministic order prefers tray matches, then higher layers and stable tile IDs. A shared 300-node depth-first search with board/tray memoization checks current continuations. SOLVABLE, UNSOLVABLE and UNKNOWN_BUDGET_EXCEEDED remain distinct. Within the tray-match priority, proven continuations precede unknown and known-unsolvable ones. Budget exhaustion never becomes proof. No original solution witness is assumed applicable after arbitrary selections.

E. Tray-aware behavior

Candidates that immediately lose are excluded, including a fourth distinct tray symbol. Available tray matches take priority. When no verified continuation exists, a legal, non-immediately-losing move can still be suggested; it carries no winning guarantee. No candidate means no debit and “No safe hint available.”

F. UI and badge

HOME | HINT | UNDO | RESTART use equal horizontal weights and 56dp height with compact 3dp margins. The original vector bulb uses muted gold, with a persistent numeric badge including zero and resource-backed accessibility text. Zero and terminal states disable Hint. Pending requests block duplicates. A two-second pulsing outline marks the suggested tile without moving it. Repeat taps during this highlight are ignored. Selection, restart, Home/back, destruction and board reload clear temporary effects; no visual effect is saved. Snapshot/request identity guards reject obsolete background results.

G. Tile thickness

A darker ceramic body extends to 0.085 tile units below the face, with the existing lighter edge at 0.045. Ivory top faces and inset highlights retain the existing dimensions and botanical art.

H. Shadows

Two directional offset bands provide contact shadow and a lighter outer falloff. Upper layers receive stronger contact contrast. Shadows follow the established light direction and are drawn before their own opaque tile faces.

I. Hidden-tile visibility

The lower ceramic body and edge remain visible wherever actual overlap geometry permits. Existing layer lift plus the thicker body reinforces exact stacks without changing logical coordinates. No transparency, symbol overlay, hidden-symbol hint, or outline through upper faces was added. Physical cues do not guarantee every fully covered tile is individually countable.

J. Rendering and touch safety

Draw order remains layer/y/x/ID; hit testing still reverses that order. Face and original edge targets, transforms, scale and engine blocking remain unchanged. The additional body/shadow regions are decorative. Existing available-tile checks still gate every accepted touch. The new body regression explicitly asserts that its extra region cannot be tapped.

K. Automated checks

Final diagnostic builder: all 196 JVM tests passed. This includes existing generation/witness, tray, Undo, progression, geometry and reliability suites, plus new Hint and decorative-body regressions. The full app and instrumentation source compilation and APK verification are recorded in docs/codex19/offline-build.log. Standard Gradle could not start because its wrapper lock location is read-only; this is not a Gradle/Lint pass.

L. Android instrumentation and visual inspection

New HintInteractionTest covers initial badge, rapid taps while pending/active, legal highlighted selection, unchanged live board before selection, Undo independence, restart retention, disk balance, activity recreation, fresh controller/disk relaunch, and restored zero disabling. Instrumentation execution, Android installation and screenshot inspection are PENDING: ADB cannot open its listening socket (“Operation not permitted”) in this sandbox. No updated Android screenshot was inspected. Existing layout/tray/defeat instrumentation remains available; do not infer runtime success from compilation.

M. APK

Fresh diagnostic debug APK: app/build/outputs/apk/debug/app-debug.apk. Exact timestamp, bytes, SHA-256, package, alignment, signature and updated resource/bytecode checks are in docs/codex19/apk-verification.txt. This APK is produced by the established fallback builder; it is not a standard Gradle release artifact.

N. Git

Existing .tilebloom-git history retained. The feature commit uses “feat: add three hint credits and improve Mahjong tile depth”. Pre-existing uncommitted navigation/manifest/test changes are retained separately in the working tree. No history rewrite and no push.

O. Known limitations

Android runtime behavior and visual quality remain unverified here. Node-bounded search can return uncertainty or a non-immediately-losing move from an already unsolvable attempt. Save writes retain the existing asynchronous ordered atomic-file semantics and error reporting; runtime storage failures are not modeled as successful durable persistence. The supplied Level 6 screenshot is not present in this workspace, and no visual comparison is claimed.

P. Next iteration

Run HintInteractionTest together with the full existing Android suite on a normal host. Install this APK and inspect Level 6 overlap regions on compact, medium and large portrait emulators. Capture actual screenshots and have the original player repeat the overlap recognition task before tuning shadow strength further. Run standard Gradle/Lint on that host.
