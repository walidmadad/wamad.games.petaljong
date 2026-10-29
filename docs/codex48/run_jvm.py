import sys
sys.argv.append('--jvm-only')
from pathlib import Path
prefix=Path('tools/verify_offline.py').read_text().split("classes = OUT / 'classes'")[0]
# Do not execute the fallback's artifact cleanup.
prefix=prefix[:prefix.index('# Remove stale')] + prefix[prefix.index('def jar('):]
__file__ = str(Path('tools/verify_offline.py').resolve())
exec(compile(prefix, 'tools/verify_offline.py', 'exec'), globals())


OUT=Path('/tmp/tilebloom48-jvm'); OUT.mkdir(exist_ok=True)
classes=OUT/'classes'; classes.mkdir(exist_ok=True)
sources=list((ROOT/'app/src/main/java/com/tilebloom/game/domain').glob('*.kt'))+list((ROOT/'app/src/main/java/com/tilebloom/game/presentation/board').glob('*.kt'))+list((ROOT/'app/src/test/java').rglob('*.kt'))
kotlin(sources, classes, ':'.join([stdlib, annotations, junit, hamcrest]))
tests=sorted('.'.join(p.relative_to(classes).with_suffix('').parts) for p in classes.rglob('*Test.class') if '$' not in p.name)
run('java','-Xmx1200m','-cp',':'.join(map(str,[classes,stdlib,junit,hamcrest])),'org.junit.runner.JUnitCore',*tests)
