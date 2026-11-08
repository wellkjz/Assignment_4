# Assignment 4: analysis

## Environment and commands

Selected language: **Java 17** (`submission.json`).

The recorded runs used Eclipse Temurin OpenJDK **17.0.18+8**, the OpenJDK 64-Bit Server VM, Python **3.12.10**, and Windows 11 build **26200**. Hardware: Intel Core **i5-12450HX**, 12 logical CPUs, 16,873,545,728 bytes of usable physical RAM (about 15.71 GiB). The launcher limited the Java heap to **384 MiB**. These were local desktop measurements, not measurements inside the teacher's isolated one-CPU grading container.

Run these commands from the root of a copy of the official template:

```text
python tools/run.py --setup
python tools/run.py --check
python tests/student/check.py
python tests/student/benchmark.py
python tools/run.py --input tests/student/demo.jsonl
```

The first two commands compile with `javac --release 17 -encoding UTF-8 -d Java/build` and run `java -Xmx384m -cp Java/build Main`. The launcher supplies the three source paths automatically. `check.py` also builds the solution. `benchmark.py` builds the solution and then compiles `tests/student/Benchmark.java` separately into `Java/build`.

The test shell temporarily prepended the installed Temurin 17 `bin` directory to its PATH. This selected JDK 17 instead of the older Java 8 that was first on the machine's default PATH. No permanent system setting was changed. On another machine, ensure that both `java -version` and `javac -version` show version 17 or newer.

The official `Main.java` and `Json.java` transport were retained. Only `Algorithms.java` implements the required methods. A request produces exactly one JSON object; there are no prompts or debug messages on stdout. All mutable algorithm data belongs to the current request. Indices are zero-based, and printable ASCII characters, including spaces and punctuation, are compared exactly.

## Algorithm explanations

### LCS

`dp[i][j]` is the maximum length of a common subsequence of the first `i` characters of `a` and the first `j` characters of `b`. Row zero and column zero contain zero because an empty string has no nonempty subsequence.

If the last characters match, `dp[i][j] = dp[i-1][j-1] + 1`. Otherwise, it is `max(dp[i-1][j], dp[i][j-1])`. Rows are processed from small to large indices, so each needed state already exists. For matching last characters, an optimum can use that final pair. For a mismatch, a common subsequence must omit at least one of the two last characters, so the two smaller prefix states cover the possibilities. Induction on the prefix lengths proves the table values.

Reconstruction starts at `(n,m)`. Matching characters are appended while moving diagonally. At a mismatch, the walk follows a neighbour with the larger value. A tie moves upwards; either optimal direction is allowed. Reversing the collected characters restores their original order. Each chosen character belongs to both prefixes, and the walk recovers exactly `dp[n][m]` characters. Empty inputs return zero and an empty string.

Time and auxiliary memory are `O((n+1)(m+1))`, including initialization and empty inputs. Reconstruction takes `O(n+m+1)` time. Input storage is `O(n+m+1)`; output and the reconstruction buffer use `O(min(n,m)+1)` space. Tests include empty strings, noncontiguous matches, punctuation and several optimal subsequences.

### Ordered adjacent-block merging

`dp[left][right]` is the minimum cost to turn the closed original interval into one block. A single block costs zero. Prefix sums give an interval's total size in constant time.

The final merge must combine `[left,k]` and `[k+1,right]` for some `k`. Its cost is the interval sum, so the transition is the minimum of `dp[left][k] + dp[k+1][right] + sum(left,right)` over all splits. Increasing interval lengths ensure that both child intervals are ready. Every legal plan has such a last split. Replacing either child plan with a cheaper one would improve the whole plan, so optimal subplans and the best split give the global optimum.

`split[left][right]` remembers the first strictly better split. Reconstruction emits both child plans before adding `[left,k,right]`. Therefore the two adjacent child blocks exist at the moment their parent merge is emitted. There are exactly `n-1` merges. Ties keep the first split. Empty and one-block inputs return cost zero and an empty plan. Recursion is used only for this reconstruction, whose depth is at most 200.

