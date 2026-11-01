"""Independent small oracles, witness checks, mixed batches and full-domain stress."""
import functools
import json
import pathlib
import platform
import random
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from run import build, execute
from public_validate import check

SEED = 20261101


def positions(text, pattern):
    # Built-in matching is used only in the independent test oracle.
    return [i for i in range(len(text) + 1) if text.startswith(pattern, i)]


def lcs_oracle(a, b):
    # Enumerate subsequences, independently of prefix DP and its backtracking.
    best = ""
    for mask in range(1 << len(a)):
        candidate = "".join(c for i, c in enumerate(a) if mask >> i & 1)
        it = iter(b)
        if len(candidate) > len(best) and all(c in it for c in candidate):
            best = candidate
    return {"length": len(best)}


@functools.lru_cache(None)
def merge_oracle(blocks):
    # Try actual adjacent moves on the current block list, not interval DP.
    if len(blocks) < 2:
        return 0
    return min(blocks[i] + blocks[i + 1]
               + merge_oracle(blocks[:i] + (blocks[i] + blocks[i + 1],) + blocks[i + 2:])
               for i in range(len(blocks) - 1))


def tree_oracle(weights, edges):
    best = 0
    for mask in range(1 << len(weights)):
        if all(not (mask >> u & 1 and mask >> v & 1) for u, v in edges):
            best = max(best, sum(w for v, w in enumerate(weights) if mask >> v & 1))
    return {"weight": best}


def suffix_oracle(text):
    # Materializing short suffixes is deliberately confined to the test oracle.
    sa = sorted(range(len(text)), key=lambda i: text[i:])
    lcp = [0] * len(text)
    repeated = ""
    for k in range(1, len(text)):
        i, j, common = sa[k - 1], sa[k], 0
        while i + common < len(text) and j + common < len(text) and text[i + common] == text[j + common]:
            common += 1
        lcp[k] = common
        if common > len(repeated):
            repeated = text[i:i + common]
    return {"sa": sa, "lcp": lcp, "repeated": repeated}


def aho_oracle(patterns, texts):
    return {"counts": [[len(positions(text, pattern)) for pattern in patterns] for text in texts]}


def fixtures():
    cases = []

    def add(name, request, expected):
        cases.append({"name": name, "input": request, "output": expected})

    for name, a, b in [("both-empty", "", ""), ("left-empty", "", "abc"),
                       ("right-empty", "abc", ""), ("subsequence", "tea_time", "team"),
                       ("ties", "ab", "ba"), ("repetitions", "aaabaa", "baaaa"),
                       ("ascii-escaping", 'a "\\~', ' "\\')]:
        add("lcs-" + name, {"task": "lcs", "a": a, "b": b}, lcs_oracle(a, b))
    for name, blocks in [("empty", []), ("one", [10**9]), ("zero-ties", [0, 0, 0, 0]),
                          ("adjacency", [1, 100, 1]), ("64bit", [10**9] * 4),
                          ("chronology", [8, 1, 3, 2, 9])]:
        add("merge-" + name, {"task": "merge", "sizes": blocks}, {"cost": merge_oracle(tuple(blocks))})
    for name, weights, edges in [("empty", [], []), ("negative-one", [-7], []),
                                  ("positive-one", [10**9], []), ("zero-ties", [0, 0], [[0, 1]]),
                                  ("negative", [-4, -2, -7], [[0, 1], [1, 2]]),
                                  ("skip-middle", [4, 9, 6], [[0, 1], [1, 2]]),
                                  ("64bit", [10**9] * 5, [[0, i] for i in range(1, 5)])]:
        add("tree-" + name, {"task": "tree", "n": len(weights), "weights": weights, "edges": edges},
            tree_oracle(weights, edges))
    pairs = [("both-empty", "", ""), ("empty-pattern", "abc", ""), ("empty-text", "", "x"),
             ("long-pattern", "ab", "abc"), ("overlaps", "aaaaa", "aaa"),
             ("fallback", "ababababacababac", "ababac"), ("collision", "abdj", "ab"),
             ("negative-remainder", "~ ! ab~ !", " !"),
             ("ascii-case", 'A a!A a!"\\~', 'A a!'), ("last-window", "xyzabc", "abc"),
             ("dense-collisions", "a" * 40, "a" * 8 + "bB"),
             ("full-ascii", "".join(map(chr, range(32, 127))) * 2, "~ !")]
    for task in ("naive", "kmp", "rk"):
        for name, text, pattern in pairs:
            add(task + "-" + name, {"task": task, "text": text, "pattern": pattern},
                {"positions": positions(text, pattern)})
    for text in ("", "x", "aaaaaa", "zyxw", "banana", "abXabYcdXcdY", ' ~!"\\ ',
                 "".join(map(chr, range(32, 127)))):
        add("suffix-" + repr(text), {"task": "suffix", "text": text}, suffix_oracle(text))
    for name, patterns, texts in [("empty-dictionary", [], ["", "abc", "abc"]),
                                  ("no-texts", ["a"], []), ("empty-text", ["a", "aa"], [""]),
                                  ("suffix-duplicates-reset", ["he", "she", "he", "e"], ["sheshe", "sheshe", ""]),
                                  ("dense-overlaps", ["a", "aa", "aaa", "aa"], ["aaaaa"]),
                                  ("ascii", ['"', "\\", " ", "~!"], ['"\\ ~!"']),
                                  ("full-ascii", list(map(chr, range(32, 127))),
                                   ["".join(map(chr, range(32, 127))) * 2])]:
        add("aho-" + name, {"task": "aho", "patterns": patterns, "texts": texts}, aho_oracle(patterns, texts))
    return cases


