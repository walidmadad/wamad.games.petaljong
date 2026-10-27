#!/usr/bin/env python3
"""Workflow tests with fake tools in temporary projects; NOT Android verification."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class HostVerifyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='tilebloom-workflow-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'project with spaces'
        (self.root / 'tools').mkdir(parents=True)
        shutil.copy2(Path(__file__).with_name('host_verify.sh'), self.root / 'tools/host_verify.sh')
        self.sdk = Path(self.temp.name) / 'sdk with spaces'
        platform = self.sdk / 'platforms/android-36/android.jar'
        platform.parent.mkdir(parents=True)
        platform.write_text('fixture only')
        self.bin = Path(self.temp.name) / 'bin'
        self.bin.mkdir()
        self.executable(self.bin / 'java', 'echo \'openjdk version "17.0.1"\' >&2')
        self.executable(self.root / 'gradlew', '''
case "$*" in
  *--version*) exit "${VERSION_EXIT:-0}" ;;
  *testDebugUnitTest*) exit "${UNIT_EXIT:-0}" ;;
  *assembleDebug*)
    if [[ ${BUILD_EXIT:-0} != 0 ]]; then exit "$BUILD_EXIT"; fi
    if [[ ${MISSING_APK:-0} == 0 ]]; then
      mkdir -p app/build/outputs/apk/debug
      echo 'fixture only, not an APK' > app/build/outputs/apk/debug/app-debug.apk
    fi ;;
  *lintDebug*) exit "${LINT_EXIT:-0}" ;;
  *) exit 99 ;;
esac
''')
        wrapper = self.root / 'gradle/wrapper'
        wrapper.mkdir(parents=True)
        (wrapper / 'gradle-wrapper.jar').write_text('fixture only')
        (wrapper / 'gradle-wrapper.properties').write_text('fixture only')
        bt = self.sdk / 'build-tools/35.0.0'
        self.executable(bt / 'aapt2', '''cat <<'META'
package: name='com.tilebloom.game' versionCode='1'
minSdkVersion:'26'
targetSdkVersion:'36'
META
''')
        self.executable(bt / 'zipalign', 'exit 0')
        self.executable(bt / 'apksigner', 'exit "${SIGN_EXIT:-0}"')
        self.env = dict(os.environ, PATH=f'{self.bin}:{os.environ["PATH"]}',
                        ANDROID_HOME=str(self.sdk))
        for key in ('JAVA_HOME', 'ANDROID_SDK_ROOT', 'VERSION_EXIT', 'UNIT_EXIT',
                    'BUILD_EXIT', 'LINT_EXIT', 'SIGN_EXIT', 'MISSING_APK'):
            self.env.pop(key, None)

    @staticmethod
    def executable(path, body):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('#!/usr/bin/env bash\n' + body + '\n')
        path.chmod(0o755)

    def verify(self, **settings):
        result = subprocess.run(['bash', str(self.root / 'tools/host_verify.sh')],
                                cwd=self.temp.name, env=dict(self.env, **settings),
                                capture_output=True, text=True, timeout=20)
        runs = list((self.root / 'app/build/host-verification').glob('run-*'))
        self.assertEqual(1, len(runs))
        self.evidence = runs[0]
        self.results = (self.evidence / 'results.tsv').read_text()
        return result

    def assert_failed(self, result):
        self.assertNotEqual(0, result.returncode)
        self.assertIn('Host build verification: FAILED', result.stdout)
        self.assertNotIn('Host build verification: PASS', result.stdout)

    def test_success_preserves_evidence_and_keeps_runtime_unexecuted(self):
        result = self.verify()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertTrue((self.evidence / 'app-debug.apk').exists())
        self.assertIn('jvm-tests\t0', self.results)
        self.assertIn('lint-debug\t0', self.results)
        self.assertIn('NOT EXECUTED by this script', result.stdout)

    def test_java_preflight_failure_stops_before_gradle(self):
        self.executable(self.bin / 'java', 'echo \'openjdk version "21.0.1"\' >&2')
        result = self.verify()
        self.assert_failed(result)
        self.assertNotIn('gradle-version', self.results)

    def test_missing_sdk_stops_before_gradle(self):
        result = self.verify(ANDROID_HOME=str(self.sdk / 'missing'))
        self.assert_failed(result)
        self.assertNotIn('gradle-version', self.results)

    def test_local_properties_precedes_environment_and_is_not_executed(self):
        sdk = str(self.sdk).replace(' ', '\\ ')
        (self.root / 'local.properties').write_text(
            '# normal SDK path\nsdk.dir=' + sdk + '\nmalicious=$(touch SHOULD_NOT_EXIST)\n')
        result = self.verify(ANDROID_HOME='/nonexistent')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertFalse((self.root / 'SHOULD_NOT_EXIST').exists())

    def test_wrapper_failure_does_not_repeat_startup_for_tasks(self):
        result = self.verify(VERSION_EXIT='23')
        self.assert_failed(result)
        self.assertIn('gradle-version\t23', self.results)
        self.assertNotIn('jvm-tests', self.results)

    def test_unit_failure_does_not_hide_other_task_results(self):
        result = self.verify(UNIT_EXIT='17')
        self.assert_failed(result)
        self.assertIn('jvm-tests\t17', self.results)
        self.assertIn('assemble-debug\t0', self.results)
        self.assertIn('lint-debug\t0', self.results)

    def test_failed_build_does_not_accept_existing_fallback_apk(self):
        stale = self.root / 'app/build/outputs/apk/debug/app-debug.apk'
        stale.parent.mkdir(parents=True)
        stale.write_text('old diagnostic fallback')
        result = self.verify(BUILD_EXIT='19')
        self.assert_failed(result)
        self.assertEqual('old diagnostic fallback', stale.read_text())
        self.assertFalse((self.evidence / 'app-debug.apk').exists())
        self.assertNotIn('Standard Gradle output:', result.stdout)

    def test_successful_command_without_apk_is_failure(self):
        result = self.verify(MISSING_APK='1')
        self.assert_failed(result)
        self.assertIn('apk-exists\t1', self.results)

    def test_lint_failure_is_not_reported_as_pass(self):
        result = self.verify(LINT_EXIT='11')
        self.assert_failed(result)
        self.assertIn('lint-debug\t11', self.results)

    def test_signature_failure_is_not_reported_as_pass(self):
        result = self.verify(SIGN_EXIT='13')
        self.assert_failed(result)
        self.assertIn('apk-signature\t13', self.results)


if __name__ == '__main__':
    unittest.main()