Time is `O(n^3)` and auxiliary space is `O(n^2)` for nonempty input. Input and output storage are `O(n+1)`; the reconstruction stack is `O(n+1)`. The empty case takes constant time and space. Prefix sums, candidate costs and DP entries use `long`. `[1,100,1]` catches illegal nonadjacent merging; four blocks of size one billion catch 32-bit overflow.

### Tree independent set

The tree is rooted at vertex zero. An array queue records each vertex after its parent. Iterating this order backwards gives an order in which every child is processed before its parent. This avoids stack overflow on a chain of 100,000 vertices.

`take[v]` is the best weight in `v`'s subtree when `v` is selected. `skip[v]` is the best weight when it is not selected. The transitions are:

```text
take[v] = weights[v] + sum(skip[child])
skip[v] = sum(max(take[child], skip[child]))
```

A selected parent forbids each child. An unselected parent permits either child state. Different child subtrees have no edges between them, so their optimum values can be added. Induction from leaves proves these transitions. A leaf has `take = weight` and `skip = 0`; this also makes the empty set available when all weights are negative.

Reconstruction scans the parent-before-child order. A vertex is selected only when its parent is not selected and its `take` value is strictly larger than its `skip` value. Ties choose skip. This respects every edge and follows an optimal state decision in each subtree. Vertex order in the output is unrestricted. The empty tree returns weight zero and an empty list.

Time, auxiliary space and input storage are `O(n+1)`; output is at most `O(n+1)`. The implementation uses adjacency lists, primitive arrays and `long` weights. Tests include negative weights, ties, relabelled trees, a maximum-size chain and a maximum-size star.

### Naive search

The outer loop examines every start from zero through `n-m`, inclusive. The inner loop compares characters until a mismatch or the pattern's end. A reported start is correct because all `m` characters matched. Every possible start is examined, so no match is missed, including overlaps.

If `m=0`, the inner loop performs no comparisons and the outer loop reports all `n+1` boundaries. If `m>n`, the outer loop does not execute. For both empty strings the answer is `[0]`.

Time is `O(n*m+n+1)`, including output. Auxiliary space is `O(1)`, input storage is `O(n+m+1)` and output storage is `O(z+1)`, where `z` is the number of matches. Tests check the final empty-pattern boundary and overlapping matches.

### KMP

`pi[i]` is the length of the longest proper prefix of `pattern[0..i]` that is also its suffix. The prefix function is built left to right. On a mismatch, `j = pi[j-1]` tries the next possible border rather than restarting from every shorter length. On a match, `j` increases by one.

During text scanning, `j` is the length of a pattern prefix matching a suffix of the text already processed. A mismatch can only leave a border of that matched prefix, so following the prefix-function chain preserves all possible matches. When `j=m`, a complete match ends at the current text index. Its start is `i-m+1`. Resetting `j` to `pi[j-1]` preserves overlaps. Empty patterns are handled before any pattern indexing.

Each increase of `j` is paired with at most the same total number of decreases. Thus prefix construction and scanning take `O(n+m+z+1)` time. Auxiliary space is `O(m+1)`, input storage is `O(n+m+1)` and output is `O(z+1)`. Tests exercise repeated fallback, an empty pattern and the maximum text and pattern lengths.

### Exact Rabin-Karp

The hash uses ASCII codes, base **31**, modulus **101**, and the highest power on the left. For a pattern of length `m`, `power` stores `31^(m-1) mod 101`. The first pattern and window hashes are built left to right.

To slide one position, remove the outgoing character's contribution, normalize the remainder, multiply by 31 and add the new character:

```text
h = (h - outgoing * power % 101 + 101) % 101
h = (h * 31 + incoming) % 101
```

This preserves the polynomial hash of the current window. Equal strings have equal hashes, so a real match cannot be rejected by unequal hashes. Equal hashes alone are insufficient: every candidate is compared character by character before reporting. This rejects collisions such as `ab` and `dj`, which both hash to 75. Starts are scanned in increasing order, including overlaps. Empty patterns and patterns longer than the text are handled before forming a window.

