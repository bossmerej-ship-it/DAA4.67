# Student tests and benchmark

Run from the repository root with Python 3.12+ and JDK 17+:

    python tests/student/run_tests.py
    python tests/student/benchmark.py

run_tests.py compiles the selected Java implementation. It checks 71 fixed fixtures, 1,460 independently generated small cases, 213 repeated mixed requests, and 28 full-domain scale cases: **1,772 total**. Seed: 20261101. Each subprocess has a 20-second timeout.

Small expected answers use exhaustive subsequences, actual adjacent merge choices, independent vertex subsets, direct built-in matching in the oracle only, and short suffix-string sorting. The supplied public validator checks output schemas and witnesses, accepting alternative optima. Large cases check known optima, plans, selected vertices, exact search positions, suffix arrays/LCP and count matrices. All printable ASCII characters and JSON quoting/backslashes are covered.

[results.json](results.json) contains the actual per-batch counts/times and every fixed fixture's input, expected result and actual result. An optional first argument writes the report elsewhere:

    python tests/student/run_tests.py /path/to/student-results.json

The CI workflow uses a temporary report destination, so the recorded local result is preserved.

SearchBenchmark.java calls the actual three Java search methods. Each scenario is checked against String.startsWith before timing; that built-in is used only as a test oracle. The benchmark covers random printable-ASCII text, repetitive near-misses and repetitive hash collisions at three increasing sizes in the common domain.

benchmark.py compiles outside timing and launches Java with a 384 MiB heap, -Xbatch, -XX:-TieredCompilation and -XX:CompileThreshold=1000. There are 10,000 global warm-up iterations (three calls per method per iteration), 100 warm-up calls per method/scenario, and three measured rounds of 50 calls. The sample includes preprocessing, result creation and volatile result consumption; JSON I/O and input construction are excluded. Algorithm order rotates by round.

[benchmark.csv](benchmark.csv) stores each raw sample in nanoseconds per 50 calls, its median, the median microseconds per call, seeds, candidate counts and correctness. An optional first argument changes the CSV destination. Re-running updates actual measurements; it does not reproduce identical times. The committed CSV is the run summarized in [ANALYSIS.md](../../ANALYSIS.md).

[setup.txt](setup.txt) and [public-checks.txt](public-checks.txt) record the supplied setup and 44-case public suite plus its 88-request repeated mixed batch. Build products and the portable local Python runtime are not committed.
