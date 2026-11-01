import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Algorithms for the eight JSON Lines commands in the assignment contract. */
public final class Algorithms {
    private Algorithms() {}

    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a");
        String b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        StringBuilder sequence = new StringBuilder(dp[n][m]);
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sequence.append(a.charAt(--i));
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--; // On a tie, discard from a.
            } else {
                j--;
            }
        }
        return Map.of("length", dp[n][m], "sequence", sequence.reverse().toString());
    }

    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> sizes = (List<?>) input.get("sizes");
        int n = sizes.size();
        List<int[]> plan = new ArrayList<>();
        if (n < 2) return Map.of("cost", 0L, "plan", plan);
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + ((Number) sizes.get(i)).longValue();
        }
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) {
            for (int l = 0; l + length <= n; l++) {
                int r = l + length - 1;
                long best = Long.MAX_VALUE;
                for (int k = l; k < r; k++) {
                    long candidate = dp[l][k] + dp[k + 1][r] + prefix[r + 1] - prefix[l];
                    if (candidate < best) {
                        best = candidate;
                        split[l][r] = k; // Keep the first (smallest) optimal split.
                    }
                }
                dp[l][r] = best;
            }
        }
        // Explicit postorder: expand both children before outputting their parent.
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{0, n - 1, 0});
        while (!stack.isEmpty()) {
            int[] frame = stack.remove(stack.size() - 1);
            int l = frame[0], r = frame[1];
            if (l == r) continue;
            int k = split[l][r];
            if (frame[2] == 1) {
                plan.add(new int[]{l, k, r});
            } else {
                stack.add(new int[]{l, r, 1});
                stack.add(new int[]{k + 1, r, 0});
                stack.add(new int[]{l, k, 0});
            }
        }
        return Map.of("cost", dp[0][n - 1], "plan", plan);
    }

    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        List<Integer> selected = new ArrayList<>();
        if (n == 0) return Map.of("weight", 0L, "selected", selected);
        List<?> weights = (List<?>) input.get("weights");
        List<?> edges = (List<?>) input.get("edges");
        int[] head = new int[n];
        Arrays.fill(head, -1);
        int[] to = new int[2 * edges.size()];
        int[] next = new int[to.length];
        int count = 0;
        for (Object item : edges) {
            List<?> edge = (List<?>) item;
            int u = ((Number) edge.get(0)).intValue();
            int v = ((Number) edge.get(1)).intValue();
            to[count] = v; next[count] = head[u]; head[u] = count++;
            to[count] = u; next[count] = head[v]; head[v] = count++;
        }
        int[] parent = new int[n];
        int[] order = new int[n];
        parent[0] = -1;
        int size = 1;
        for (int index = 0; index < size; index++) {
            int u = order[index];
            for (int e = head[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (v != parent[u]) {
                    parent[v] = u;
                    order[size++] = v;
                }
            }
        }
        long[] take = new long[n], skip = new long[n];
        for (int u = 0; u < n; u++) take[u] = ((Number) weights.get(u)).longValue();
        for (int index = n - 1; index > 0; index--) {
            int u = order[index], p = parent[u];
            take[p] += skip[u];
            skip[p] += Math.max(take[u], skip[u]);
        }
        boolean[] chosen = new boolean[n];
        for (int u : order) {
            chosen[u] = (parent[u] == -1 || !chosen[parent[u]]) && take[u] > skip[u];
            if (chosen[u]) selected.add(u); // Skip on ties, including zero/negative weights.
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    static Map<String, Object> naive(Map<String, Object> input) {
        return Map.of("positions", naivePositions((String) input.get("text"), (String) input.get("pattern")));
    }

    static List<Integer> naivePositions(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        for (int start = 0; start <= n - m; start++) {
            int j = 0;
            while (j < m && text.charAt(start + j) == pattern.charAt(j)) j++;
            if (j == m) positions.add(start);
        }
        return positions;
    }

    static Map<String, Object> kmp(Map<String, Object> input) {
        return Map.of("positions", kmpPositions((String) input.get("text"), (String) input.get("pattern")));
    }

    static List<Integer> kmpPositions(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) positions.add(i);
            return positions;
        }
        if (m > n) return positions;
        int[] pi = new int[m];
        for (int i = 1, j = 0; i < m; i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            pi[i] = j;
        }
        for (int i = 0, j = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) {
                positions.add(i - m + 1);
                j = pi[j - 1]; // Preserve overlapping occurrences.
            }
        }
        return positions;
    }

    static Map<String, Object> rk(Map<String, Object> input) {
        return Map.of("positions", rkPositions((String) input.get("text"), (String) input.get("pattern")));
    }

    static List<Integer> rkPositions(String text, String pattern) {
        final long base = 31, modulus = 101;
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) positions.add(i);
            return positions;
        }
        if (m > n) return positions;
        long power = 1, wanted = 0, window = 0;
        for (int i = 0; i < m; i++) {
            wanted = (wanted * base + pattern.charAt(i)) % modulus;
            window = (window * base + text.charAt(i)) % modulus;
            if (i + 1 < m) power = power * base % modulus;
        }
        for (int start = 0; start <= n - m; start++) {
            if (window == wanted) {
                int j = 0;
                while (j < m && text.charAt(start + j) == pattern.charAt(j)) j++;
                if (j == m) positions.add(start);
            }
            if (start < n - m) {
                window = (window - text.charAt(start) * power) % modulus;
                if (window < 0) window += modulus;
                window = (window * base + text.charAt(start + m)) % modulus;
            }
        }
        return positions;
    }

    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        int[] sa = new int[n], lcp = new int[n];
        if (n == 0) return Map.of("sa", sa, "lcp", lcp, "repeated", "");
        int[] rank = new int[n], newRank = new int[n], buffer = new int[n];
        int[] counts = new int[Math.max(n + 1, 128)];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i);
        }
        for (int width = 1; width < n; width *= 2) {
            // Stable LSD radix sort by second rank, then first rank.
            // Missing second halves have key 0; actual ranks have key rank+1.
            sortRanks(sa, buffer, rank, width, counts);
            sortRanks(buffer, sa, rank, 0, counts);
            int classes = 1;
            newRank[sa[0]] = 0;
            for (int i = 1; i < n; i++) {
                int a = sa[i - 1], b = sa[i];
                if (rank[a] != rank[b]
                        || (a + width < n ? rank[a + width] : -1)
                        != (b + width < n ? rank[b + width] : -1)) classes++;
                newRank[b] = classes - 1;
            }
            int[] swap = rank; rank = newRank; newRank = swap;
            if (classes == n) break;
        }
        // Kasai with the previous suffix in sa order.
        int[] inverse = new int[n];
        for (int i = 0; i < n; i++) inverse[sa[i]] = i;
        int common = 0, best = 0, bestStart = 0;
        for (int i = 0; i < n; i++) {
            int position = inverse[i];
            if (position == 0) {
                common = 0;
                continue;
            }
            int j = sa[position - 1];
            while (i + common < n && j + common < n
                    && text.charAt(i + common) == text.charAt(j + common)) common++;
            lcp[position] = common;
            if (common > best) { best = common; bestStart = i; }
            if (common > 0) common--;
        }
        return Map.of("sa", sa, "lcp", lcp, "repeated", text.substring(bestStart, bestStart + best));
    }

    private static void sortRanks(int[] source, int[] target, int[] rank, int offset, int[] counts) {
        Arrays.fill(counts, 0);
        int n = rank.length;
        for (int i : source) counts[i + offset < n ? rank[i + offset] + 1 : 0]++;
        int sum = 0;
        for (int key = 0; key < counts.length; key++) {
            int frequency = counts[key];
            counts[key] = sum;
            sum += frequency;
        }
        for (int i : source) {
            int key = i + offset < n ? rank[i + offset] + 1 : 0;
            target[counts[key]++] = i;
        }
    }

    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> patterns = (List<?>) input.get("patterns");
        List<?> texts = (List<?>) input.get("texts");
        int p = patterns.size(), q = texts.size();
        long[][] result = new long[q][p];
        if (p == 0 || q == 0) return Map.of("counts", result);
        final int alphabet = 95;
        // 0 means a missing edge during insertion; no real trie edge targets root.
        List<int[]> go = new ArrayList<>();
        go.add(new int[alphabet]);
        int[] terminal = new int[p];
        for (int index = 0; index < p; index++) {
            String pattern = (String) patterns.get(index);
            int state = 0;
            for (int i = 0; i < pattern.length(); i++) {
                int c = pattern.charAt(i) - 32;
                int[] row = go.get(state);
                if (row[c] == 0) {
                    row[c] = go.size();
                    go.add(new int[alphabet]);
                }
                state = row[c];
            }
            terminal[index] = state; // Duplicate entries share one terminal state.
        }
        int states = go.size();
        int[] failure = new int[states], order = new int[states];
        int tail = 0;
        for (int c = 0; c < alphabet; c++) {
            if (go.get(0)[c] != 0) order[tail++] = go.get(0)[c];
        }
        for (int head = 0; head < tail; head++) {
            int state = order[head];
            int[] row = go.get(state), fallback = go.get(failure[state]);
            for (int c = 0; c < alphabet; c++) {
                int child = row[c];
                if (child == 0) {
                    row[c] = fallback[c];
                } else {
                    failure[child] = fallback[c];
                    order[tail++] = child;
                }
            }
        }
        long[] visits = new long[states];
        for (int index = 0; index < q; index++) {
            Arrays.fill(visits, 0);
            int state = 0;
            String text = (String) texts.get(index);
            for (int i = 0; i < text.length(); i++) {
                state = go.get(state)[text.charAt(i) - 32];
                visits[state]++;
            }
            // Aggregate suffix matches without enumerating dictionary occurrences.
            for (int i = tail - 1; i >= 0; i--) {
                int u = order[i];
                visits[failure[u]] += visits[u];
            }
            for (int i = 0; i < p; i++) result[index][i] = visits[terminal[i]];
        }
        return Map.of("counts", result);
    }
}
