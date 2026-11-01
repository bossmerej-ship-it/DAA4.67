# Assignment 4 — Analysis

## Environment and actual commands

The selected implementation is Java 17, configured in submission.json. Local verification and measurements were performed on 1 November 2026 using Oracle JDK / HotSpot 17.0.12, javac 17.0.12, Python 3.12.10, Windows 11 Pro x64 (10.0.26200), an AMD Ryzen 7 7435HS (8 cores, 16 logical processors), and 16,849,264,640 bytes of physical RAM (approximately 15.69 GiB). The program is single-threaded. The launcher limits the Java heap to 384 MiB. No measured peak-memory claim is made.

Commands from the repository root:

    python tools/run.py --setup
    python tools/run.py --check
    python tools/run.py --input examples/lcs.jsonl
    python tests/student/run_tests.py
    python tests/student/benchmark.py

In the local Windows run, the portable Python 3.12.10 executable was invoked as ../runtime/python312/python.exe; that runtime is outside the repository. The commands above use python for a normal installation. The launcher compiles all Java/source/*.java with javac --release 17 -encoding UTF-8 into Java/build, then runs java -Xmx384m -cp Java/build Main. Build artifacts are ignored by Git. No external Java libraries are required.

The original supplied JSON parser/serializer remains in Json.java. Main dispatches one task per line and prints exactly one JSON object. Algorithm state belongs to the current request. Numeric input values are read through Number; accumulated costs, tree weights and Aho counts use long.

## 1. LCS

Let dp[i][j] be the maximum common-subsequence length of the prefixes a[0..i) and b[0..j). The row and column at index zero are zero. If the final prefix characters match, dp[i][j] = dp[i-1][j-1] + 1; otherwise dp[i][j] = max(dp[i-1][j], dp[i][j-1]). Rows and columns increase, so all dependencies have already been computed.

For correctness, when equal last characters are available, an optimal common subsequence can end with that character after an optimal solution of the two shorter prefixes. When they differ, a common subsequence cannot use both final characters, so at least one is omitted; the two possible omissions cover an optimum. Induction on prefix lengths proves every state.

Reconstruction starts at (n,m). Equal characters are appended while both indices decrease. At a mismatch the index of a maximum predecessor decreases; ties discard from a. The collected characters are reversed. Each step preserves the remaining optimum length, and chosen characters occur in both inputs in order. Either empty input yields length zero and the empty sequence.

Time and auxiliary DP storage are O((n+1)(m+1)); reconstruction is O(n+m). Input storage is O(n+m), and the returned sequence uses O(length+1) output storage. The builder also uses O(length+1) auxiliary space, dominated by the DP table. The tea_time/team fixture detects confusing a subsequence with a substring; ab/ba checks a non-unique optimum.

## 2. Ordered adjacent-block merging

Let dp[l][r] be the minimum cost of merging the closed original interval [l,r] into one block. A singleton costs zero. Prefix sums give weight(l,r) in O(1). For l<r:

    dp[l][r] = min over l <= k < r of
               dp[l][k] + dp[k+1][r] + prefix[r+1] - prefix[l]

Intervals are processed by increasing length. Every valid final merge joins two adjacent intervals separated by some k. Its cost is the sum of both optimal child costs and the combined interval size. Conversely, combining two optimal child plans gives a valid plan for that split. This proves the recurrence by induction on interval length.

The smallest optimal k is stored, since updates occur only on strict improvement. An explicit stack produces a postorder traversal of these splits: left child, right child, then [l,k,r]. Thus both operands exist at the time a merge is emitted. The plan has exactly n-1 operations. Empty and singleton inputs return cost zero and no operations.

For n>=2, time is O(n^3), auxiliary tables are O(n^2), and the reconstruction stack and output are O(n). The n<=1 cases take O(1). Input sizes use O(n) storage. All sums and costs use long: 200 maximum-size blocks can exceed 32-bit arithmetic. The [1,100,1] fixture has optimum 203; merging the two nonadjacent ones would incorrectly give 104. Plan validation simulates the current adjacent intervals and recomputes the full cost.

## 3. Maximum-weight independent set on a tree

Root the tree at vertex zero. Compact adjacency arrays store each undirected edge twice. An iterative traversal records parent[u] and a parent-before-child order. For the subtree rooted at u:

    take[u] = weights[u] + sum(skip[v]) over children v
    skip[u] = sum(max(take[v], skip[v])) over children v

Reverse traversal order processes children before their parents. Arrays begin with take[u]=weights[u] and skip[u]=0. Each completed child's values are added to its parent. If u is selected, each child must be skipped; if u is skipped, the child subtrees are independent and may choose their better option. This establishes both states by induction. The optimum is max(take[0],skip[0]), independent of which vertex was used as root.

Reconstruction scans the recorded order forwards. A vertex is chosen exactly when its parent was not chosen and take[u]>skip[u]. Ties are skipped. If the parent was chosen, the forced skip state is used, but that vertex's children still make their own allowed decisions. Each vertex is visited once. An all-negative tree chooses the empty set, and an empty tree returns zero with an empty list.

Time and auxiliary storage are O(n+1), including reconstruction. Input adjacency/weights and the output set each require at most O(n+1) space. There is no recursion, so the 100,000-vertex chain does not depend on the Java call stack. A maximum-weight star checks a total of 99,999,000,000,000 and validates every selected ID, uniqueness, independence and the summed witness weight.

## 4. Exact single-pattern searches

All three methods return every occurrence start in increasing order, including overlaps. For an empty pattern the result is every boundary 0..n; for m>n it is empty. Character comparisons are case-sensitive over the full printable ASCII domain. Production algorithms do not call built-in substring-search methods. The common notation is n=text length, m=pattern length and z=number of returned starts.

### Naive

For every start from zero through n-m inclusive, compare pattern characters from left to right until a mismatch or all m characters match. Before comparison j, the first j characters of that window equal the pattern prefix. Therefore an emitted start is a genuine match, and testing every legal start proves completeness. Advancing the start by one preserves overlaps. The same loop handles m=0 without indexing the pattern.

Time is O(n*m+n+1), including empty-pattern output. Auxiliary storage is O(1), input storage O(n+m), and output storage O(z+1). The last-window fixture catches an exclusive upper bound; aaaaa/aaa requires [0,1,2].

### KMP

pi[i] is the length of the longest proper prefix of pattern[0..i] that is also a suffix. Prefix construction extends a current border j when characters agree; otherwise j falls back to pi[j-1] until extension is possible or j is zero. Only shorter borders can remain valid after a mismatch, and the fallback chain visits them in decreasing length, proving the prefix invariant.

During search, j is the length of the longest pattern prefix matching a suffix of the consumed text. The same border argument makes fallback safe without moving the text index backwards. When j reaches m, a full match ends at the current text character. After reporting i-m+1, setting j=pi[m-1] preserves overlapping matches. The empty-pattern case returns before pattern indexing; m>n returns before allocating pi.

Each successful extension increases j by one and each fallback decreases it. Total increases bound total decreases in both scans, giving O(n+m+z+1) time. Auxiliary storage is O(m+1), input is O(n+m), and output is O(z+1). abab abab-style fallback is exercised by the fixture text ababababacababac with pattern ababac; long near-misses and the empty pattern are also tested.

### Exact Rabin–Karp

For a length-m window, the hash is the polynomial in ASCII character codes with base 31, modulus 101, and highest power on the left. Store power=31^(m-1) mod 101. After examining a window, update:

    remaining = (window - outgoingASCII * power) % 101
    if remaining < 0: remaining += 101
    window = (remaining * 31 + incomingASCII) % 101

Removing the leading term, multiplying by the base and adding the incoming character preserves the polynomial invariant. All arithmetic uses long. Unequal hashes cannot describe equal strings. Equal hashes are only candidates: a character-by-character comparison verifies each candidate before emitting it. Thus collisions do not create false matches. Empty and oversized patterns return before forming the initial window.

For 1<=m<=n and c equal-hash windows, time is O(n+m+c*m); z<=c, so output is included. A bound covering every input, including the empty pattern, is O(n*m+n+m+1). Rolling state uses O(1) auxiliary storage; input uses O(n+m), output O(z+1). There is no deterministic linear worst-case guarantee. The ab/dj collision hashes both strings to 75; abdj/ab must return only [0]. The ~ ! ab~ ! / space-exclamation fixture returns [1,7] and exercises negative-remainder normalization.

## 5. Suffix array and Kasai LCP

### Prefix doubling

Only starts 0..n-1 are stored. Initially rank[i] is the ASCII character code. At width w, the pair (rank[i], rank[i+w]) describes the first 2w characters of suffix i; an absent second half uses rank -1. Two stable counting-sort passes sort by second rank and then first rank. Counting keys are rank+1, reserving zero for the absent half. No suffix strings are materialized.

After sorting, equal pairs receive the same new rank and distinct pairs receive increasing ranks. Induction on doubling width proves that these ranks order prefixes of length 2w. When every rank is distinct, later characters cannot change their order; otherwise doubling eventually covers the entire text. The absent-half key orders a shorter identical-prefix suffix first. A one-character text already has the correct array; empty text returns three empty results.

The counting buffer has max(n+1,128) entries, covering initial printable ASCII and all later class ranks. Each round costs O(n+1), with O(log(n+1)) rounds, giving O(n log(n+1)) time for nonempty text and O(n+1) auxiliary space. This is radix/counting sorting, not comparison sorting.

### Kasai and longest repeated substring

An inverse array gives each suffix's position in sa. For text positions i in increasing order, compare suffix i with its lexicographic predecessor, starting from the reusable common-prefix length h. Assign that length to lcp[inverse[i]]. The first suffix has LCP zero. Before moving to i+1, decrease h by one when positive.

Deleting the first character of two suffixes sharing h characters leaves a shared prefix of at least h-1. The predecessor of the new suffix is at least as suitable for this lower bound; resetting h at the first suffix covers its missing predecessor. Successful character extensions increase h, and h decreases at most once per text position. Kasai therefore uses O(n) time and O(n) inverse-array space, with O(n) output arrays.

Any repeated substring is a common prefix of two suffixes. All suffixes with that prefix form one contiguous sorted interval, which contains an adjacent pair with at least that LCP. Conversely, every positive adjacent LCP is a repeated substring. Hence max(lcp) gives a longest repeat, including overlaps. Strict improvement keeps the first maximum encountered while scanning text positions. One final substring is copied for the output; sa and lcp plus repeated use O(n+1) output space. Total suffix-command auxiliary space is O(n+1); empty input costs O(1). banana and aaaaaa check overlap, the tied-repeat fixture accepts either optimal substring, and the full ASCII fixture checks ordering and the absence of an added sentinel.

## 6. Aho–Corasick

Build one trie per request. Each actual trie state has one 95-entry transition row; rows are allocated only when a new state is created. Pattern positions store their terminal-state IDs, so duplicates share a state without losing dictionary order. The root represents the empty prefix; patterns themselves are nonempty.

BFS computes failure links. A state's failure link is the longest proper suffix of its represented prefix that is also a trie prefix. Missing root edges stay at root. For a child reached by character c, its failure is the c-transition from its parent's failure state. BFS processes shorter prefixes first, so all fallback transitions are ready. Missing transitions copy the appropriate fallback transition. This gives one constant-time transition per text character.

While scanning a text, increment visits[state] at the reached state. That state represents the longest dictionary-prefix suffix of the consumed text. Every suffix-pattern match lies on its failure chain. Processing states in reverse BFS order and adding visits[u] to visits[failure[u]] counts each such suffix occurrence once, without listing all individual matches. Reading visits[terminal[i]] then yields exactly the overlapping count for dictionary position i. This also proves equal counts for duplicate patterns.

Reset the state to root and clear visits before each independent text. Empty texts produce zero rows, an empty dictionary produces one empty row per text, and an empty texts array produces no rows. All request data is local; there are no inherited output lists or per-match enumeration.

Let L be total pattern length, p dictionary size, S actual trie states including root, and q text count. Construction takes O(L+95*S+p) time and O(95*S+p) auxiliary memory. Each text takes O(|text|+S+p), including reset, propagation and output extraction. Output storage is O(q*(p+1)+1). Parsed input storage is O(L+total text length+p+q). Allocating rows by actual states preserves the auxiliary bound even with many duplicates. Empty dictionary/text-list cases return directly with the required output shape. Tests cover he as a suffix of she, duplicate columns, repeated independent texts, a 100,000-character trie depth, and dense suffix matches.

## Own tests and actual results

The student suite uses seed 20261101. Small expected results come from independent methods: enumerating all subsequences for LCS, enumerating actual adjacent moves for merging, enumerating vertex subsets for trees, built-in startsWith-style checks for search and dictionary matching, and direct short-suffix sorting/comparison for suffix indexing. These oracle-only built-ins never replace the submitted algorithms. The supplied unmodified public validator then checks schemas, optima and witnesses. Random trees are relabeled so vertex zero is not tied to their generation root.

The fixed fixture definitions are in tests/student/run_tests.py::fixtures. Every one of their complete requests, expected results and actual outputs is saved in [results.json](tests/student/results.json). Expected results may omit non-unique witness choices; actual witnesses are checked for validity and optimality.

| Command | Fixture/input | Expected | Actual / record | Defect checked |
|---|---|---|---|---|
| lcs | tea_time, team | length 4, sequence team | PASS, team | Subsequence versus substring |
| lcs | ab, ba | length 1, either optimum | PASS, a | Non-unique reconstruction |
| merge | [1,100,1] | cost 203 | PASS, valid two-step plan | Adjacency |
| merge | four blocks of 10^9 | cost 8,000,000,000 | PASS | 32-bit overflow |
| tree | [4,9,6], path 0–1–2 | weight 10, vertices 0 and 2 | PASS | Forced child exclusion |
| tree | zero or all-negative weights | weight 0, empty set allowed | PASS | Ties and empty optimum |
| naive/kmp/rk | aaaaa, aaa | [0,1,2] | PASS for all three | Overlaps |
| naive/kmp/rk | abc, empty pattern | [0,1,2,3] | PASS for all three | Final boundary |
| rk | abdj, ab | [0] | PASS | Hash-collision rejection |
| rk | ~ ! ab~ !, pattern space-exclamation | [1,7] | PASS | Remainder normalization |
| suffix | banana | sa=[5,3,1,0,4,2], lcp=[0,1,3,0,0,2], ana | PASS in public log | Order, LCP and overlap |
| suffix | abXabYcdXcdY | valid longest repeat of length 2 | PASS, ab | Tied longest repeats |
| aho | [he,she,he,e], [sheshe,sheshe,empty] | two [2,2,2,2] rows, one zero row | PASS | Suffix matches, duplicates, reset |
| aho | empty dictionary or no texts | correctly shaped empty rows / [] | PASS | Empty dimensions |

Actual batch results are below. Times include process startup and JSON transport for that batch; they are not the direct-call search benchmark times.

| Batch | Cases | Actual seconds | Result |
|---|---:|---:|---|
| boundary fixtures | 71 | 0.254 | PASS |
| independent randomized oracles | 1460 | 0.376 | PASS |
| repeated mixed requests | 213 | 0.274 | PASS |
| scale lcs | 3 | 0.366 | PASS |
| scale merge | 3 | 0.294 | PASS |
| scale tree | 3 | 0.958 | PASS |
| scale naive | 4 | 0.268 | PASS |
| scale kmp | 4 | 0.538 | PASS |
| scale rk | 4 | 0.407 | PASS |
| scale suffix | 2 | 0.450 | PASS |
| scale aho | 5 | 0.842 | PASS |

The public setup passed. All 44 public cases passed, followed by 88 requests in the public repeated mixed batch; see [setup.txt](tests/student/setup.txt) and [public-checks.txt](tests/student/public-checks.txt). Student tests passed 1,772 cases. The 28 scale cases use published maximum lengths and check correctness as well as completion. Each student task's scale batch uses the same 20-second process timeout as the public launcher.

Scale checks include 1000-by-1000 LCS, 200-block merging (equal, zero and sparse sizes), three 100,000-vertex trees, naive patterns up to 3000, KMP text/pattern lengths 200000/100000, RK text/pattern maxima and a 20,000,000 text-pattern product, 50,000-character equal and periodic suffix inputs, and all Aho aggregate limits. Aho checks include 10,000 duplicate patterns with total length 100,000 and a 100-by-10,000 count matrix. Each output batch remains below 8 MiB.

For 200 equal merge weights, an independent closed-form optimum is (200*8 - (256-200))*10^9 = 1,544,000,000,000. A single nonzero block can be joined after merging all zero blocks, for cost 10^9. Known suffix ordering/LCP formulas validate the large repetitive strings without allocating all suffixes. Small exhaustive/random checks separately validate general behaviour.

The 19 rubric groups are covered: lcs/merge/tree core, witness and scale (9 groups); naive core, kmp core/scale and rk core (4); suffix core/witness/scale (3); aho core/edge/scale (3). These are verification results, not an official awarded mark.

## Reproducible naive / KMP / exact-RK comparison

The benchmark calls the same naivePositions, kmpPositions and rkPositions methods used by the JSONL program. Inputs are constructed before timing and are identical across methods. The three sizes are (n,m)=(600,200), (1800,600), (3000,1000), all in the common permitted domain.

Random text uses java.util.Random with seed 20261101+n; each character is 32+nextInt(95), uniform over printable ASCII. The pattern is the m-character substring starting at n/3. The repetitive-miss text is a^n and the pattern is a^(m-1)b. The repetitive-collision text is a^n and the pattern is a^(m-2)bB. The latter hashes like a^m because the two-character polynomial values for aa and bB are equal. These last two cases have no real occurrences.

Before measurement, each method's positions are compared with an independent String.startsWith oracle for that exact pair. The benchmark CSV marks all checks PASS. Candidate counts are computed outside timing by direct polynomial evaluation of each window.

Compilation and JVM startup are excluded. The benchmark runs in one JVM with:

    java -Xmx384m -Xbatch -XX:-TieredCompilation -XX:CompileThreshold=1000 -cp Java/build SearchBenchmark

The VM uses synchronous compilation with tiered compilation disabled. Each method receives 10,000 global warm-up iterations across three small cases (30,000 calls per method) and then 100 warm-up calls per scenario. Warm-up durations are excluded. There are three measured rounds; each sample contains 50 full calls, including preprocessing, result-list allocation and filling, and a volatile result assignment preventing unused-result elimination. Algorithm order rotates by round. JSON parsing/serialization, correctness checking, input generation, candidate counting and CSV printing are outside every timing interval. No explicit GC is requested; ordinary allocation/GC and OS scheduling can affect times.

Raw sample durations use System.nanoTime and are **nanoseconds per 50 calls**. The median is the middle raw sample; the final column divides it by 50,000 to give microseconds per call. The complete machine-readable data, including candidate counts and matches, is [benchmark.csv](tests/student/benchmark.csv). Reproduce with python tests/student/benchmark.py; timings vary by machine and JVM.

| Case | n | m | Seed | Method | Run 1 ns | Run 2 ns | Run 3 ns | Median ns | Median µs/call | Checked |
|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| random | 600 | 200 | 20261701 | naive | 63300 | 77400 | 64500 | 64500 | 1.290 | PASS |
| random | 600 | 200 | 20261701 | kmp | 171900 | 310000 | 172400 | 172400 | 3.448 | PASS |
| random | 600 | 200 | 20261701 | rk | 445100 | 393200 | 394600 | 394600 | 7.892 | PASS |
| random | 1800 | 600 | 20262901 | naive | 206200 | 195100 | 192100 | 195100 | 3.902 | PASS |
| random | 1800 | 600 | 20262901 | kmp | 506400 | 734100 | 680800 | 680800 | 13.616 | PASS |
| random | 1800 | 600 | 20262901 | rk | 1295800 | 1694600 | 1215800 | 1295800 | 25.916 | PASS |
| random | 3000 | 1000 | 20264101 | naive | 299500 | 307200 | 296500 | 299500 | 5.990 | PASS |
| random | 3000 | 1000 | 20264101 | kmp | 837700 | 896000 | 908500 | 896000 | 17.920 | PASS |
| random | 3000 | 1000 | 20264101 | rk | 2061900 | 2264000 | 1988500 | 2061900 | 41.238 | PASS |
| repetitive-miss | 600 | 200 | 20261701 | naive | 6202900 | 6344700 | 6264400 | 6264400 | 125.288 | PASS |
| repetitive-miss | 600 | 200 | 20261701 | kmp | 307900 | 348100 | 323800 | 323800 | 6.476 | PASS |
| repetitive-miss | 600 | 200 | 20261701 | rk | 336700 | 337000 | 337600 | 337000 | 6.740 | PASS |
| repetitive-miss | 1800 | 600 | 20262901 | naive | 68283400 | 59594000 | 57550800 | 59594000 | 1191.880 | PASS |
| repetitive-miss | 1800 | 600 | 20262901 | kmp | 1043600 | 996300 | 927400 | 996300 | 19.926 | PASS |
| repetitive-miss | 1800 | 600 | 20262901 | rk | 1052200 | 1035600 | 1034200 | 1035600 | 20.712 | PASS |
| repetitive-miss | 3000 | 1000 | 20264101 | naive | 174034700 | 177489100 | 164114900 | 174034700 | 3480.694 | PASS |
| repetitive-miss | 3000 | 1000 | 20264101 | kmp | 1929500 | 1771700 | 1567100 | 1771700 | 35.434 | PASS |
| repetitive-miss | 3000 | 1000 | 20264101 | rk | 1831400 | 1775800 | 1870900 | 1831400 | 36.628 | PASS |
| repetitive-collision | 600 | 200 | 20261701 | naive | 6427000 | 6374400 | 6268900 | 6374400 | 127.488 | PASS |
| repetitive-collision | 600 | 200 | 20261701 | kmp | 293500 | 323100 | 311900 | 311900 | 6.238 | PASS |
| repetitive-collision | 600 | 200 | 20261701 | rk | 21481100 | 21408000 | 21287500 | 21408000 | 428.160 | PASS |
| repetitive-collision | 1800 | 600 | 20262901 | naive | 55928700 | 72139100 | 68869600 | 68869600 | 1377.392 | PASS |
| repetitive-collision | 1800 | 600 | 20262901 | kmp | 911900 | 1016600 | 4015200 | 1016600 | 20.332 | PASS |
| repetitive-collision | 1800 | 600 | 20262901 | rk | 197501800 | 209541600 | 203809100 | 203809100 | 4076.182 | PASS |
| repetitive-collision | 3000 | 1000 | 20264101 | naive | 165697700 | 167539500 | 167055400 | 167055400 | 3341.108 | PASS |
| repetitive-collision | 3000 | 1000 | 20264101 | kmp | 1715300 | 1526700 | 1580100 | 1580100 | 31.602 | PASS |
| repetitive-collision | 3000 | 1000 | 20264101 | rk | 573103600 | 582398600 | 585976700 | 582398600 | 11647.972 | PASS |

Random inputs usually reject windows at their first mismatching character, so naive search can have a low constant cost. Samples vary between rounds despite identical inputs. Warm-up and synchronous compilation reduce startup effects, but scheduling, allocation and cache effects remain. These short measurements must not be used to infer an asymptotic growth law.

On repetitive near-misses, naive search repeatedly rechecks long prefixes, while KMP reuses borders. RK rejects every window in the plain repetitive-miss case (c=0). In the deliberate collision case, every one of the n-m+1 windows has the pattern hash, and character verification scans almost the whole pattern before rejecting it. RK therefore demonstrates its candidate-sensitive worst case rather than behaving linearly. It still returns the exact correct empty result.

The random scenarios have 3, 15 and 22 hash candidates but only one actual occurrence each, illustrating the small modulus's false candidates. A modulus of 101 is a collision laboratory required by this assignment; these timings do not characterize all rolling-hash implementations or production hash choices. Correctness follows from the proofs and character verification, not from the timing medians.

## Sources and verification

The [official assignment template](https://github.com/RZhaniya/daa-assignment-4-archive) supplies the JSON transport, launcher, public fixtures, validator, specifications and rubric. The local [contracts](docs/guide/CONTRACTS.md) determine domains and output schemas; the [algorithm clarifications](docs/LECTURE_CLARIFICATIONS_EN_RU.md) provide the course conventions for DP, borders, hash collisions, suffix/LCP indexing and failure links.

The selected Java algorithm methods implement those specified techniques directly using standard arrays and collections. Main was reformatted around the supplied task dispatch and transport. Student tests add independent small oracles, witness validation, deterministic mixed batches and large cases. Recorded outputs and timings were produced by the committed test/benchmark programs on the environment described above; no measurements were inferred from theoretical complexity.