def random_cases():
    rng = random.Random(SEED)
    cases = []

    def word(limit):
        alphabet = "abAB !~\"\\"
        return "".join(rng.choice(alphabet) for _ in range(rng.randrange(limit + 1)))

    def add(request, expected):
        cases.append({"name": f"random-{request['task']}-{len(cases)}", "input": request, "output": expected})

    for _ in range(80):
        a, b = word(9), word(9)
        add({"task": "lcs", "a": a, "b": b}, lcs_oracle(a, b))
    for _ in range(80):
        sizes = [rng.randrange(8) for _ in range(rng.randrange(8))]
        add({"task": "merge", "sizes": sizes}, {"cost": merge_oracle(tuple(sizes))})
    for _ in range(100):
        n = rng.randrange(13)
        weights = [rng.randrange(-10, 16) for _ in range(n)]
        # Random relabeling makes the chosen root independent of tree shape.
        labels = rng.sample(range(n), n)
        edges = [[labels[i], labels[rng.randrange(i)]] for i in range(1, n)]
        rng.shuffle(edges)
        add({"task": "tree", "n": n, "weights": weights, "edges": edges}, tree_oracle(weights, edges))
    for _ in range(300):
        text, pattern = word(80), word(20)
        if text and rng.randrange(2):
            start = rng.randrange(len(text))
            pattern = text[start:start + rng.randrange(15)]
        expected = {"positions": positions(text, pattern)}
        for task in ("naive", "kmp", "rk"):
            add({"task": task, "text": text, "pattern": pattern}, expected)
    for _ in range(180):
        text = word(70)
        add({"task": "suffix", "text": text}, suffix_oracle(text))
    for _ in range(120):
        patterns = [word(8) or "a" for _ in range(rng.randrange(14))]
        if patterns:
            patterns.append(patterns[0])
        texts = [word(60) for _ in range(rng.randrange(7))]
        add({"task": "aho", "patterns": patterns, "texts": texts}, aho_oracle(patterns, texts))
    rng.shuffle(cases)
    return cases


