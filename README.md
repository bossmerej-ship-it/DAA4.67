# Assignment 4 — Dynamic Programming and String Processing

Selected language: **Java 17** (see [submission.json](submission.json)).

The implementation provides all eight commands through the supplied UTF-8 JSON Lines interface. Each input line produces one JSON result line; requests can be mixed in the same process.

| Command | Implementation |
|---|---|
| lcs | Prefix DP and reconstruction of an optimal subsequence |
| merge | Interval DP, 64-bit costs and a chronological adjacent-merge plan |
| tree | Iterative take/skip DP and independent-set reconstruction |
| naive | Direct character comparisons, including overlapping matches |
| kmp | Prefix function, mismatch fallback and overlapping matches |
| rk | Base 31, modulus 101, normalized rolling hash and character verification |
| suffix | Prefix doubling with stable counting sorts, Kasai LCP and a longest repeat |
| aho | Trie, BFS failure links and reverse-failure count aggregation |

## Build and run

Install JDK 17+ and Python 3.12+. Run from the repository root:

    python tools/run.py --setup
    python tools/run.py --check
    python tools/run.py --input examples/lcs.jsonl

To build and launch Java directly:

    javac --release 17 -encoding UTF-8 -d Java/build Java/source/Json.java Java/source/Algorithms.java Java/source/Main.java
    java -Xmx384m -cp Java/build Main

The launcher creates Java/build automatically. Before the direct build, create that directory if it does not exist.

## Verification and comparison

    python tests/student/run_tests.py
    python tests/student/benchmark.py

The public suite passes all 44 cases and the repeated 88-request mixed batch. The student suite passes 1,774 checks: boundary fixtures, independent randomized oracles, repeated mixed requests and full-domain stress. Tests accept alternative optimal witnesses and validate reconstruction. The GitHub Actions workflow also runs the student suite.

[ANALYSIS.md](ANALYSIS.md) contains correctness arguments, reconstruction, time and memory bounds, actual test results, and the search comparison. Raw benchmark samples are in [benchmark.csv](tests/student/benchmark.csv), with runtime information and file digests in [benchmark.metadata.json](tests/student/benchmark.metadata.json); fixed request/expected/actual records are in [results.json](tests/student/results.json). See [student test instructions](tests/student/README.md) for generators and timing details.

Only Java is the selected solution; the other language directories retain the template starters.

## Assignment references and submission

[Official template](https://github.com/RZhaniya/daa-assignment-4-archive) · [English specification](docs/Assignment_4_EN.md) · [Contracts](docs/guide/CONTRACTS.md) · [Rubric](docs/guide/RUBRIC.md) · [Interactive lesson and practice](https://rzhaniya.github.io/daa-assignment-4-archive/)

For Moodle Online text, provide the exact GitHub commit URL with its full 40-character SHA and the chosen language, **Java**. The Actions run URL for the same commit is optional. Click **Submit assignment** and check the final submission status. The practical repository work and the individual defence are separate components.