For `1 <= m <= n`, time is `O(n+m+c*m)`, where `c` is the number of equal-hash candidate windows, including true matches. A bound for all allowed cases is `O(n*m+n+m+1)`. The algorithm is exact but does **not** have deterministic linear worst-case time. It uses `long` hash arithmetic, `O(1)` auxiliary space, `O(n+m+1)` input storage and `O(z+1)` output storage.

### Suffix array and Kasai LCP

Initially, each suffix rank is its first character's ASCII code. In a doubling round, a suffix is represented by the pair `(rank[i], rank[i+length])`; a missing second part has rank -1. The current ranks describe equal-length prefixes, so comparing the pairs orders prefixes twice as long. `Arrays.sort` sorts suffix indices using these two integers, then equal pairs receive equal new ranks. The prefix length doubles after every round. Once all ranks are distinct, they determine the final suffix order. There is no output sentinel or empty suffix, and whole suffix strings are never constructed for sorting.

With comparison sorting, doubling takes `O(n log^2(n+1))` time. Java's object-array sort and the rank arrays use `O(n+1)` auxiliary space. Empty and one-character strings need no sorting rounds.

Kasai uses an inverse suffix array. For each text start `i`, it compares the suffix with its lexicographic predecessor, extending a known common-prefix length. After moving to `i+1`, at least the old common length minus one is a valid lower bound whenever the predecessor exists; any closer predecessor shares at least that prefix. The first suffix in lexicographic order has LCP zero and resets this carried value. The total extension work is linear because the carried length decreases by at most one per iteration. `lcp[0]=0` and `lcp[k]` compares `sa[k-1]` with `sa[k]`.

Suffixes sharing a prefix form a consecutive interval in lexicographic order. Therefore any repeated substring is a prefix shared by some adjacent pair, and the maximum adjacent LCP gives a longest repeat. The implementation keeps the first strictly larger maximum encountered during Kasai's scan and extracts that one substring. Overlapping occurrences are valid. Empty input returns empty arrays and an empty string.

Kasai takes `O(n+1)` time and space. Overall auxiliary, input and output storage are `O(n+1)`. Tests include spaces, `$`, overlapping repeats and 50,000 identical characters. The slow oracle creates suffix strings only for tiny test cases, not in the submitted algorithm.

### Aho-Corasick

The trie has one transition row per actual state and 95 columns, one for each printable ASCII character. A dictionary-position array records terminal states. Duplicate words share a terminal state but keep separate dictionary positions.

Breadth-first processing constructs failure links. A failure link points to the longest proper suffix that is a trie prefix. The failure state's transitions are already ready because that state is shallower. Missing transitions reuse failure transitions, so scanning one character takes constant time. After a prefix of text is read, the current state is its longest suffix that is a trie prefix.

For each text, a fresh `long[] visits` and state zero are used. Scanning increments the reached state. Reverse breadth-first order then adds `visits[v]` to `visits[fail[v]]`. A suffix pattern matches whenever its terminal state is reached directly or is on a reached state's failure chain. The propagation accounts for both cases once per text end position, without enumerating all matches. Reading the visit count at each dictionary position's terminal handles duplicates and preserves column order. Text order is also preserved.

With total dictionary length `L`, `p` dictionary positions and `S` actual trie states including the root, construction costs `O(L+95*S+p)` time and `O(95*S+p)` auxiliary memory. For each independent text, processing costs `O(|text|+S+p)`. The complete output uses `O(q*(p+1)+1)` space for `q` texts; input storage is `O(L+total_text_length+p+q+1)`. No inherited output lists are copied. An empty dictionary produces one empty row per text; no texts produce no rows; an empty text produces a row of zeros. Tests cover suffix patterns, duplicates, dense matches, a deep trie and independent texts.

## Own tests and actual results

`tests/student/check.py` contains **24 fixed cases**, **960 reproducible random cases** with seed **20261108**, and **17 scale cases**. The fixed and random cases are sent forwards and backwards in one process: **1,968 JSONL requests** in total. All passed. The separate public check passed **44/44 cases** plus its two complete batches in one process. These results do not claim access to the teacher's hidden tests.

