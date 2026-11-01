import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Direct calls to the same search methods used by the JSONL application. */
public final class SearchBenchmark {
    private static final int REPETITIONS = 50;
    private static volatile Object sink;

    private static List<Integer> run(int algorithm, String text, String pattern) {
        return switch (algorithm) {
            case 0 -> Algorithms.naivePositions(text, pattern);
            case 1 -> Algorithms.kmpPositions(text, pattern);
            default -> Algorithms.rkPositions(text, pattern);
        };
    }

    private static int hash(String text, int start, int length) {
        int value = 0;
        for (int i = 0; i < length; i++) value = (31 * value + text.charAt(start + i)) % 101;
        return value;
    }

    private static void measure(String kind, int n) {
        int m = n / 3;
        long seed = 20261101L + n;
        String text, pattern;
        if (kind.equals("random")) {
            Random random = new Random(seed);
            StringBuilder builder = new StringBuilder(n);
            for (int i = 0; i < n; i++) builder.append((char) (32 + random.nextInt(95)));
            text = builder.toString();
            pattern = text.substring(n / 3, n / 3 + m); // Sample at the fixed start n/3.
        } else {
            text = "a".repeat(n);
            pattern = kind.equals("repetitive-miss") ? "a".repeat(m - 1) + "b"
                    : "a".repeat(m - 2) + "bB"; // hash(aa) == hash(bB).
        }
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i <= n - m; i++) if (text.startsWith(pattern, i)) expected.add(i);
        int candidates = 0, wanted = hash(pattern, 0, m);
        for (int i = 0; i <= n - m; i++) if (hash(text, i, m) == wanted) candidates++;
        for (int algorithm = 0; algorithm < 3; algorithm++) {
            if (!run(algorithm, text, pattern).equals(expected)) throw new AssertionError("Incorrect search");
            for (int i = 0; i < 100; i++) sink = run(algorithm, text, pattern);
        }
        long[][] raw = new long[3][3];
        for (int round = 0; round < 3; round++) {
            for (int order = 0; order < 3; order++) {
                int algorithm = (round + order) % 3;
                long start = System.nanoTime();
                for (int i = 0; i < REPETITIONS; i++) sink = run(algorithm, text, pattern);
                raw[algorithm][round] = System.nanoTime() - start;
            }
        }
        String[] names = {"naive", "kmp", "rk"};
        for (int algorithm = 0; algorithm < 3; algorithm++) {
            long[] sorted = raw[algorithm].clone();
            Arrays.sort(sorted);
            System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%s,%d,%d,%d,%d,%d,%d,%d,%.3f,PASS%n",
                    kind, n, m, seed, names[algorithm], candidates, expected.size(), REPETITIONS,
                    raw[algorithm][0], raw[algorithm][1], raw[algorithm][2], sorted[1],
                    sorted[1] / (1000.0 * REPETITIONS));
        }
    }

    public static void main(String[] args) {
        // Warm each implementation before the nine measured scenarios.
        Random random = new Random(20261101L);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 128; i++) builder.append((char) (32 + random.nextInt(95)));
        String randomText = builder.toString(), randomPattern = randomText.substring(40, 72);
        for (int i = 0; i < 10000; i++) {
            for (int algorithm = 0; algorithm < 3; algorithm++) {
                sink = run(algorithm, "a".repeat(128), "a".repeat(30) + "bB");
                sink = run(algorithm, "abc abc abc", "abc");
                sink = run(algorithm, randomText, randomPattern);
            }
        }
        System.out.println("case,n,m,seed,algorithm,hash_candidates,matches,repetitions,run1_ns,run2_ns,run3_ns,median_ns,median_us_per_call,correctness");
        for (String kind : new String[]{"random", "repetitive-miss", "repetitive-collision"}) {
            for (int n : new int[]{600, 1800, 3000}) measure(kind, n);
        }
    }
}
