#!/usr/bin/env bash
# Run in a normal Linux terminal. Never invokes the diagnostic fallback or ADB.
set -uo pipefail

if [[ $# -ne 0 ]]; then
    printf 'Usage: %s\nRuns standard Gradle checks; see HOST_VERIFICATION.md for device testing.\n' "$0"
    exit 2
fi
project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd) || exit 1
cd -- "$project_root" || exit 1
mkdir -p app/build/host-verification || exit 1
evidence=$(mktemp -d "$project_root/app/build/host-verification/run-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX") || exit 1
printf 'step\texit_code\n' > "$evidence/results.tsv"
failed=0
gradle_status='NOT EXECUTED'
apk_status='NOT VERIFIED (any existing APK may be a diagnostic fallback)'

run() {
    local name=$1 code
    shift
    printf '\nRunning %s; log: %s/%s.log\n' "$name" "$evidence" "$name"
    printf '%q ' "$@" >> "$evidence/commands.txt"
    printf '\n' >> "$evidence/commands.txt"
    if "$@" > "$evidence/$name.log" 2>&1; then code=0; else code=$?; fi
    printf '%s\t%s\n' "$name" "$code" >> "$evidence/results.tsv"
    printf '%s exit code: %s\n' "$name" "$code"
    if [[ $code -ne 0 ]]; then
        failed=1
        tail -n 20 "$evidence/$name.log"
    fi
    return "$code"
}

finish() {
    {
        printf 'Standard Gradle tasks: %s\nAPK: %s\n' "$gradle_status" "$apk_status"
        printf 'Android installation/launch/instrumentation: NOT EXECUTED by this script\n'
        printf 'Visual review/human playtest: NOT PERFORMED by this script\n'
        printf 'Evidence directory: %s\n' "$evidence"
        if [[ $failed -eq 0 ]]; then
            printf 'Host build verification: PASS (device testing still required)\n'
        else
            printf 'Host build verification: FAILED; inspect results.tsv and logs\n'
        fi
    } | tee "$evidence/summary.txt"
    exit "$failed"
}

{
    date -u +%FT%TZ
    printf 'Project: %s\nJAVA_HOME: %s\nANDROID_HOME: %s\nANDROID_SDK_ROOT: %s\nGRADLE_USER_HOME: %s\n' \
        "$project_root" "${JAVA_HOME:-unset}" "${ANDROID_HOME:-unset}" \
        "${ANDROID_SDK_ROOT:-unset}" "${GRADLE_USER_HOME:-default}"
    if [[ -d .tilebloom-git ]]; then
        git --git-dir=.tilebloom-git --work-tree=. rev-parse HEAD
        git --git-dir=.tilebloom-git --work-tree=. status --short
    elif [[ -e .git ]]; then
        git rev-parse HEAD
        git status --short
    fi
} > "$evidence/context.txt" 2>&1

# Match the wrapper's Java selection; do not alter JAVA_HOME or PATH.
java_command=java
if [[ -n ${JAVA_HOME:-} ]]; then java_command="$JAVA_HOME/bin/java"; fi
if run java "$java_command" -version; then
    if ! run java-17 grep -Eq 'version "17([."]|$)' "$evidence/java.log"; then
        printf 'Java 17 is required. Select an installed JDK 17 and rerun.\n'
    fi
fi
run wrapper-files test -x ./gradlew && \
    run wrapper-jar test -s gradle/wrapper/gradle-wrapper.jar && \
    run wrapper-properties test -s gradle/wrapper/gradle-wrapper.properties

# Gradle prefers sdk.dir over environment variables. Support ordinary Linux SDK
# paths and Java-properties escaped spaces, colons and backslashes; never source it.
sdk=''
if [[ -f local.properties ]]; then
    while IFS= read -r line || [[ -n $line ]]; do
        line=${line%$'\r'}
        if [[ $line =~ ^[[:space:]]*sdk[.]dir[[:space:]]*=[[:space:]]*(.*)$ ]]; then
            sdk=$(printf '%s' "${BASH_REMATCH[1]}" | sed 's/\\ / /g; s/\\:/:/g; s/\\\\/\\/g')
        fi
    done < local.properties
fi
sdk=${sdk:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}
printf 'Resolved SDK: %s\n' "${sdk:-unset}" | tee -a "$evidence/context.txt"
run sdk-platform test -s "$sdk/platforms/android-36/android.jar"
build_tools="$sdk/build-tools/35.0.0"
for tool in aapt2 zipalign apksigner; do
    run "sdk-$tool" test -x "$build_tools/$tool"
done
if [[ $failed -ne 0 ]]; then
    printf 'Preflight failed. Correct Java, wrapper or SDK paths; no Gradle task was attempted.\n'
    finish
fi

# A wrapper startup failure is a prerequisite failure, so do not repeat it for
# every task. Independent task failures below are all recorded.
if ! run gradle-version ./gradlew --version; then finish; fi
run jvm-tests ./gradlew --console=plain :app:testDebugUnitTest
touch "$evidence/build-start"
build_code=0
run assemble-debug ./gradlew --console=plain :app:assembleDebug || build_code=$?
run lint-debug ./gradlew --console=plain :app:lintDebug
gradle_status='PASS'
if [[ $failed -ne 0 ]]; then gradle_status='FAILED (see per-command exit codes)'; fi

apk="$project_root/app/build/outputs/apk/debug/app-debug.apk"
if [[ $build_code -eq 0 ]]; then
    if run apk-exists test -s "$apk"; then
        apk_status="Standard Gradle output: $apk"
        run apk-fresh test "$apk" -nt "$evidence/build-start"
        run apk-timestamp stat -c '%y' "$apk"
        run apk-ad-integrations python3 tools/check_ads_apk.py "$apk"
        run apk-manifest "$build_tools/aapt2" dump xmltree "$apk" --file AndroidManifest.xml
        run apk-admob-id grep -Fq 'ca-app-pub-3940256099942544~3347511713' "$evidence/apk-manifest.log"
        run apk-size stat -c '%s bytes' "$apk"
        run apk-sha256 sha256sum "$apk"
        run apk-metadata "$build_tools/aapt2" dump badging "$apk"
        run apk-package grep -q "^package: name='com.tilebloom.game' " "$evidence/apk-metadata.log"
        run apk-min-api grep -qx "minSdkVersion:'26'" "$evidence/apk-metadata.log"
        run apk-target-api grep -qx "targetSdkVersion:'36'" "$evidence/apk-metadata.log"
        run apk-alignment "$build_tools/zipalign" -c -v 4 "$apk"
        run apk-signature "$build_tools/apksigner" verify --verbose --print-certs "$apk"
        # Preserve the artifact and reports with this invocation's evidence.
        run archive-apk cp -- "$apk" "$evidence/app-debug.apk"
    fi
else
    printf 'assembleDebug failed; any existing APK is NOT evidence of this build.\n'
fi
if [[ -d app/build/reports ]]; then
    run archive-reports cp -R -- app/build/reports "$evidence/reports"
fi
if [[ -d app/build/test-results/testDebugUnitTest ]]; then
    run archive-jvm-results cp -R -- app/build/test-results/testDebugUnitTest "$evidence/jvm-results"
fi
finish
