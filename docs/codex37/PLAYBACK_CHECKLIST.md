# Android playback verification pending

Use Java 17 and run tools/host_verify.sh on the host. Verify the new APK's
certificate equals fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6.
Only install with adb install -r; do not uninstall or clear player data.

- Listen to selection, reveal, switching/hiding, ordinary/direct matching,
  Hint, Undo, Restart, victory and defeat through speakers and headphones.
- Verify blocked/repeated taps and unsuccessful Hint/Undo produce no effects.
- Verify win/loss effects occur once and rapid taps have bounded overlap.
- Check comfortable loudness at normal system media volume; tune timbres by ear.
- Verify no music or continuous ambience plays anywhere.
- Toggle the single speaker control on Home and gameplay; mute must stop active
  effects immediately. Check level changes, relaunch and Activity recreation.
- Check TalkBack labels and 48 dp targets in portrait, landscape and large text.
- Background, interrupt audio focus, resume and verify no stale effects replay.
- Show rewarded/interstitial test ads; game effects must stop before presentation
  and must not queue. Check mute while interrupted; leave SDK audio unchanged.
- Run AudioPreferencesDeviceTest in its isolated namespace. It does not change
  real progression, credits or audio preferences.

No listening, live-ad playback, or device interaction has been verified here.