def scale_cases():
    cases = []

    def add(name, request, expected):
        cases.append({"name": name, "input": request, "output": expected})

    for name, a, b, length in [("same", "a" * 1000, "a" * 1000, 1000),
                                ("disjoint", "a" * 1000, "b" * 1000, 0),
                                ("crossed", "a" * 500 + "b" * 500, "b" * 500 + "a" * 500, 500)]:
        add("lcs-1000-" + name, {"task": "lcs", "a": a, "b": b}, {"length": length})
    for name, sizes, cost in [("equal-64bit", [10**9] * 200, 1544 * 10**9),
                               ("zeros", [0] * 200, 0), ("sparse", [10**9] + [0] * 199, 10**9)]:
        add("merge-200-" + name, {"task": "merge", "sizes": sizes}, {"cost": cost})
    n = 100000
    for name, edges, weights, weight in [
            ("chain", [[i - 1, i] for i in range(1, n)], [10**9] * n, 50000 * 10**9),
            ("star", [[0, i] for i in range(1, n)], [10**9] * n, 99999 * 10**9),
            ("negative-chain", [[i - 1, i] for i in range(1, n)], [-10**9] * n, 0)]:
        add("tree-100000-" + name, {"task": "tree", "n": n, "edges": edges, "weights": weights}, {"weight": weight})
    for task, n, m in [("naive", 3000, 1500), ("kmp", 200000, 100000), ("rk", 10000, 2000)]:
        add(task + "-max-overlap", {"task": task, "text": "a" * n, "pattern": "a" * m},
            {"positions": list(range(n - m + 1))})
        add(task + "-max-empty", {"task": task, "text": "a" * n, "pattern": ""},
            {"positions": list(range(n + 1))})
    add("kmp-max-fallback", {"task": "kmp", "text": "a" * 200000, "pattern": "a" * 99999 + "b"}, {"positions": []})
    add("naive-max-pattern", {"task": "naive", "text": "a" * 3000, "pattern": "a" * 3000}, {"positions": [0]})
    add("naive-max-longer", {"task": "naive", "text": "", "pattern": "a" * 3000}, {"positions": []})
    add("kmp-max-longer", {"task": "kmp", "text": "", "pattern": "a" * 100000}, {"positions": []})
    add("rk-max-collisions", {"task": "rk", "text": "a" * 10000, "pattern": "a" * 1998 + "bB"}, {"positions": []})
    add("rk-max-pattern-longer", {"task": "rk", "text": "a" * 2000, "pattern": "a" * 10000}, {"positions": []})
    n = 50000
    add("suffix-50000-equal", {"task": "suffix", "text": "a" * n},
        {"sa": list(range(n - 1, -1, -1)), "lcp": list(range(n)), "repeated": "a" * (n - 1)})
    add("suffix-50000-periodic", {"task": "suffix", "text": "ab" * (n // 2)},
        {"sa": list(range(n - 2, -1, -2)) + list(range(n - 1, -1, -2)),
         "lcp": [0] + list(range(2, n - 1, 2)) + [0] + list(range(1, n - 2, 2)),
         "repeated": "ab" * (n // 2 - 1)})
    # Maximum trie depth and total pattern/text length; no recursive traversal.
    add("aho-100000-depth", {"task": "aho", "patterns": ["a" * 100000], "texts": ["a" * 200000]},
        {"counts": [[100001]]})
    add("aho-10000-duplicates", {"task": "aho", "patterns": ["a" * 10] * 10000, "texts": ["a" * 200000]},
        {"counts": [[199991] * 10000]})
    patterns = ["a" * i for i in range(1, 447)]  # Total length 99,681.
    add("aho-dense-suffixes", {"task": "aho", "patterns": patterns, "texts": ["a" * 200000]},
        {"counts": [[200001 - i for i in range(1, 447)]]})
    add("aho-max-output-reset", {"task": "aho", "patterns": ["a"] * 10000, "texts": ["a" * 2000] * 100},
        {"counts": [[2000] * 10000 for _ in range(100)]})
    add("aho-max-empty-rows", {"task": "aho", "patterns": [], "texts": [""] * 100},
        {"counts": [[] for _ in range(100)]})
    return cases


def main():
    command = build("Java")
    report = {"seed": SEED, "python": platform.python_version(), "platform": platform.platform(),
              "command": "python tests/student/run_tests.py", "batches": []}

    def batch(label, cases, save_details=False):
        start = time.perf_counter()
        answers = execute(command, [case["input"] for case in cases])
        elapsed = time.perf_counter() - start
        for case, answer in zip(cases, answers):
            if not check(case["input"], answer, case["output"]):
                raise AssertionError(f"{label}: {case['name']} failed: {str(answer)[:1000]}")
        report["batches"].append({"name": label, "cases": len(cases), "seconds": elapsed, "status": "PASS"})
        if save_details:
            report["fixtures"] = [{**case, "actual": answer, "status": "PASS"} for case, answer in zip(cases, answers)]
        print(f"PASS {label}: {len(cases)} cases ({elapsed:.3f} s)", flush=True)

    fixed = fixtures()
    batch("boundary fixtures", fixed, True)
    batch("independent randomized oracles", random_cases())
    batch("repeated mixed requests", fixed + list(reversed(fixed)) + fixed)
    # Separate task batches reproduce the official process/timeout boundary.
    stress = scale_cases()
    for task in ("lcs", "merge", "tree", "naive", "kmp", "rk", "suffix", "aho"):
        cases = [case for case in stress if case["input"]["task"] == task]
        # Keep every JSONL output batch below 8 MiB, including the largest matrix.
        batch("scale " + task, cases)
    report["total_cases"] = sum(item["cases"] for item in report["batches"])
    report["status"] = "PASS"
    destination = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "tests/student/results.json"
    destination.write_text(json.dumps(report, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    print(f"PASS total: {report['total_cases']} cases; seed={SEED}")


if __name__ == "__main__":
    main()
