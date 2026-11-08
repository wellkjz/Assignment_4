"""Independent small-input oracles, boundary cases, scale and request-reset tests.
Run from the repository root: python tests/student/check.py
"""
import functools
import json
import pathlib
import random
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from run import build, execute
from public_validate import check, subsequence

SEED = 20261108


def oracle(x):
    """Intentionally slow references, used only for small inputs."""
    task = x["task"]
    if task == "lcs":
        a, b = x["a"], x["b"]
        best = ""
        for mask in range(1 << len(a)):
            s = "".join(a[i] for i in range(len(a)) if mask & (1 << i))
            if len(s) > len(best) and subsequence(s, b):
                best = s
        return {"length": len(best), "sequence": best}
    if task == "merge":
        @functools.lru_cache(None)
        def brute(blocks):
            if len(blocks) <= 1:
                return 0
            return min(blocks[i] + blocks[i + 1] + brute(
                blocks[:i] + (blocks[i] + blocks[i + 1],) + blocks[i + 2:])
                for i in range(len(blocks) - 1))
        return {"cost": brute(tuple(x["sizes"]))}
    if task == "tree":
        best, selected = 0, []
        for mask in range(1 << x["n"]):
            if any(mask & (1 << u) and mask & (1 << v) for u, v in x["edges"]):
                continue
            vertices = [i for i in range(x["n"]) if mask & (1 << i)]
            weight = sum(x["weights"][i] for i in vertices)
            if weight > best:
                best, selected = weight, vertices
        return {"weight": best, "selected": selected}
    if task in ("naive", "kmp", "rk"):
        return {"positions": [i for i in range(len(x["text"]) + 1)
                              if x["text"].startswith(x["pattern"], i)]}
    if task == "suffix":
        text = x["text"]
        sa = sorted(range(len(text)), key=lambda i: text[i:])
        lcp = [0] * len(sa)
        repeated = ""
        for i in range(1, len(sa)):
            a, b = sa[i - 1], sa[i]
            while a + lcp[i] < len(text) and b + lcp[i] < len(text) \
                    and text[a + lcp[i]] == text[b + lcp[i]]:
                lcp[i] += 1
            if lcp[i] > len(repeated):
                repeated = text[b:b + lcp[i]]
        return {"sa": sa, "lcp": lcp, "repeated": repeated}
    if task == "aho":
        return {"counts": [[sum(text.startswith(p, i) for i in range(len(text)))
                            for p in x["patterns"]] for text in x["texts"]]}
    raise ValueError(task)


def fixed_cases():
    return [
        ("lcs-empty-both", {"task": "lcs", "a": "", "b": ""}, "empty base row/column"),
        ("lcs-tie", {"task": "lcs", "a": "ABBA", "b": "BAAB"}, "accept any optimal subsequence"),
        ("lcs-punctuation", {"task": "lcs", "a": "A x! B?", "b": "A!B?"}, "subsequence, spaces and punctuation"),
        ("merge-empty", {"task": "merge", "sizes": []}, "empty plan"),
        ("merge-single", {"task": "merge", "sizes": [1000000000]}, "one block costs zero"),
        ("merge-adjacency", {"task": "merge", "sizes": [1, 100, 1]}, "arbitrary smallest blocks cannot be merged"),
        ("merge-zero-tie", {"task": "merge", "sizes": [0, 0, 0, 0]}, "valid chronological plan with tied costs"),
        ("merge-long", {"task": "merge", "sizes": [1000000000] * 4}, "64-bit total cost"),
        ("tree-empty", {"task": "tree", "n": 0, "weights": [], "edges": []}, "empty tree"),
        ("tree-negative", {"task": "tree", "n": 3, "weights": [-2, -8, -1], "edges": [[2, 0], [1, 2]]}, "empty set is allowed"),
        ("tree-long", {"task": "tree", "n": 4, "weights": [1, 1000000000, 1000000000, 1000000000], "edges": [[0, 1], [0, 2], [0, 3]]}, "64-bit weight and parent restriction"),
        ("tree-tie", {"task": "tree", "n": 2, "weights": [7, 7], "edges": [[1, 0]]}, "alternative optimal vertex set"),
        ("naive-empty", {"task": "naive", "text": "abc!", "pattern": ""}, "include final boundary"),
        ("naive-overlap", {"task": "naive", "text": "ababab", "pattern": "abab"}, "overlapping matches"),
        ("kmp-fallback", {"task": "kmp", "text": "abababacaba", "pattern": "ababaca"}, "repeated prefix fallback"),
        ("kmp-both-empty", {"task": "kmp", "text": "", "pattern": ""}, "do not index empty pattern"),
        ("rk-collision", {"task": "rk", "text": "dj ab dj", "pattern": "ab"}, "equal hash does not imply equality"),
        ("rk-long-pattern", {"task": "rk", "text": "a", "pattern": "aaaa"}, "no first window exists"),
        ("suffix-empty", {"task": "suffix", "text": ""}, "empty array has no lcp[0]"),
        ("suffix-ascii", {"task": "suffix", "text": "$ a$ a"}, "space and dollar are ordinary characters"),
        ("suffix-overlap", {"task": "suffix", "text": "abababa"}, "overlapping longest repeat"),
        ("aho-suffix-duplicate", {"task": "aho", "patterns": ["a", "ba", "aba", "a"], "texts": ["ababa", "", "ababa"]}, "failure counts, duplicates, per-text reset"),
        ("aho-empty-dictionary", {"task": "aho", "patterns": [], "texts": ["", "x"]}, "one empty row per text"),
        ("aho-no-texts", {"task": "aho", "patterns": ["a"], "texts": []}, "zero result rows"),
    ]


