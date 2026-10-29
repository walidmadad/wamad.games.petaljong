#!/usr/bin/env python3
"""Summarize recorded measurements only; fail if the complete matrix is absent."""
import csv, statistics
from pathlib import Path
path=Path('docs/codex34/benchmark.csv')
rows=list(csv.DictReader(path.open()))
expected={*range(1,8),8,12,15,16,23,30,31,40,50,51,75,100,101,501,1001}
assert len(rows)==len(expected)*2
assert {(int(r['version']),int(r['level'])) for r in rows}=={(v,i) for v in (8,9) for i in expected}
assert all(r['witness']=='true' and int(r['nodes'])<=1800 and int(r['searchNodes'])<=1000 for r in rows)
def rate(n,d): return f'{n}/{d} ({100*n/d:.1f}%)' if d else '—'
lines=['# CODEX 34 measured benchmark','',
'Java 17, Kotlin 1.9.24, fixed levels/seeds; frozen v8 and current v9 in one JVM after warming both paths. Generation is timed before separate analysis and holdout probes. Each lookahead playthrough has at most 1,800 expanded states and depth six. Depth-one results use the same policy without future search. Full-state continuation sampling has a shared 1,000-node budget per puzzle. Four independent ID relabelings test tie-break generalization on levels 16+; they were not used for selection. These are puzzle design proxies, not human difficulty measurements.','',
'| Tier | Version | Boards | Greedy wins | Simple wins | Lookahead wins | Depth-one wins |',
'|---|---:|---:|---:|---:|---:|---:|']
for tier in ('NORMAL','HARD','VERY_HARD','EXPERT'):
 for version in ('8','9'):
  rs=[r for r in rows if r['tier']==tier and r['version']==version]
  lines.append('| '+ ' | '.join([tier,version,str(len(rs)),*[rate(sum(r[k]=='true' for r in rs),len(rs)) for k in ('greedy','simple','lookahead','depth1Wins')]])+' |')
lines+=['','| Holdout tier | Version | Greedy wins | Simple wins | Lookahead wins |','|---|---:|---:|---:|---:|']
for tier in ('VERY_HARD','EXPERT'):
 for version in ('8','9'):
  rs=[r for r in rows if r['tier']==tier and r['version']==version]
  runs=sum(int(r['holdoutRuns']) for r in rs)
  lines.append('| '+' | '.join([tier,version,*[rate(sum(int(r[k]) for r in rs),runs) for k in ('holdoutGreedy','holdoutSimple','holdoutLookahead')]])+' |')
lines+=['','## Generation and analyzer cost','',
'These host timings were measured during other verification work and include CPU contention. They are not Android latency measurements. Levels 1–5 share unchanged, warmed cached fixtures; exclude them from cold timing comparisons. No artificial delay is added.','',
'| Tier (levels 6+) | Version | Median generation ms | Maximum generation ms | Median independent analysis ms |','|---|---:|---:|---:|---:|']
for tier in ('NORMAL','HARD','VERY_HARD','EXPERT'):
 for version in ('8','9'):
  rs=[r for r in rows if r['tier']==tier and r['version']==version and int(r['level'])>=6]
  lines.append(f'| {tier} | {version} | {statistics.median(float(r["ms"]) for r in rs):.1f} | {max(float(r["ms"]) for r in rs):.1f} | {statistics.median(float(r["analysisMs"]) for r in rs):.1f} |')
v9=[r for r in rows if r['version']=='9']
expert=[r for r in v9 if r['tier']=='EXPERT']
old=[r for r in rows if r['version']=='8']
limited=[r['level'] for r in v9 if r['target']=='false']
lines+=['','## Verification and interpretation','',
 f'- All {len(rows)} original sampled boards and all {sum(int(r["holdoutRuns"]) for r in rows)} relabeled holdout witnesses passed production-controller verification. No witness overflowed.',
 f'- v9 boards missing the strategic profile: {", ".join(limited) or "none in this sample"}. Historical v8 target flags use its old geometry/pair metrics and are not comparable strategic certifications.',
 f'- v8 witness peak occupancy is always one. v9 Expert witness peak occupancy is {min(int(r["peak"]) for r in expert)}–{max(int(r["peak"]) for r in expert)}, delayed matches {min(int(r["delayed"]) for r in expert)}–{max(int(r["delayed"]) for r in expert)} per board. This describes a certified route, not minimum required pressure.',
 f'- Maximum expanded lookahead states: {max(int(r["nodes"]) for r in rows)} / 1,800. Maximum full-state search expansions: {max(int(r["searchNodes"]) for r in rows)} / 1,000.',
 f'- Sampled continuation totals v8: safe {sum(int(r["safe"]) for r in old)}, losing {sum(int(r["losing"]) for r in old)}, unknown {sum(int(r["unknown"]) for r in old)}; v9: safe {sum(int(r["safe"]) for r in v9)}, losing {sum(int(r["losing"]) for r in v9)}, unknown {sum(int(r["unknown"]) for r in v9)}. These sample different certified routes and are not exhaustive loss probabilities. Unknown is never counted as losing.',
 f'- Distinct scheduled advanced families in sampled v9 boards: {len({r["family"].split("-")[0] for r in v9 if int(r["level"])>=6})}. Novelty-limited sampled v9 boards: {sum(r["noveltyLimited"]=="true" for r in v9)}.',
 '- Holdouts expose residual shallow wins and selection-policy overfitting. Expert generalization improves, but several boards remain easy for some tie breaks. Very Hard holdout separation is weaker and needs more tuning/playtest evidence.',
 '- Visibility-restricted lookahead cannot use a newly uncovered identity until a real observation. This reduces hidden-information peeking; it does not prove every loss is fair or every choice is inferable to a human.',
 '- The broader JVM matrix contains additional profile misses; this is not a claim that every Expert level meets the target. Human playtest and device performance remain unverified.',
 '', '[Per-puzzle measurements](benchmark.csv) · [Playtest protocol](playtest.md) · [Build limitations](verification.md)','']
Path('docs/codex34/benchmark-summary.md').write_text('\n'.join(lines))
