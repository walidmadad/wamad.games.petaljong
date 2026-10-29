"""Summarize measured host JVM samples; not device results."""
from pathlib import Path
import re, statistics, math, sys
root = Path('docs/codex32')
rows = {}
files = [('Before','serial-original-jvm.txt'),('After','serial-optimized-jvm.txt')] if '--serial' in sys.argv else [('Before','baseline-phases-jvm.txt'),('After','final-phases-jvm.txt')]
if '--isolated' in sys.argv: files = [('Before','isolated-original-jvm.txt'),('After','isolated-optimized-jvm.txt')]
for label, name in files:
    samples = {}
    for line in (root/name).read_text().splitlines():
        m = re.search(r'level=(\d+).*total=([\d.]+)', line)
        if m: samples.setdefault(int(m[1]), []).append(float(m[2]))
    rows[label] = samples
print('Host JVM milliseconds; three samples per target, no Android performance claim. p95 = nearest rank.\n')
print('| Target | Before median / p95 / max | After median / p95 / max |')
print('|---|---|---|')
for level in rows['Before']:
    values=[]
    for label in rows:
        v=sorted(rows[label].get(level, []))
        values.append(' / '.join(f'{x:.1f}' for x in (statistics.median(v),v[math.ceil(.95*len(v))-1],max(v))) if v else 'pending')
    print(f'| {level} | {values[0]} | {values[1]} |')
for label, samples in rows.items():
    v=sorted(x for items in samples.values() for x in items)
    if v: print(f'\n{label}: n={len(v)}, median={statistics.median(v):.1f}, p95={v[math.ceil(.95*len(v))-1]:.1f}, max={max(v):.1f}')