def random_cases():
    rng = random.Random(SEED)
    def string(n):
        return "".join(rng.choice('ab A!$\\"') for _ in range(n))
    cases = []
    for test in range(120):
        cases.append({"task": "lcs", "a": string(rng.randrange(9)), "b": string(rng.randrange(9))})
        cases.append({"task": "merge", "sizes": [rng.randrange(10) for _ in range(rng.randrange(8))]})
        n = rng.randrange(11)
        labels = list(range(n))
        rng.shuffle(labels)
        cases.append({"task": "tree", "n": n, "weights": [rng.randrange(-5, 10) for _ in range(n)],
                      "edges": [[labels[i], labels[rng.randrange(i)]] for i in range(1, n)]})
        text, pattern = string(rng.randrange(35)), string(rng.randrange(10))
        for task in ("naive", "kmp", "rk"):
            cases.append({"task": task, "text": text, "pattern": pattern})
        cases.append({"task": "suffix", "text": string(rng.randrange(40))})
        patterns = [string(rng.randrange(1, 7)) for _ in range(rng.randrange(9))]
        if patterns:
            patterns.append(patterns[0])
        cases.append({"task": "aho", "patterns": patterns,
                      "texts": [string(rng.randrange(40)) for _ in range(rng.randrange(5))]})
    return cases


def scale_cases():
    yield "lcs-1000", {"task": "lcs", "a": "a" * 1000, "b": "a" * 1000}, {"length": 1000}
    # Equal leaf weights: minimum external path length at n=200 is 1544.
    yield "merge-200-long", {"task": "merge", "sizes": [1000000000] * 200}, {"cost": 1544000000000}
    yield "tree-chain-100000", {"task": "tree", "n": 100000, "weights": [1000000000] * 100000,
                               "edges": [[i - 1, i] for i in range(1, 100000)]}, {"weight": 50000000000000}
    yield "tree-star-100000", {"task": "tree", "n": 100000, "weights": [1000000000] * 100000,
                              "edges": [[0, i] for i in range(1, 100000)]}, {"weight": 99999000000000}
    for task, n, m in [("naive", 3000, 1500), ("kmp", 200000, 100000), ("rk", 10000, 2000)]:
        yield task + "-dense-max", {"task": task, "text": "a" * n, "pattern": "a" * m}, {"positions": list(range(n - m + 1))}
        yield task + "-near-miss", {"task": task, "text": "a" * n, "pattern": "a" * (m - 1) + "b"}, {"positions": []}
    yield "kmp-empty-200000", {"task": "kmp", "text": "x" * 200000, "pattern": ""}, {"positions": list(range(200001))}
    yield "rk-pattern-10000", {"task": "rk", "text": "a", "pattern": "a" * 10000}, {"positions": []}
    n = 50000
    yield "suffix-50000", {"task": "suffix", "text": "a" * n}, {
        "sa": list(range(n - 1, -1, -1)), "lcp": list(range(n)), "repeated": "a" * (n - 1)}
    yield "aho-deep-100000", {"task": "aho", "patterns": ["a" * 100000], "texts": ["a" * 200000]}, {"counts": [[100001]]}
    yield "aho-10000-duplicates", {"task": "aho", "patterns": ["a" * 10] * 10000, "texts": ["a" * 200000]}, {"counts": [[199991] * 10000]}
    yield "aho-100-texts", {"task": "aho", "patterns": ["a" * 10] * 10000, "texts": ["a" * 2000] * 100}, {"counts": [[1991] * 10000 for _ in range(100)]}
    patterns = ["a" * i for i in range(1, 447)] # L = 99681, dense suffix-pattern matches.
    yield "aho-dense-suffixes", {"task": "aho", "patterns": patterns, "texts": ["a" * 200000]}, {"counts": [[200000 - len(p) + 1 for p in patterns]]}


def main():
    command = build("Java")
    fixed = fixed_cases()
    requests = [x for _, x, _ in fixed] + random_cases()
    expected = [oracle(x) for x in requests]
    # Forward then reverse order in the same JVM exposes stale task state.
    batch = requests + list(reversed(requests))
    answers = execute(command, batch)
    for index, (x, y, e) in enumerate(zip(batch, answers, expected + list(reversed(expected)))):
        assert check(x, y, e), (index, x, e, y)
    print(f"PASS {len(fixed)} fixed + {len(requests) - len(fixed)} random cases; {len(batch)} mixed requests")
    report = {"seed": SEED, "mixed_requests": len(batch), "fixed": [], "scale": []}
    for i, (name, x, purpose) in enumerate(fixed):
        report["fixed"].append({"name": name, "input": x, "expected": expected[i],
                                "actual": answers[i], "checks": purpose, "passed": True})
    for name, x, expected_output in scale_cases():
        start = time.perf_counter()
        answer = execute(command, [x])[0]
        elapsed = time.perf_counter() - start
        assert check(x, answer, expected_output), name
        print(f"PASS {name}: {elapsed:.3f} s (JVM + JSON + algorithm)")
        report["scale"].append({"name": name, "passed": True, "seconds": round(elapsed, 6)})
    path = pathlib.Path(__file__).with_name("test_results.json")
    path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"Saved {path.name}")


if __name__ == "__main__":
    main()
