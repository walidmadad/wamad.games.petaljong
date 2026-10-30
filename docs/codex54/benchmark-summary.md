# Reproducible JVM comparison

Production v11 vs v12. Both are evaluated using the same corrected observed-information player. Available mysteries are actually scanned one at a time, remembered, then re-revealed before collection. Covered, never observed identities are excluded from search. Scans retain exactly one active reveal; players use legal reveal/collect taps and known mystery + normal direct matches, with no free hide action or mystery + mystery shortcut. These are simulated policies, not human feedback. The retained v11 internal selection algorithm remains frozen for save compatibility; its original difficulty scores are not used as player evidence.

Held-out tie breaks: greedy 10–21 (12 runs/puzzle), short lookahead 10–13 (4 runs, depth 3/600-node budget), stronger lookahead 10–13 (4 runs, depth 6/1800-node budget). None of these tie breaks is used for candidate acceptance. Generation uses depth 6/1200 nodes plus depth 3/600 nodes at most, reduced to 100 each for shallow-player rejects; pair-only analysis adds at most 64 nodes. No omniscient solution or construction witness is passed to a player.

Timing: one warmup followed by interleaved old/new generation in the same JVM, fixed level order, JDK 17, 512 MiB heap, two active processors. Host timings include generation and candidate evaluation, exclude the subsequent held-out benchmark. They are not Android-device timings and can be affected by concurrent diagnostic checks.

| Levels | Puzzles | Greedy wins old → new | Short wins old → new | Strong wins old → new | Mean three-held old → new | Generation median ms old → new | New quality targets |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1–7 | 2 | 24/24 → 24/24 | 8/8 → 8/8 | 8/8 → 8/8 | 0.0 → 0.0 | 401 → 426 | 2/2 |
| 8–15 | 3 | 11/36 → 16/36 | 12/12 → 12/12 | 12/12 → 12/12 | 9.0 → 11.3 | 786 → 1078 | 3/3 |
| 16–30 | 3 | 4/36 → 4/36 | 12/12 → 12/12 | 12/12 → 12/12 | 12.7 → 19.0 | 1991 → 1937 | 3/3 |
| 31–50 | 3 | 10/36 → 1/36 | 8/12 → 12/12 | 8/12 → 12/12 | 13.3 → 21.7 | 1383 → 2366 | 3/3 |
| 51–100 | 3 | 11/36 → 1/36 | 12/12 → 11/12 | 11/12 → 12/12 | 14.3 → 21.3 | 996 → 871 | 3/3 |
| 101–200 | 3 | 7/36 → 0/36 | 12/12 → 11/12 | 11/12 → 12/12 | 14.7 → 21.3 | 553 → 1479 | 3/3 |
| 201+ | 3 | 4/36 → 0/36 | 12/12 → 10/12 | 11/12 → 12/12 | 16.3 → 19.3 | 1204 → 1028 | 2/3 |

Expert aggregate: greedy 32/144 → 2/144; stronger planning 41/48 → 48/48. New quality-target misses: [501]. Every returned puzzle still has a complete verified production-controller witness.

`pairs` counts matching opportunities after free legal inspection; `initialVisiblePairs` counts only initially face-up legal matches. `pairOnly` is bounded omniscient immediate-board-pair search, not four-slot solvability and not a player result. UNSOLVABLE here proves only that this restricted pair-only policy cannot finish; UNKNOWN is a timeout, never a loss proof. `traps` counts production-controller four-distinct losses among alternative collections along the verified route; `consequential` counts witness states with those losing choices and a known winning continuation. These do not prove a minimum required tray size or minimum human planning depth.

Family count uses explicit recognition families (leaves, pink flowers, yellow flowers, greenery, purple flowers, branches), excluding OTHER. The v12 palette also has narrower co-occurrence groups for maples and related leaf variants. Full CSV rows retain generation version, symbols, layout, legal moves, pressure, targets and witness status.

Limitations: 20 fixed levels, not an exhaustive distribution study. Tie breaks are reproducible variants of a few policies and are not independent human subjects. A stronger bounded policy need not outperform every shorter policy on every puzzle. Recognition difficulty is not measured by a symbolic solver. Human testing, device generation latency and Android visual verification remain outstanding.