Small-input references use exhaustive subsequences for LCS, exhaustive adjacent merge choices for merging, exhaustive vertex subsets for trees, and direct string comparisons for searches and dictionary counts. The suffix oracle sorts full suffix strings only on tiny inputs. This checks the implementations against simple independent methods. Reconstructed subsequences, merge plans and vertex sets are validated structurally and by optimum value; an alternative optimum is accepted.

The complete input, expected result, actual result and defect checked for every fixed case are in `tests/student/test_results.json`. For merging, the reference records the optimum cost and the public validator checks the actual chronological plan. Large fixture definitions and their expected values are in `scale_cases()`; their observed status and elapsed time are in the same result file.

| Case in `check.py` | Expected | Actual | Defect detected |
| --- | --- | --- | --- |
| `lcs-empty-both` | length 0, sequence empty | 0, empty | Invalid empty-table indexing |
| `lcs-punctuation` | length 4, `A!B?` | length 4, `A!B?` | Confusing subsequence with substring |
| `merge-adjacency` | cost 203, valid two-step plan | cost 203, valid plan | Merging the separated size-1 blocks |
| `merge-long` | cost 8,000,000,000 | 8,000,000,000 | 32-bit overflow |
| `tree-negative` | weight 0, selected `[]` | weight 0, selected `[]` | Forcing a negative vertex |
| `tree-long` | weight 3,000,000,000, leaves 1,2,3 | same | Overflow or adjacent selections |
| `naive-empty` | `[0,1,2,3,4]` | `[0,1,2,3,4]` | Missing final boundary |
| `naive-overlap` | `[0,2]` | `[0,2]` | Skipping overlaps |
| `kmp-fallback` | `[2]` | `[2]` | Incorrect border fallback |
| `rk-collision` | `[3]` | `[3]` | Treating hash equality as a match |
| `suffix-overlap` | maximum LCP 5, repeat `ababa` | same | Disallowing overlapping repeats |
| `aho-suffix-duplicate` | `[[3,2,2,3],[0,0,0,0],[3,2,2,3]]` | same | Missing suffix counts, duplicates or reset |

Scale tests passed for LCS 1000 by 1000, 200 large merge blocks, 100,000-vertex chain and star, KMP text 200,000/pattern 100,000, RK text 10,000/pattern 2,000, suffix text 50,000, a 100,000-character Aho pattern, 10,000 duplicate dictionary positions and 100 independent texts. Dense suffix-pattern matching was also tested. Each scale request used the launcher's 20-second timeout and 384 MiB heap; all completed locally in under one second including JVM startup, JSON transport and algorithm work. These end-to-end scale timings are distinct from the direct-method search comparison below.

## Search comparison

The reproducible generator and timing code are `tests/student/Benchmark.java`, run by `python tests/student/benchmark.py`. The original measured nanosecond batches are stored in `tests/student/benchmark_results.json`.

All methods receive the same prebuilt text and pattern for each case. Three increasing `(n,m)` pairs are `(500,50)`, `(1500,150)` and `(3000,300)`, all in the common input domain. For random cases, Java `Random(20261108 + n)` selects uniformly from `a,b,c,d`; the pattern is the length-`m` substring beginning at `n/2`, guaranteeing a real match. Repetitive cases use `a` repeated `n` times and `a` repeated `m` times, producing `n-m+1` overlapping matches.

Before timing, all three outputs are compared with independently generated expected starts using Java `startsWith` in the benchmark only. There are 200 untimed warm-up calls per method per case. Then three measured runs are made per method and case, each containing 100 calls. Method order rotates across runs. `System.nanoTime` measures the same boundary for all methods: dispatch, input-field access, the full algorithm and result allocation. Compilation, JVM startup, input generation, correctness checking, JSON parsing/printing and warm-up are excluded. A volatile result reference prevents unused results from being discarded. JIT remains enabled; warm-up reduces startup effects but does not guarantee a fully stable JVM, and garbage collection or desktop activity may influence results.

