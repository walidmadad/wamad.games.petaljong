"""Build only the unchanged CeramicArtworkVisualTest as an auxiliary test APK.

Requires fresh diagnostic compilation under /tmp/tilebloom47-compile. This is not
the standard Gradle Android test build and does not replace the application APK.
"""
import os
import subprocess
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SDK=Path.home()/'Android/Sdk'
BT=SDK/'build-tools/35.0.0'
OUT=ROOT/'app/build/codex47-visual-test'
OUT.mkdir(parents=True,exist_ok=True)

def run(*args,**kwargs):
    subprocess.run(list(map(str,args)),check=True,**kwargs)

classes=Path('/tmp/tilebloom47-compile/classes')
selected=list(classes.rglob('CeramicArtworkVisualTest*.class'))
assert selected
testjar=OUT/'test.jar'
with zipfile.ZipFile(testjar,'w') as target:
    for path in selected:
        target.write(path,str(path.relative_to(classes)))
manifest=OUT/'AndroidManifest.xml'
manifest.write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.tilebloom.game.artworktest">
<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/>
<instrumentation android:name="android.test.InstrumentationTestRunner" android:targetPackage="com.tilebloom.game"/>
<application android:debuggable="true" android:label="TileBloom artwork verification"><uses-library android:name="android.test.runner"/></application>
</manifest>''')
unsigned=OUT/'unsigned.apk'
run(BT/'aapt2','link','-o',unsigned,'-I',SDK/'platforms/android-36/android.jar','--manifest',manifest)
dex=OUT/'dex'; dex.mkdir(exist_ok=True)
run(BT/'d8','--min-api','26','--lib',SDK/'platforms/android-36/android.jar',
    '--lib',SDK/'platforms/android-36/optional/android.test.runner.jar',
    '--lib',SDK/'platforms/android-36/optional/android.test.base.jar','--output',dex,testjar)
with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as apk:
    for path in dex.glob('*.dex'):
        apk.write(path,path.name)
aligned=OUT/'aligned.apk'
run(BT/'zipalign','-f','4',unsigned,aligned)
props={}
for line in (ROOT/'signing.local.properties').read_text().splitlines():
    if line.strip() and not line.lstrip().startswith('#') and '=' in line:
        k,v=line.split('=',1);props[k.strip()]=v.strip()
env=os.environ.copy()
env['TILEBLOOM47_STORE_PASS']=props['storePassword']
env['TILEBLOOM47_KEY_PASS']=props['keyPassword']
apk=OUT/'artwork-test.apk'
run(BT/'apksigner','sign','--ks',ROOT/props['storeFile'],'--ks-key-alias',props['keyAlias'],
    '--ks-pass','env:TILEBLOOM47_STORE_PASS','--key-pass','env:TILEBLOOM47_KEY_PASS',
    '--out',apk,aligned,env=env)
run(BT/'apksigner','verify',apk)
print('Auxiliary visual-test APK:',apk)
