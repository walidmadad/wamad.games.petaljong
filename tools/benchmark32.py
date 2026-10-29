#!/usr/bin/env python3
"""CODEX32 host JVM matrix. Use --original to benchmark committed baseline."""
from pathlib import Path
import sys
prefix=Path('tools/verify_offline.py').read_text().split("classes = OUT")[0]
prefix=prefix[:prefix.index('# Remove stale')] + prefix[prefix.index('def jar('):]
prefix=prefix.replace("ROOT = Path(__file__).resolve().parents[1]", "ROOT = Path.cwd()")
sys.argv.append('--jvm-only')
exec(prefix)
sys.argv.remove('--jvm-only')
out=Path('/tmp/tilebloom32-original' if '--original' in sys.argv else '/tmp/tilebloom32-benchmark'); out.mkdir(exist_ok=True)
source=out/'Baseline.kt'
source.write_text('import com.tilebloom.game.domain.*\nfun main() { repeat(3) { run -> for(id in listOf(8L,16,30,31,51,101,501,1001)) {\n val (l,p)=PreparationMetrics.measure { EndlessGenerator.generate(id) }\n println("run=$run level=$id tier=${DifficultyPolicy.difficulty(id)} layout=${l.layout.id} attempts=${l.attempts} candidates=${l.candidatesEvaluated} nodes=${l.candidateQuality.sumOf { it.searchNodes }} fallback=${l.attempts==0} phasesMs=${p.mapValues { it.value/1000000.0 }}")\n} } }\n')
sources = list((ROOT/'app/src/main/java/com/tilebloom/game/domain').glob('*.kt'))
if '--original' in sys.argv:
    import subprocess
    original = out/'original'; original.mkdir(exist_ok=True)
    frozen = []
    tracked = set(subprocess.check_output(['git','--git-dir=.tilebloom-git','ls-tree','-r','--name-only','HEAD'], text=True).splitlines())
    for path in sources:
        if str(path.relative_to(ROOT)) not in tracked:
            continue
        target = original/path.name
        if path.name in ('PreparationMetrics.kt', 'LevelPreparation.kt'):
            target.write_text(path.read_text())
        else:
            target.write_text(subprocess.check_output(['git','--git-dir=.tilebloom-git','--work-tree=.','show',f'HEAD:{path.relative_to(ROOT)}'], text=True))
        frozen.append(target)
    sources = frozen
sources += list((ROOT/'app/src/main/java/com/tilebloom/game/presentation/board').glob('*.kt'))
kotlin(sources+[source],out,':'.join([stdlib,annotations]))
run('java','-cp',f'{out}:{stdlib}', 'BaselineKt', *sys.argv[1:])
