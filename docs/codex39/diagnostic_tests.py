import sys
sys.argv.append('--jvm-only')
from pathlib import Path
prefix=Path('tools/verify_offline.py').read_text().split("classes = OUT / 'classes'")[0]
# Do not execute the fallback's artifact cleanup.
prefix=prefix[:prefix.index('# Remove stale')] + prefix[prefix.index('def jar('):]
__file__ = str(Path('tools/verify_offline.py').resolve())
exec(compile(prefix, 'tools/verify_offline.py', 'exec'), globals())


classes=ROOT/'app/build/offline/test-classes'
tests=sorted((ROOT/'app/src/test/java').rglob('*Test.kt'))
names=['.'.join(p.relative_to(ROOT/'app/src/test/java').with_suffix('').parts) for p in tests if (classes/p.relative_to(ROOT/'app/src/test/java').with_suffix('.class')).exists()]
run('/usr/lib/jvm/java-17-openjdk-amd64/bin/java','-cp',':'.join([str(classes),stdlib,junit,hamcrest]),'org.junit.runner.JUnitCore',*names)
