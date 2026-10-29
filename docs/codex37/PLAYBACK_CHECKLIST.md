# Host/device verification still required

Use Java 17 and run tools/host_verify.sh. Keep player data and the existing
signing identity. Install with adb install -r; never uninstall or clear data.
Verify the fresh APK certificate, raw resources, package, timestamp/hash and
alignment. Existing APK evidence here is explicitly old.

Listen with speakers/headphones at normal system media volume:
- Select available/blocked tiles; reveal/switch/hide mysteries; ordinary/direct
  matches, rapid pairs, Hint, successful/unsuccessful Undo, Restart, win/loss.
- Enable music and listen across at least three 48-second boundaries. Review
  timbre, loudness, fatigue, transitions and clicks by ear.
- Toggle each independent switch and master mute; confirm previous choices
  return. Check Home, game, offline Home and offline gameplay overlay.
- Relaunch and recreate the Activity; verify preferences and one music player.
- Background/foreground, system media volume, phone/focus interruptions,
  focus denial, headphones/Bluetooth and mute while interrupted.
- Play both rewarded and interstitial test ads. Confirm game audio suspends
  before presentation and resumes only when permitted, rewards remain exact,
  and victory is not replayed. The toggle does not control ad SDK audio.
- Check TalkBack, large text, small portrait/landscape and loading/next level.

AudioPreferencesDeviceTest uses an isolated storage namespace and does not
change real audio preferences, progression or credits.
