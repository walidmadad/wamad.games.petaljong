from pathlib import Path
import sys
prefix=Path('tools/verify_offline.py').read_text().split("classes = OUT")[0]
prefix=prefix[:prefix.index('# Remove stale')] + prefix[prefix.index('def jar('):]
prefix=prefix.replace("ROOT = Path(__file__).resolve().parents[1]", "ROOT = Path.cwd()")
sys.argv.append('--jvm-only')
exec(prefix)
classes=Path('/tmp/tilebloom36-tests-gd8vzd2i')
cp=':'.join([str(classes),stdlib,annotations,junit,hamcrest])
tests=sorted((ROOT/'app/src/test/java').rglob('*Test.kt'))
run('java','-XX:-UsePerfData','-Xmx512m','-cp',cp,'org.junit.runner.JUnitCore',*['.'.join(p.relative_to(ROOT/'app/src/test/java').with_suffix('').parts) for p in tests if p.stem!='WitnessReplayTest'])
