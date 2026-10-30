# Repeated generation timing

Three repeats per puzzle/version, alternating old/new order after warmup; JDK 17, two processors, fixed 512 MiB heap. The original engine is compiled from the pinned baseline revision under a diagnostic-only package prefix; the new engine is the final production implementation. Only package/import prefixes are relocated in the baseline. This timing pass ran after the other diagnostics finished. Host wall time includes full generation and candidate analysis; these are not device timings. Every repetition has the same board fingerprint as its full benchmark row and passes the complete production witness.

| Level | v11 median / max ms | v12 median / max ms | v12 candidates |
|---|---:|---:|---:|
| 8 | 236 / 315 | 633 / 675 | 3 |
| 16 | 1090 / 1701 | 414 / 537 | 2 |
| 31 | 907 / 931 | 474 / 491 | 2 |
| 75 | 331 / 338 | 502 / 506 | 2 |
| 150 | 369 / 373 | 794 / 1121 | 3 |
| 501 | 1743 / 1829 | 2982 / 3050 | 24 |
| 1001 | 1541 / 1636 | 250 / 252 | 2 |

The first qualifying candidate now stops generation after the minimum two-candidate comparison. Level 1001 previously evaluated 24 candidates and returned candidate 1 despite its existing target success; it now returns the same board after two evaluations. The retained v11 reconstruction intentionally keeps its original algorithm.

Level 501 still exhausts all 24 candidates without meeting its full late-Expert quality target; it remains explicitly marked as a quality miss. Its preparation latency remains a limitation requiring measurement on an Android device. No easy unverified fallback is substituted.
