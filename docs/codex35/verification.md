# Verification boundaries

The default host Java 25 cannot run the cached Kotlin 1.9.24 compiler. Independent compilation/tests use installed Java 17 and cached dependencies only. No dependencies were downloaded and no mock AdMob replacement was put in the production build.

- Requested Gradle unit/assemble/lint: attempted individually, each failed before task execution on the read-only wrapper lock (`gradle-unit.txt`, `gradle-assemble.txt`, `gradle-lint.txt`).
- Direct installed Gradle 8.13 + Java 17 + writable `/tmp/tilebloom35-gradle`, offline/no daemon: failed before tasks because the lock service cannot determine/open a wildcard IP (`gradle-cached.txt`).
- Independent main + instrumentation Kotlin sources and Android resources: PASS (`android-final.txt`). Recipe: `android-compile-recipe.py`, which uses the cached dependency jars from the earlier diagnostic compilation directory. This is not Gradle, Lint, executed instrumentation, or an APK build.
- Focused mystery/strategic tests: 21 PASS (`tests-focused.txt`). First full run: 269/273 PASS; four old Hint/navigation tests omitted the new reveal transition (`tests-full-first.txt`). Corrected affected classes: all 15 PASS (`tests-corrected-regressions.txt`). **Final full rerun: 273/273 PASS in 153.536 seconds** (`tests-final.txt`).
- All 23 final benchmark boards passed production full-action witness verification (`benchmark-final.csv`).
- Six historical v9 seed/geometry/symbol/witness SHA-256 fingerprints match the actual prior compiled implementation (`v9-old-fingerprints.txt`, `v9-new-fingerprints.txt`). No hash was generated from a claimed but untested baseline.
- ADB: listener startup forbidden (`adb.txt`). No installation, instrumentation or Android screenshot capture was performed.
- APK: the pre-existing artifact is identified separately in `existing-apk.txt`; its matching certificate, package and alignment checks do not certify this source revision. No new APK, new dex contents or actual Android animation behavior can be verified in this environment.

Reproduce JVM checks with:

```sh
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH JAVA_TOOL_OPTIONS=-XX:-UsePerfData python3 tools/test35.py
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH JAVA_TOOL_OPTIONS=-XX:-UsePerfData python3 tools/benchmark35.py
```

A normal host should run the three requested standard Gradle tasks and the isolated Android fixture protocol before distributing a CODEX 35 APK. No player data or signing files should be removed or replaced. Existing advertising SDK, consent, cooldown, network and reward implementations were not edited.
