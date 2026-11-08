This folder contains tests for all eight assignment commands.

- `check.py` contains 24 fixed cases, 960 reproducible random cases and 17 large-input cases.
- `test_results.json` stores fixed-case inputs, expected results, actual results and the defect each test checks.
- `demo.jsonl` contains example requests.
- `Benchmark.java` and `benchmark.py` compare naive, KMP and Rabin–Karp.
- `benchmark_results.json` stores measured times.
- `verification_log.txt` contains recorded verification output.

Run these commands from the repository root:

```text
python tools/run.py --check
python tests/student/check.py
python tests/student/benchmark.py
```

Examples of tests defined in `check.py`:

| Test | Expected result | Recorded actual result | Defect checked |
|---|---|---|---|
| lcs-empty-both | Length 0, empty sequence | Same | Incorrect handling of empty strings |
| merge-adjacency | Cost 203 and a valid plan | Cost 203, valid plan | Merging nonadjacent blocks |
| tree-negative | Weight 0, empty selection | Same | Selecting negative-weight vertices unnecessarily |
| naive-overlap | Positions `[0,2]` | `[0,2]` | Missing overlapping matches |
| kmp-fallback | Positions `[2]` | `[2]` | Incorrect prefix fallback |
| rk-collision | Positions `[3]` | `[3]` | Accepting equal hashes without checking characters |
| suffix-overlap | Longest repeat `"ababa"` | `"ababa"` | Missing overlapping repeats |
| aho-empty-dictionary | Counts `[[],[]]` | `[[],[]]` | Missing empty result rows |

Recorded automated verification passed all 1,968 mixed requests and all 17 large-input cases. The mixed batch runs the fixed and random cases forwards and backwards in one process to check that requests remain independent.

Further explanations, complexity analysis, benchmark interpretation and acknowledgement of ChatGPT/Codex assistance are included in the root `ANALYSIS.md`.