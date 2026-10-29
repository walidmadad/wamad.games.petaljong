import sys
sys.argv.append('--jvm-only')
from pathlib import Path
prefix=Path('tools/verify_offline.py').read_text().split("classes = OUT / 'classes'")[0]
# Do not execute the fallback's artifact cleanup.
prefix=prefix[:prefix.index('# Remove stale')] + prefix[prefix.index('def jar('):]
__file__ = str(Path('tools/verify_offline.py').resolve())
exec(compile(prefix, 'tools/verify_offline.py', 'exec'), globals())

OUT=Path('/tmp/tilebloom48-compile'); OUT.mkdir(exist_ok=True)
classes=OUT/'classes'; classes.mkdir(exist_ok=True)
res=OUT/'resources.zip'
run(bt/'aapt2','compile','--dir','app/src/main/res','-o',res)
manifest=OUT/'AndroidManifest.xml'
manifest.write_text((ROOT/'app/src/main/AndroidManifest.xml').read_text().replace('<manifest ', '<manifest package="com.tilebloom.game" ').replace('${admobAppId}', 'ca-app-pub-3940256099942544~3347511713'))
generated=OUT/'generated'; generated.mkdir(exist_ok=True)
run(bt/'aapt2','link','-o',OUT/'resources.apk','-I',android,'--manifest',manifest,'--java',generated,'--min-sdk-version','26','--target-sdk-version','36',res)
run('javac','-source','17','-target','17','-classpath',android,'-d',classes,*generated.rglob('*.java'))
config=OUT/'BuildConfig.kt'
config.write_text('package com.tilebloom.game\nobject BuildConfig { const val DEBUG = true; const val INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712"; const val REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917" }')
deps=list(Path('/tmp/tilebloom24-compile').glob('*.jar'))
cp=':'.join(map(str,[android,classes,stdlib,annotations,SDK/'platforms/android-36/optional/android.test.runner.jar',SDK/'platforms/android-36/optional/android.test.base.jar']+deps))
sources=list((ROOT/'app/src/main/java').rglob('*.kt'))+list((ROOT/'app/src/androidTest/java').rglob('*.kt'))+[config]
run('java','-cp',compiler,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-Xskip-metadata-version-check','-no-stdlib','-no-reflect','-jvm-target','17','-classpath',cp,'-d',classes,*sources)
print('Diagnostic main + Android test compilation PASS; not a Gradle build or executed instrumentation.')
