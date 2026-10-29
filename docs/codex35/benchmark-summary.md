# CODEX 35 benchmark

Measured in the same host Java 17 JVM after warming level 8. Cold generation is timed independently of the subsequent full strategic reanalysis. Concurrent verification was running, so these are host observations, not isolated device latency or human difficulty. All 23 sampled boards replayed complete production REVEAL/COLLECT witnesses to WON.

| Window | Boards | Tiles | Depth | Pressure | Delayed | Mixed families | Mysteries | Greedy wins | Simple wins | Lookahead wins | Target met | Median/max generation ms |
|---|---:|---|---|---|---|---|---|---:|---:|---:|---:|---|
| 1–7 | 2 | 24–28 | 2–3 | 0 | 0 | 0–1 | 0 | 2/2 | 2/2 | 2/2 | 2/2 | 366.1/732.1 |
| 8–15 | 3 | 34–40 | 4–5 | 22–27 | 10–17 | 2–3 | 2–3 | 0/3 | 3/3 | 3/3 | 3/3 | 451.3/909.8 |
| 16–30 | 3 | 40–48 | 5–6 | 30–38 | 16–20 | 3–4 | 4–6 | 0/3 | 0/3 | 3/3 | 3/3 | 1242.9/2284.9 |
| 31–50 | 3 | 48–54 | 5–6 | 29–40 | 19–24 | 4 | 7–8 | 0/3 | 0/3 | 3/3 | 3/3 | 1922.8/4558.5 |
| 51–75 | 3 | 50–54 | 6 | 36–42 | 17–24 | 4 | 10 | 0/3 | 0/3 | 3/3 | 3/3 | 2029.3/2328.3 |
| 76–100 | 3 | 48 | 5–6 | 22–35 | 17–18 | 4 | 7 | 0/3 | 0/3 | 2/3 | 2/3 | 669.2/2383.2 |
| 101–150 | 3 | 46–56 | 6–7 | 29–46 | 16–23 | 3–5 | 8–9 | 0/3 | 0/3 | 3/3 | 2/3 | 2131.0/2215.0 |
| 151+ | 3 | 46–54 | 5–6 | 36–40 | 17–21 | 4–5 | 8–10 | 1/3 | 0/3 | 3/3 | 1/3 | 2370.7/3572.8 |

Expert target windows (not claims of achieved minimum difficulty):

| Level window | Delayed-match target | Pressure-state target | Dependency-depth target | Maximum shallow wins per 3 probes |
|---|---:|---:|---:|---:|
| 31–50 | 6 | 12 | 4 | 1 |
| 51–75 | 7 | 13 | 4 | 1 |
| 76–100 | 8 | 14 | 4 | 1 |
| 101–125 | 9 | 15 | 5 | 1 |
| 126–150 | 10 | 16 | 5 | 0 |
| 151–175 | 11 | 17 | 5 | 0 |
| 401+ | 18 | 24 | 6 | 0 |

Expert generation median: 2029.3 ms; maximum: 4558.5 ms. Hard median: 451.3 ms. Quality-profile misses: 100, 101, 151, 501. All witnesses still passed. Lookahead also loses on level 100; a known witness is a separate complete solvability certificate.

Strategic immediatePairs are measured after all initially legal free reveals, preventing hidden artwork from lowering the scored opening-pair count. pressure counts verified witness states holding 2–3 symbols, not unavoidable occupancy. depth is structural dependency rounds, not a proven planning horizon. Simulated players receive neither the witness nor paid hints/undos. All lookahead runs stay within 1,800 expanded states. Mystery count and visual similarity are not strategic difficulty scores.

Targets increase across windows, but achieved delays/pressure are variable and this sample does not establish an increasing measured average or human challenge. Later strict targets miss within the 24-candidate limit. Investigate those misses and independent strategy holdouts before claiming stronger human difficulty. Saturation at 18 delayed matches / 24 pressure states / depth 6 / zero shallow wins is deliberate. Tile count stays bounded by the existing Expert range.

Reproduce: `PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH JAVA_TOOL_OPTIONS=-XX:-UsePerfData python3 tools/benchmark35.py`. Raw per-level data, layouts, initial choices, blocking, policy outcomes and witness flags are in `benchmark-final.csv`.