The table reports **microseconds per call**: each raw batch time divided by 100 calls and then by 1000. The median is the median of the three per-call values. Correct outputs were checked for every row.

| Case | n | m | Algorithm | Run 1 (us) | Run 2 (us) | Run 3 (us) | Median (us) | Correct |
| --- | ---: | ---: | --- | ---: | ---: | ---: | ---: | --- |
| random | 500 | 50 | naive | 2.642 | 0.923 | 0.816 | 0.923 | Yes |
| random | 500 | 50 | kmp | 1.116 | 1.507 | 1.070 | 1.116 | Yes |
| random | 500 | 50 | rk | 4.734 | 4.225 | 4.086 | 4.225 | Yes |
| random | 1500 | 150 | naive | 2.666 | 2.282 | 2.365 | 2.365 | Yes |
| random | 1500 | 150 | kmp | 3.099 | 4.239 | 3.405 | 3.405 | Yes |
| random | 1500 | 150 | rk | 12.532 | 14.344 | 12.100 | 12.532 | Yes |
| random | 3000 | 300 | naive | 7.469 | 4.990 | 4.493 | 4.990 | Yes |
| random | 3000 | 300 | kmp | 6.108 | 7.657 | 6.505 | 6.505 | Yes |
| random | 3000 | 300 | rk | 26.317 | 23.769 | 24.899 | 24.899 | Yes |
| repetitive | 500 | 50 | naive | 24.130 | 26.488 | 30.238 | 26.488 | Yes |
| repetitive | 500 | 50 | kmp | 2.433 | 2.460 | 2.965 | 2.460 | Yes |
| repetitive | 500 | 50 | rk | 40.725 | 40.050 | 41.235 | 40.725 | Yes |
| repetitive | 1500 | 150 | naive | 199.687 | 195.132 | 195.575 | 195.575 | Yes |
| repetitive | 1500 | 150 | kmp | 7.506 | 9.739 | 11.421 | 9.739 | Yes |
| repetitive | 1500 | 150 | rk | 351.641 | 366.618 | 358.354 | 358.354 | Yes |
| repetitive | 3000 | 300 | naive | 757.650 | 846.632 | 812.055 | 812.055 | Yes |
| repetitive | 3000 | 300 | kmp | 25.293 | 28.877 | 25.406 | 25.406 | Yes |
| repetitive | 3000 | 300 | rk | 1390.204 | 1481.442 | 1436.484 | 1436.484 | Yes |

Naive search was fastest on these random inputs because most candidate starts fail after very few comparisons, while KMP pays for its prefix table and RK pays for modular arithmetic. This does not contradict their worst-case bounds. On repetitive inputs, every window is a real match. Naive and exact RK repeatedly check long patterns, while KMP reuses prefix information. At `(3000,300)`, the recorded medians were about 812.055 microseconds for naive, 25.406 for KMP and 1436.484 for RK.

Modulus 101 deliberately creates a small collision laboratory. False hash candidates add verification work, but do not make the result incorrect. The repetitive benchmark mainly demonstrates the cost of verifying many true matches, not just false collisions. Larger production moduli or multiple hashes can reduce false candidates; these measurements should not be generalized to every production RK implementation. Three timing medians do not prove asymptotic complexity.

## Sources and AI assistance

- [Official Assignment 4 template](https://github.com/RZhaniya/daa-assignment-4-archive), inspected at revision `4686cc60334aa189c2ac168e10a82d882b32858c`: interface, supplied transport, launcher, 44 public fixtures and public validator.
- [English specification](https://github.com/RZhaniya/daa-assignment-4-archive/blob/main/docs/Assignment_4_EN.md), [rubric](https://github.com/RZhaniya/daa-assignment-4-archive/blob/main/docs/guide/RUBRIC.md), and the template's analysis outline: required methods, domains, output schemas and analysis/benchmark requirements.
- ChatGPT/Codex assisted with writing analysis.
