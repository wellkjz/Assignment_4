import java.util.*;

/** Assignment 4. All task data is local, so consecutive requests are independent. */
public final class Algorithms {

    // 1. Longest common subsequence: prefix DP and backward reconstruction.
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a");
        String b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        StringBuilder sequence = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sequence.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--; // On a tie, either optimal direction is valid.
            } else {
                j--;
            }
        }
        return Map.of("length", dp[n][m], "sequence", sequence.reverse().toString());
    }

    // 2. Ordered merging: try every possible last split of each interval.
    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> sizes = (List<?>) input.get("sizes");
        int n = sizes.size();
        List<int[]> plan = new ArrayList<>();
        if (n <= 1) return Map.of("cost", 0L, "plan", plan);

        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + ((Number) sizes.get(i)).longValue();
        }
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) {
            for (int left = 0; left + length <= n; left++) {
                int right = left + length - 1;
                dp[left][right] = Long.MAX_VALUE;
                long sum = prefix[right + 1] - prefix[left];
                for (int k = left; k < right; k++) {
                    long cost = dp[left][k] + dp[k + 1][right] + sum;
                    if (cost < dp[left][right]) {
                        dp[left][right] = cost;
                        split[left][right] = k;
                    }
                }
            }
        }
        buildPlan(0, n - 1, split, plan);
        return Map.of("cost", dp[0][n - 1], "plan", plan);
    }

    // At most 200 blocks: this small recursion is safe.
    private static void buildPlan(int left, int right, int[][] split, List<int[]> plan) {
        if (left >= right) return;
        int k = split[left][right];
        buildPlan(left, k, split, plan);
        buildPlan(k + 1, right, split, plan);
        plan.add(new int[]{left, k, right}); // Children must be merged first.
    }

    // 3. Tree DP without recursion, including reconstruction.
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        List<?> weights = (List<?>) input.get("weights");
        List<?> edges = (List<?>) input.get("edges");
        List<Integer> selected = new ArrayList<>();
        if (n == 0) return Map.of("weight", 0L, "selected", selected);

        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
        for (Object item : edges) {
            List<?> edge = (List<?>) item;
            int u = ((Number) edge.get(0)).intValue();
            int v = ((Number) edge.get(1)).intValue();
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        // order is a queue: each parent appears before its children.
        int[] parent = new int[n];
        int[] order = new int[n];
        Arrays.fill(parent, -1);
        order[0] = 0;
        int count = 1;
        for (int i = 0; i < count; i++) {
            int v = order[i];
            for (int child : graph.get(v)) {
                if (child != parent[v]) {
                    parent[child] = v;
                    order[count++] = child;
                }
            }
        }

        long[] take = new long[n];
        long[] skip = new long[n];
        for (int i = n - 1; i >= 0; i--) {
            int v = order[i];
            take[v] = ((Number) weights.get(v)).longValue();
            for (int child : graph.get(v)) {
                if (parent[child] == v) {
                    take[v] += skip[child];
                    skip[v] += Math.max(take[child], skip[child]);
                }
            }
        }

        boolean[] chosen = new boolean[n];
        for (int i = 0; i < n; i++) {
            int v = order[i];
            boolean parentChosen = parent[v] != -1 && chosen[parent[v]];
            if (!parentChosen && take[v] > skip[v]) {
                chosen[v] = true;
                selected.add(v);
            }
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    // 4a. Compare the pattern at every possible start, including overlaps.
    static Map<String, Object> naive(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        for (int start = 0; start <= n - m; start++) {
            int j = 0;
            while (j < m && text.charAt(start + j) == pattern.charAt(j)) j++;
            if (j == m) positions.add(start);
        }
        return Map.of("positions", positions);
    }

    // 4b. KMP: build the prefix function, then scan the text once.
    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        if (m == 0) return emptyPattern(n);

        int[] pi = new int[m];
        for (int i = 1; i < m; i++) {
            int j = pi[i - 1];
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            pi[i] = j;
        }

        List<Integer> positions = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) {
                positions.add(i - m + 1);
                j = pi[j - 1]; // Keep an overlapping match possible.
            }
        }
        return Map.of("positions", positions);
    }

    private static Map<String, Object> emptyPattern(int n) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i <= n; i++) positions.add(i);
        return Map.of("positions", positions);
    }

    // 4c. Exact Rabin-Karp: the required base is 31 and modulus is 101.
    static Map<String, Object> rk(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        if (m == 0) return emptyPattern(n);
        List<Integer> positions = new ArrayList<>();
        if (m > n) return Map.of("positions", positions);

        long base = 31, mod = 101;
        long power = 1, patternHash = 0, windowHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * base + pattern.charAt(i)) % mod;
            windowHash = (windowHash * base + text.charAt(i)) % mod;
            if (i < m - 1) power = power * base % mod;
        }
        for (int start = 0; start <= n - m; start++) {
            if (patternHash == windowHash) {
                int j = 0;
                while (j < m && text.charAt(start + j) == pattern.charAt(j)) j++;
                if (j == m) positions.add(start);
            }
            if (start < n - m) {
                windowHash = (windowHash - text.charAt(start) * power % mod + mod) % mod;
                windowHash = (windowHash * base + text.charAt(start + m)) % mod;
            }
        }
        return Map.of("positions", positions);
    }

    // 5. Prefix doubling and Kasai LCP. Never create all suffix strings.
    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        Integer[] sa = new Integer[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i);
        }

        for (int length = 1; length < n; length *= 2) {
            final int step = length;
            final int[] oldRank = rank;
            // Compare two integer pairs instead of comparing whole suffixes.
            Arrays.sort(sa, (a, b) -> {
                if (oldRank[a] != oldRank[b]) return Integer.compare(oldRank[a], oldRank[b]);
                int secondA = a + step < n ? oldRank[a + step] : -1;
                int secondB = b + step < n ? oldRank[b + step] : -1;
                return Integer.compare(secondA, secondB);
            });

            int[] newRank = new int[n];
            for (int i = 1; i < n; i++) {
                int a = sa[i - 1], b = sa[i];
                int secondA = a + step < n ? rank[a + step] : -1;
                int secondB = b + step < n ? rank[b + step] : -1;
                newRank[b] = newRank[a];
                if (rank[a] != rank[b] || secondA != secondB) newRank[b]++;
            }
            rank = newRank;
            if (rank[sa[n - 1]] == n - 1) break;
        }

        int[] position = new int[n];
        int[] lcp = new int[n];
        for (int i = 0; i < n; i++) position[sa[i]] = i;
        int common = 0, bestLength = 0, bestStart = 0;
        for (int i = 0; i < n; i++) {
            int place = position[i];
            if (place == 0) {
                common = 0;
                continue;
            }
            int j = sa[place - 1];
            while (i + common < n && j + common < n
                    && text.charAt(i + common) == text.charAt(j + common)) common++;
            lcp[place] = common;
            if (common > bestLength) {
                bestLength = common;
                bestStart = i;
            }
            if (common > 0) common--;
        }
        return Map.of("sa", sa, "lcp", lcp,
                "repeated", text.substring(bestStart, bestStart + bestLength));
    }

    // 6. Aho-Corasick with reverse failure-link count propagation.
    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> patterns = (List<?>) input.get("patterns");
        List<?> texts = (List<?>) input.get("texts");
        List<int[]> next = new ArrayList<>(); // One row per actual trie state.
        next.add(new int[95]); // Printable ASCII: character - 32.
        int[] terminal = new int[patterns.size()];
        // State 0 is the root; zero initially means a missing edge.
        for (int id = 0; id < patterns.size(); id++) {
            String pattern = (String) patterns.get(id);
            int state = 0;
            for (int i = 0; i < pattern.length(); i++) {
                int c = pattern.charAt(i) - 32;
                if (next.get(state)[c] == 0) {
                    next.get(state)[c] = next.size();
                    next.add(new int[95]);
                }
                state = next.get(state)[c];
            }
            terminal[id] = state; // Duplicates naturally share a terminal state.
        }

        int states = next.size();
        int[] fail = new int[states];
        int[] queue = new int[states];
        int head = 0, tail = 0;
        for (int c = 0; c < 95; c++) {
            if (next.get(0)[c] != 0) queue[tail++] = next.get(0)[c];
        }
        while (head < tail) {
            int state = queue[head++];
            for (int c = 0; c < 95; c++) {
                int child = next.get(state)[c];
                if (child != 0) {
                    fail[child] = next.get(fail[state])[c];
                    queue[tail++] = child;
                } else {
                    next.get(state)[c] = next.get(fail[state])[c];
                }
            }
        }

        List<long[]> counts = new ArrayList<>();
        for (Object item : texts) {
            String text = (String) item;
            long[] visits = new long[states];
            int state = 0; // Every text starts a fresh search.
            for (int i = 0; i < text.length(); i++) {
                state = next.get(state)[text.charAt(i) - 32];
                visits[state]++;
            }
            for (int i = tail - 1; i >= 0; i--) {
                int v = queue[i];
                visits[fail[v]] += visits[v];
            }
            long[] row = new long[patterns.size()];
            for (int id = 0; id < row.length; id++) row[id] = visits[terminal[id]];
            counts.add(row);
        }
        return Map.of("counts", counts);
    }
}
