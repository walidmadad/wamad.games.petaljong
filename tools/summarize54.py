#!/usr/bin/env python3
"""Summarize completed, interleaved JVM comparison; never invent missing rows."""
import csv,statistics,json
from pathlib import Path
folder=Path('docs/codex54')
rows=list(csv.DictReader((folder/'comparison-final.csv').open()))
assert len(rows)==40,len(rows)
old,new=rows[::2],rows[1::2]
assert [r['level'] for r in old]==[r['level'] for r in new]
assert all(r['witness']=='true' for r in rows)
for name,data in [('baseline',old),('after',new)]:
 with (folder/f'{name}.csv').open('w') as f:
  writer=csv.DictWriter(f,fieldnames=rows[0].keys(),lineterminator='\n');writer.writeheader();writer.writerows(data)
def group(level):
 return '1–7' if level<=7 else '8–15' if level<=15 else '16–30' if level<=30 else '31–50' if level<=50 else '51–100' if level<=100 else '101–200' if level<=200 else '201+'
summary=[]
text=['# Reproducible JVM comparison','',
'Production v11 vs v12. Both are evaluated using the same corrected observed-information player. Available mysteries are actually scanned one at a time, remembered, then re-revealed before collection. Covered, never observed identities are excluded from search. Scans retain exactly one active reveal; players use legal reveal/collect taps and known mystery + normal direct matches, with no free hide action or mystery + mystery shortcut. These are simulated policies, not human feedback. The retained v11 internal selection algorithm remains frozen for save compatibility; its original difficulty scores are not used as player evidence.',
'', 'Held-out tie breaks: greedy 10–21 (12 runs/puzzle), short lookahead 10–13 (4 runs, depth 3/600-node budget), stronger lookahead 10–13 (4 runs, depth 6/1800-node budget). None of these tie breaks is used for candidate acceptance. Generation uses depth 6/1200 nodes plus depth 3/600 nodes at most, reduced to 100 each for shallow-player rejects; pair-only analysis adds at most 64 nodes. No omniscient solution or construction witness is passed to a player.',
'', 'Timing: one warmup followed by interleaved old/new generation in the same JVM, fixed level order, JDK 17, 512 MiB heap, two active processors. Host timings include generation and candidate evaluation, exclude the subsequent held-out benchmark. They are not Android-device timings and can be affected by concurrent diagnostic checks.',
'', '| Levels | Puzzles | Greedy wins old → new | Short wins old → new | Strong wins old → new | Mean three-held old → new | Generation median ms old → new | New quality targets |',
'|---|---:|---:|---:|---:|---:|---:|---:|']
for band in dict.fromkeys(group(int(r['level'])) for r in old):
 a=[r for r in old if group(int(r['level']))==band];b=[r for r in new if group(int(r['level']))==band];n=len(a)
 def wins(rs,k):return sum(int(r[k]) for r in rs)
 def avg(rs,k):return statistics.mean(float(r[k]) for r in rs)
 def median(rs):return statistics.median(float(r['generationMs']) for r in rs)
 record=dict(band=band,puzzles=n,oldGreedy=wins(a,'heldoutGreedyWins'),newGreedy=wins(b,'heldoutGreedyWins'),greedyRuns=n*12,oldShort=wins(a,'heldoutShortWins'),newShort=wins(b,'heldoutShortWins'),oldStrong=wins(a,'heldoutStrongWins'),newStrong=wins(b,'heldoutStrongWins'),planningRuns=n*4,oldThree=avg(a,'threeHeld'),newThree=avg(b,'threeHeld'),oldMedianMs=median(a),newMedianMs=median(b),newTargets=sum(r['target']=='true' for r in b))
 summary.append(record)
 text.append(f"| {band} | {n} | {record['oldGreedy']}/{n*12} → {record['newGreedy']}/{n*12} | {record['oldShort']}/{n*4} → {record['newShort']}/{n*4} | {record['oldStrong']}/{n*4} → {record['newStrong']}/{n*4} | {record['oldThree']:.1f} → {record['newThree']:.1f} | {record['oldMedianMs']:.0f} → {record['newMedianMs']:.0f} | {record['newTargets']}/{n} |")
