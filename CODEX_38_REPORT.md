# CODEX 38 — Clickable saved-level Home card

Removed the separate gold Play button, its custom rendering class, layout wrapper,
IDs and obsolete Home copy. The existing green card is now the full-width primary
action, with gold saved level, warm ivory “Tap to continue your garden”, secondary
arrow, rounded bordered surface, ripple and a 100 ms subtle press scale.
The centered composition retains the title, illustration, gradient, mute control
and existing conditional Privacy control. Scroll layout and automatic level text
sizing protect small screens and large saved level numbers.

The card reuses the existing navigation.play(session.level.id) route and pending
initialization behavior. Online, transition, pending-entry and current-screen
guards prevent duplicate game entry; initialization consumes and clears the
pending action. Error recovery disables the card before recreating the Activity.
No puzzle generation, progression, wallet, save schema, recovery, loading screen,
connectivity, sound or privacy implementation was changed.

Accessibility: one focusable clickable card with Android Button class semantics,
“Continue Level N” content description, minimum 112 dp height and decorative
children excluded from separate accessibility navigation. All padding and child
areas belong to the same click target.

Android regression callers now target continue_level instead of the removed
Play control. The fresh-entry test dispatches a real local touch at the card's
corner, checks button semantics and its saved-level label, then activates it
again to verify the same board is retained. Existing saved-state, relaunch,
victory, defeat, offline, loading, privacy and large-level cases are retained.
Instrumentation is compiled diagnostically, not executed on a device.

Verification evidence is under docs/codex38. Standard Gradle JVM tests and debug
assembly were attempted: wrapper fails at its read-only distribution lock;
installed Gradle 8.13 with Java 17 and a writable temporary user home fails before
task execution because no usable wildcard IP is available. ADB cannot start its
listener (Operation not permitted), so no device screenshot, runtime interaction,
TalkBack or small-screen visual verification is claimed.

No fresh APK could be generated with Gradle in this environment. The pre-existing
APK was not replaced or installed and is NOT this change's deliverable. Its
signature verifies against required SHA-256 certificate
fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6.
Signing configuration and keys are unchanged. No uninstall, app-data clearing,
push or publication was performed. On a normal Android build host run
tools/host_verify.sh, then verify the certificate before any in-place install -r.

Final automated results: all 304 existing JVM regression tests passed using the
cached Kotlin compiler/JUnit runner with Java 17 (175.59 seconds; see
docs/codex38/tests-final.log). Main and Android instrumentation sources plus
Android resources compiled/linked successfully using cached tools; the revised
corner-touch test also passed source compilation. This is not Gradle task success
or executed instrumentation. git diff --check passed.
