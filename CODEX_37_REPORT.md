# CODEX 37 — Sound effects only and persistent mute

Source implementation is complete. Fresh Gradle APK generation, Android Lint,
and real Android playback verification are blocked by the execution environment.
The APK already in the output folder is OLD and contains the previous music
resource; it must not be treated as the sound-effects-only deliverable.

A. Implemented effects: ceramic-inspired selection, gentle mystery reveal/hide,
crystalline matching, Hint sparkle, reverse Undo, Restart, victory, descending
defeat, and a short navigation button click. Confirmed interactions alone emit
cues. Invalid actions emit nothing; end-state transitions emit once. Mystery
collection/direct matching no longer emits an erroneous hide cue. Gameplay,
Option C mystery rules, four-slot tray, credits, generation, progression and
connectivity requirements were not changed. Optional match pitch variation is
omitted. All MediaPlayer/music playback, loop generation, the bundled loop,
music preferences in the policy, settings dialog and settings icon were removed.

B. Architecture: the existing dedicated GameAudioManager now owns only a
three-stream SoundPool with sonification attributes. All ten resources preload
once per Activity lifetime; interactions reuse loaded resources without decoding.
Not-yet-loaded cues are dropped, never queued. Playback uses 30% gain and no
looping. Per-cue 65 ms throttling also bounds alternating rapid taps; the backend
stops the oldest stream above three. Application context only is retained.
Destruction releases resources, with an idempotent close. Pure Kotlin AudioPolicy
and GameplayAudio support deterministic fake-backend tests.

C. Provenance: original procedural synthesis in tools/generate_audio.py, using
Python standard library only, with no external samples or recordings. Source
and audio assets are dedicated under CC0-1.0; see AUDIO_ASSETS.md. Ten mono
22.05 kHz 16-bit PCM assets total 259,528 bytes (about 253 KiB). Signal analysis
found no clipped samples; detailed durations/hashes are in docs/codex37/assets.json.
Procedural timbres have not been approved through listening.

D. Controls: one visible speaker/crossed-speaker button on Home and the gameplay
header, also retained in the offline panel. Each target is 48 dp and uses garden
surfaces/colors. TalkBack text states sound on/off and the available action.
One tap updates every control and immediately stops active effects when muted.
Platform click sounds are disabled on the board and styled buttons, including
the mute buttons, so Android's default clicks cannot bypass the game mute.

E. Persistence: existing garden_audio SharedPreferences store the selected mute
state using apply(). Defaults are effects enabled and unmuted. Previous disabled
effects migrate into mute, so existing silence is respected; unmuting restores
effects. The obsolete music key is ignored and removed on preference changes.
Changing levels never resets this state. Android instrumentation tests cover
manager recreation and migration in isolated namespaces without player data.

F. Lifecycle/ads: playback requires resumed/focused foreground state, audio focus,
no full-screen ad, and unmuted effects. Background, focus loss, mute and ads stop
current streams; no queue or past events replay afterward. Audio focus is
abandoned on suppression. The existing AdExposure gate covers rewarded and
interstitial presentation. AdMob, Google UMP, rewards, frequency, and SDK audio
configuration are unchanged. No service or third-party audio dependency was added.

G. Automated verification: all 304 JVM tests passed in 172.745 seconds, including
the existing gameplay/progression/advertising regressions and 21 sound-policy
tests. Final results are recorded in docs/codex37/tests-final.txt.
Final main and instrumentation Kotlin compilation plus Android resource
compilation/linking passed using the cached compiler on Java 17; see
android-compilation.txt. These are diagnostics, not Gradle task success or Android
instrumentation execution. Fake-backend tests cover every cue under mute/ad
suppression, unmute, background/focus, preference recreation, level changes,
rapid alternating cues, accepted Hint/Undo/Restart, selection, reveal, hide,
mystery collection/direct pairing, and once-only victory/defeat.

H. Android playback: NOT VERIFIED. ADB cannot open its listener (Operation not
permitted). No speaker/headphone listening, audible quality, device latency,
TalkBack interaction, relaunch interaction, or live-ad audio test is claimed.
Follow docs/codex37/PLAYBACK_CHECKLIST.md on an available Android host/device.

I. APK/build: all three exact requested wrapper commands were attempted and
failed before task execution at the read-only Gradle wrapper lock. Installed
Gradle 8.13 with a writable temporary user home also failed before task execution:
Could not determine a usable wildcard IP for this machine. Tests/assemble/lint
therefore have no Gradle success, and no fresh APK was generated. The existing
APK is 7,732,535 bytes, modified 2026-10-29T10:41:17.545938+00:00, SHA-256
b7e3759b06a04c04e0f2455d728467fc90269de4585087606e11c986a06e1c9a.
Its signature verifies against required certificate
fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6.
It contains the PREVIOUS implementation and music resource. It was not replaced
or installed. Signing configuration and keys are unchanged. Use Java 17 and
tools/host_verify.sh on a normal host to build/check the new APK; verify the
certificate before an in-place adb install -r. Never uninstall or clear data.

J. Git: use the existing .tilebloom-git history with commit message
feat: add premium game sound effects with persistent mute control.
No history rewrite, signing keys, unrelated untracked files, push, or publication.

K. Limitations: a fresh signed APK, successful standard Gradle tests and Lint,
Android instrumentation, listening/timbre tuning, and actual device lifecycle/ad/
accessibility checks remain outstanding. Historical tests.txt and audio-tests.txt
in docs/codex37 describe the superseded music implementation; tests-final.txt
and this report describe the correction.
