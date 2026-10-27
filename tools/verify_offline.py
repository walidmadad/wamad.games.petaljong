#!/usr/bin/env python3
"""Restricted-environment fallback, independent of Gradle. Requires cached Kotlin 1.9.24.
Builds a genuine debug APK and runs production engine tests using JUnitCore.
The standard supported build remains ./gradlew; this does not run Android Lint.
"""
from pathlib import Path
import glob, os, subprocess, zipfile
ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
SDK = Path(os.environ.get('ANDROID_HOME', '/home/walid/Android/Sdk'))
CACHE = Path.home() / '.gradle/caches/modules-2/files-2.1'
OUT = ROOT / 'app/build/offline'
OUT.mkdir(parents=True, exist_ok=True)
def jar(group, artifact, version):
    return glob.glob(str(CACHE / group / artifact / version / '*' / '*.jar'))[0]
def run(*args):
    subprocess.run([str(a) for a in args], check=True)
stdlib = jar('org.jetbrains.kotlin','kotlin-stdlib','1.9.24')
annotations = jar('org.jetbrains','annotations','13.0')
compiler = ':'.join([jar('org.jetbrains.kotlin','kotlin-compiler-embeddable','1.9.24'), stdlib,
    jar('org.jetbrains.kotlin','kotlin-reflect','1.6.10'), jar('org.jetbrains.intellij.deps','trove4j','1.0.20200330'), annotations])
junit = jar('junit','junit','4.13.2')
hamcrest = jar('org.hamcrest','hamcrest-core','1.3')
android = SDK / 'platforms/android-36/android.jar'
bt = SDK / 'build-tools/35.0.0'
def kotlin(sources, destination, cp):
    run('java','-cp',compiler,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect',
        '-jvm-target','17','-classpath',cp,'-d',destination,*sources)
classes = OUT / 'classes'; classes.mkdir(exist_ok=True)
testclasses = OUT / 'test-classes'; testclasses.mkdir(exist_ok=True)
kotlin(list((ROOT/'app/src/main/java/com/tilebloom/game/domain').glob('*.kt')) + list((ROOT/'app/src/test/java').rglob('*.kt')),
       testclasses, ':'.join([stdlib, annotations, junit, hamcrest]))
run('java','-cp',':'.join([str(testclasses),stdlib,junit,hamcrest]),'org.junit.runner.JUnitCore','com.tilebloom.game.GameTest')
res = OUT / 'resources.zip'
run(bt/'aapt2','compile','--dir','app/src/main/res','-o',res)
manifest = OUT / 'AndroidManifest.xml'
manifest.write_text((ROOT/'app/src/main/AndroidManifest.xml').read_text().replace('<manifest ', '<manifest package="com.tilebloom.game" ').replace('<application ', '<application android:debuggable="true" '))
generated = OUT / 'generated'; generated.mkdir(exist_ok=True)
unsigned = OUT / 'unsigned.apk'
run(bt/'aapt2','link','-o',unsigned,'-I',android,'--manifest',manifest,'--java',generated,'--min-sdk-version','26',
    '--target-sdk-version','36','--version-code','1','--version-name','0.1',res)
run('javac','-source','17','-target','17','-classpath',android,'-d',classes,*generated.rglob('*.java'))
kotlin(list((ROOT/'app/src/main/java').rglob('*.kt')), classes, ':'.join([str(android),str(classes),stdlib,annotations]))
instrumentation = OUT / 'instrumentation-classes'; instrumentation.mkdir(exist_ok=True)
kotlin(list((ROOT/'app/src/androidTest/java').rglob('*.kt')), instrumentation,
       ':'.join([str(android), str(classes), stdlib, annotations,
                 str(SDK/'platforms/android-36/optional/android.test.runner.jar'),
                 str(SDK/'platforms/android-36/optional/android.test.base.jar')]))
print('Instrumentation smoke test compiled; NOT executed.', flush=True)
bytecode = OUT / 'app.jar'
run('jar','cf',bytecode,'-C',classes,'.')
dex = OUT / 'dex'; dex.mkdir(exist_ok=True)
run(bt/'d8','--min-api','26','--lib',android,'--output',dex,bytecode,stdlib,annotations)
with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as apk:
    for d in dex.glob('*.dex'): apk.write(d,d.name)
aligned = OUT / 'aligned.apk'
run(bt/'zipalign','-f','4',unsigned,aligned)
key = OUT / 'debug.keystore'
if not key.exists():
    run('keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','androiddebugkey',
        '-dname','CN=Android Debug,O=Android,C=US','-keyalg','RSA','-validity','10000')
apk = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
apk.parent.mkdir(parents=True,exist_ok=True)
run(bt/'apksigner','sign','--ks',key,'--ks-pass','pass:android','--out',apk,aligned)
run(bt/'apksigner','verify','--verbose',apk)
print(f'APK: {apk} ({apk.stat().st_size} bytes)')
