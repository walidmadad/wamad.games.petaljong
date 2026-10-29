# CODEX 34 verification environment

Commands use Java 17 (`PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH`). The host default Java 25 cannot compile the cached Kotlin 1.9.24 compiler. No dependencies were downloaded.

Standard tasks attempted:

- `./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`: wrapper lock path is read-only (`gradle.log`).
- Writable Gradle home: wrapper attempted a distribution download; network sockets are forbidden (`gradle-writable.log`).
- Direct installed Gradle 8.13 with writable home, offline and no daemon: Gradle cannot determine a usable wildcard IP / create its lock contention service (`gradle-cached.log`).
- `adb devices`: ADB cannot install its socket listener (`adb.txt`). No device installation, instrumentation, uninstall, or data clearing occurred.

Final isolated JVM verification passed all 264 tests (tests-final.txt). The final independent Android source/instrumentation compilation also passed (android-final.txt). These do not replace the blocked Gradle tasks.

The independent cached Kotlin compiler check compiles production Android and instrumentation sources against the Android 36 SDK and cached dependency jars. The exact diagnostic recipe is preserved in android-compile-recipe.py; its jars came from a previous diagnostic cache under /tmp. This is a compilation check, not Android Lint, an executed device test, a standard Gradle build, or a distributable APK.

The previous offline test runner deletes app/build/offline on startup. Overlapping exploratory runs invalidated each other's classes; jvm*.log results are not final verification evidence. tools/test34.py now gives each invocation a fresh temporary directory. Use tests-final.txt for the isolated final result. All existing test methods remain; tests that assumed full-board pair moves now replay the authoritative winningSelections sequence, and the botanical identity/Hint/Undo fixture tests those contracts without requiring immediately available partners in generated advanced boards.

Reproduce final JVM checks:

```sh
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/test34.py
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH python3 tools/benchmark34.py
```

To finish APK verification on a host permitting Gradle:

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 tools/host_verify.sh
```

Verify the fresh artifact's certificate SHA-256 equals fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6 before any update installation. Use `adb install -r` only; never uninstall or clear data. No publishing is authorized or performed.

The existing APK's timestamp, size, digest and certificate are recorded in existing-apk*.txt solely to identify the old artifact. It does not contain the CODEX 34 changes and must not be described as a fresh build.
