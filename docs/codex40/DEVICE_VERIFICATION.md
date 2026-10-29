# Host follow-up

Use Java 17, the existing local compatible signing configuration, and a working Android SDK. Run all three Gradle tasks listed in CODEX_40_REPORT.md plus `:app:assembleDebugAndroidTest`. Inspect lint results without suppressing missing translations. Verify the newly built APK using `aapt2 dump badging`, `aapt2 dump configurations`, `zipalign -c -P 16 -v 4` and `apksigner verify --verbose --print-certs`; record its timestamp, size and SHA-256. The certificate must match the report. Install as an update only; never uninstall or clear player data.

Execute LocalizationDeviceTest and LanguageSwitchInteractionTest plus the existing instrumentation suite. Test language changes through both TileBloom Settings and Android 13+ system app-language Settings. Test at least one API 26–32 device and one API 33+ device; verify explicit selection, system Automatic selection, unsupported-system English fallback, force-stop/relaunch persistence and OS-upgrade preference migration.

Capture Home and Settings in en/fr/es/pt-BR/ar/hi/ja/de. Also capture Arabic gameplay, Hindi victory, Japanese offline, German reward prompt, Portuguese loading and Spanish defeat. Use a narrow portrait screen and increased font scale. Check glyph shaping, connected Arabic, mixed numbers, natural RTL UI, unchanged board/tray geometry, scrolling, readable labels and TalkBack. Restore the tester's original language afterwards.

All device checks in this environment are NOT VERIFIED. ADB cannot start its listener. Existing APK metadata describes an older artifact, not this change.
