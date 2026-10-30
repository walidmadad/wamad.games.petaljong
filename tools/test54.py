#!/usr/bin/env python3
"""Independent cached Kotlin/JUnit run; no Gradle or Android-runtime claim."""
from pathlib import Path
import sys, tempfile
exec(compile(Path('tools/jvm54.py').read_text(),'tools/jvm54.py','exec'),globals())
OUT=Path(tempfile.mkdtemp(prefix='petaljong54-tests-')); OUT.mkdir(exist_ok=True)
classes=OUT/'classes'; classes.mkdir(exist_ok=True)
record=ROOT/'docs/codex54/jvm-classes.txt'; record.unlink(missing_ok=True)
sources=sorted((ROOT/'app/src/test/java').rglob('*.kt'))
kotlin(list((ROOT/'app/src/main/java/com/tilebloom/game/domain').glob('*.kt'))+list((ROOT/'app/src/main/java/com/tilebloom/game/presentation/board').glob('*.kt'))+sources,classes,':'.join([stdlib,annotations,junit,hamcrest]))
record.write_text(str(classes)+'\n')
print(f'Compiled JVM classes: {classes}',flush=True)
tests=[p for p in sources if p.name.endswith('Test.kt') and p.name != 'WitnessReplayTest.kt'] # helper object, not a JUnit test class
if '--focus' in sys.argv:
 tests=[p for p in tests if p.stem in {'Botanical54Test','StrategicDifficultyTest','MysteryTilesTest','MysteryOptionCTest'}]
run('java','-XX:-UsePerfData','-Xmx768m','-cp',':'.join([str(classes),stdlib,junit,hamcrest]),'org.junit.runner.JUnitCore',*['.'.join(p.relative_to(ROOT/'app/src/test/java').with_suffix('').parts) for p in tests])
