#!/usr/bin/env python3
"""Isolated cached-compiler JVM regression run; never deletes another run's classes."""
from pathlib import Path
import sys, tempfile
prefix=Path('tools/verify_offline.py').read_text().split("classes = OUT")[0]
prefix=prefix[:prefix.index('# Remove stale')] + prefix[prefix.index('def jar('):]
prefix=prefix.replace("ROOT = Path(__file__).resolve().parents[1]", "ROOT = Path.cwd()")
sys.argv.append('--jvm-only')
exec(prefix)
sys.argv.remove('--jvm-only')
out=Path(tempfile.mkdtemp(prefix='tilebloom37-tests-'))
tests=sorted((ROOT/'app/src/test/java').rglob('*Test.kt'))
sources=list((ROOT/'app/src/main/java/com/tilebloom/game/domain').glob('*.kt'))
sources+=list((ROOT/'app/src/main/java/com/tilebloom/game/presentation/board').glob('*.kt'))
kotlin(sources+tests,out,':'.join([stdlib,annotations,junit,hamcrest]))
runnable=[p for p in tests if p.stem != 'WitnessReplayTest']
selected=runnable if '--focused' not in sys.argv else [p for p in tests if p.stem in ('MysteryTilesTest','StrategicDifficultyTest')]
if '--classes' in sys.argv:
    names=sys.argv[sys.argv.index('--classes')+1].split(',')
    selected=[p for p in runnable if p.stem in names]
print(f'Isolated classes: {out}',flush=True)
run('java','-XX:-UsePerfData','-Xmx512m','-cp',':'.join([str(out),stdlib,junit,hamcrest]),'org.junit.runner.JUnitCore',
    *['.'.join(p.relative_to(ROOT/'app/src/test/java').with_suffix('').parts) for p in selected])