a=[r for r in old if int(r['level'])>=31];b=[r for r in new if int(r['level'])>=31]
totals=dict(expertPuzzles=len(a),oldGreedy=sum(int(r['heldoutGreedyWins']) for r in a),newGreedy=sum(int(r['heldoutGreedyWins']) for r in b),greedyRuns=len(a)*12,oldStrong=sum(int(r['heldoutStrongWins']) for r in a),newStrong=sum(int(r['heldoutStrongWins']) for r in b),planningRuns=len(a)*4,newShort=sum(int(r['heldoutShortWins']) for r in b),oldShort=sum(int(r['heldoutShortWins']) for r in a),oldThree=statistics.mean(int(r['threeHeld']) for r in a),newThree=statistics.mean(int(r['threeHeld']) for r in b),newTargetMisses=[int(r['level']) for r in b if r['target']!='true'],newMaximumMs=max(float(r['generationMs']) for r in new),oldMaximumMs=max(float(r['generationMs']) for r in old))
assert totals['newGreedy']<totals['oldGreedy'],'No greedy improvement: revise generator before claiming success.'
assert totals['newStrong']/totals['planningRuns']>totals['newGreedy']/totals['greedyRuns'],'No planning advantage.'
text += ['',f"Expert aggregate: greedy {totals['oldGreedy']}/{totals['greedyRuns']} → {totals['newGreedy']}/{totals['greedyRuns']}; stronger planning {totals['oldStrong']}/{totals['planningRuns']} → {totals['newStrong']}/{totals['planningRuns']}. New quality-target misses: {totals['newTargetMisses']}. Every returned puzzle still has a complete verified production-controller witness.",'',
'`pairs` counts matching opportunities after free legal inspection; `initialVisiblePairs` counts only initially face-up legal matches. `pairOnly` is bounded omniscient immediate-board-pair search, not four-slot solvability and not a player result. UNSOLVABLE here proves only that this restricted pair-only policy cannot finish; UNKNOWN is a timeout, never a loss proof. `traps` counts production-controller four-distinct losses among alternative collections along the verified route; `consequential` counts witness states with those losing choices and a known winning continuation. These do not prove a minimum required tray size or minimum human planning depth.', '',
'Family count uses explicit recognition families (leaves, pink flowers, yellow flowers, greenery, purple flowers, branches), excluding OTHER. The v12 palette also has narrower co-occurrence groups for maples and related leaf variants. Full CSV rows retain generation version, symbols, layout, legal moves, pressure, targets and witness status.', '',
'Limitations: 20 fixed levels, not an exhaustive distribution study. Tie breaks are reproducible variants of a few policies and are not independent human subjects. A stronger bounded policy need not outperform every shorter policy on every puzzle. Recognition difficulty is not measured by a symbolic solver. Human testing, device generation latency and Android visual verification remain outstanding.']
(folder/'benchmark-summary.md').write_text('\n'.join(text)+'\n')
(folder/'benchmark-summary.json').write_text(json.dumps(dict(bands=summary,totals=totals),indent=2)+'\n')
print(json.dumps(totals,indent=2))

performance=list(csv.DictReader((folder/'performance.csv').open()))
assert len(performance)==42,len(performance)
reference={(r['level'],r['version']):r['fingerprint'] for r in rows}
assert all(reference[(r['level'],r['version'])]==r['fingerprint'] for r in performance)
lines=['# Repeated generation timing','', 'Three repeats per puzzle/version, alternating old/new order after warmup; JDK 17, two processors, fixed 512 MiB heap. The original engine is compiled from the pinned baseline revision under a diagnostic-only package prefix; the new engine is the final production implementation. Only package/import prefixes are relocated in the baseline. This timing pass ran after the other diagnostics finished. Host wall time includes full generation and candidate analysis; these are not device timings. Every repetition has the same board fingerprint as its full benchmark row and passes the complete production witness.', '', '| Level | v11 median / max ms | v12 median / max ms | v12 candidates |','|---|---:|---:|---:|']
for level in dict.fromkeys(r['level'] for r in performance):
 a=[float(r['generationMs']) for r in performance if r['level']==level and r['version']=='11']
 b=[float(r['generationMs']) for r in performance if r['level']==level and r['version']=='12']
 candidates=next(r['candidates'] for r in performance if r['level']==level and r['version']=='12')
 lines.append(f'| {level} | {statistics.median(a):.0f} / {max(a):.0f} | {statistics.median(b):.0f} / {max(b):.0f} | {candidates} |')
lines+=['','The first qualifying candidate now stops generation after the minimum two-candidate comparison. Level 1001 previously evaluated 24 candidates and returned candidate 1 despite its existing target success; it now returns the same board after two evaluations. The retained v11 reconstruction intentionally keeps its original algorithm.','', 'Level 501 still exhausts all 24 candidates without meeting its full late-Expert quality target; it remains explicitly marked as a quality miss. Its preparation latency remains a limitation requiring measurement on an Android device. No easy unverified fallback is substituted.']
(folder/'performance-summary.md').write_text('\n'.join(lines)+'\n')
pressure=list(csv.DictReader((folder/'player-pressure.csv').open()))
assert len(pressure)==36 and all(r['controllerReplay']=='true' for r in pressure)
print('Actual player pressure:',[(s,sum(r['won']=='true' for r in pressure if r['strategy']==s),sum(r['peakTray']=='3' for r in pressure if r['strategy']==s)) for s in ['greedy','short','strong']])
