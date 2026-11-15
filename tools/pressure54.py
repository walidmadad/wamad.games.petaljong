#!/usr/bin/env python3
"""Record actual observed-information player tray occupancy, not just witness occupancy."""
from pathlib import Path
import tempfile,sys
exec(compile(Path('tools/jvm54.py').read_text(),'tools/jvm54.py','exec'),globals())
out=Path(tempfile.mkdtemp(prefix='petaljong54-pressure-'))
source=out/'Pressure.kt'
source.write_text('''import com.wamad.petaljong.domain.*
fun main(){
 println("level,strategy,won,peakTray,pressureStates,nodes,controllerReplay")
 for(id in listOf(31L,40,50,51,75,100,101,150,200,201,501,1001)) {
 val l=Levels.get(id)
 for(strategy in listOf("greedy","short","strong")) {
 val r=PlanningPlayer.play(l,if(strategy=="greedy") SimulatedPlayer.Strategy.GREEDY else SimulatedPlayer.Strategy.LOOKAHEAD,if(strategy=="short") 600 else 1800,if(strategy=="short") 3 else 6,10)
 val g=Game(l.tiles);check(g.restore(r.snapshot!!,true));check((g.status==Status.WON)==r.won)
 println("$id,$strategy,${r.won},${r.peakTray},${r.pressureStates},${r.nodes},true")
 }
 }
}''')
base=None
if '--classes' in sys.argv:base=Path(sys.argv[sys.argv.index('--classes')+1])
sources=[source] if base else list((ROOT/'app/src/main/java/com/wamad/petaljong/domain').glob('*.kt'))+list((ROOT/'app/src/main/java/com/wamad/petaljong/presentation/board').glob('*.kt'))+[source]
cp=':'.join([stdlib,annotations]+([str(base)] if base else []))
kotlin(sources,out,cp)
run('java','-cp',':'.join([str(out),stdlib]+([str(base)] if base else [])),'PressureKt')
