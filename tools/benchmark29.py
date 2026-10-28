#!/usr/bin/env python3
"""JVM baseline: pass `historical` for frozen v6, omit for v7. Uses cached compiler."""
from pathlib import Path
import sys
prefix=Path('tools/verify_offline.py').read_text().split("classes = OUT")[0]
prefix=prefix[:prefix.index('# Remove stale')] + prefix[prefix.index('def jar('):]
prefix=prefix.replace("ROOT = Path(__file__).resolve().parents[1]", "ROOT = Path.cwd()")
sys.argv.append('--jvm-only')
exec(prefix)
sys.argv.remove('--jvm-only')
out=Path('/tmp/tilebloom29-benchmark'); out.mkdir(exist_ok=True)
source=out/'Baseline.kt'
source.write_text('''import com.tilebloom.game.domain.*
fun main(args: Array<String>) { for(id in listOf(1L,7,8,15,16,30,31,50,100,500,1000)) { val t=System.nanoTime(); val l=if(id < 6 || args.isEmpty()) Levels.get(id) else VersionSixGenerator.generate(id); val generationMs=(System.nanoTime()-t)/1000000; val at=System.nanoTime(); val a=PuzzleAnalyzer.analyze(l,2000); val analysisMs=(System.nanoTime()-at)/1000000; val overlaps=l.tiles.sumOf { x -> l.tiles.count { it.layer>x.layer && Rules.overlaps(x,it) } }; println("$id tiles=${a.tileCount} layers=${a.layers} overlap=$overlaps blocked=${a.initialBlocked} free=${a.initialFree} choices=${a.consequenceChoiceStates} risk=${a.riskyChoiceStates} unknown=${a.unknownAlternatives} generationMs=$generationMs analysisMs=$analysisMs ms=${(System.nanoTime()-t)/1000000}") } }
''')
kotlin(list((ROOT/'app/src/main/java/com/tilebloom/game/domain').glob('*.kt'))+[source],out,':'.join([stdlib,annotations]))
run('java','-cp',f'{out}:{stdlib}', 'BaselineKt', *sys.argv[1:])
