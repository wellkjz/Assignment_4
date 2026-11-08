import java.util.*;

/** Direct algorithm timing; compile separately from the submitted source files. */
public class Benchmark {
    static volatile Object sink; // Retain each result so the JVM cannot discard the work.

    static Map<String, Object> search(int method, Map<String, Object> input) {
        if (method == 0) return Algorithms.naive(input);
        if (method == 1) return Algorithms.kmp(input);
        return Algorithms.rk(input);
    }

    public static void main(String[] args) {
        long seed = 20261108L;
        String[] methods = {"naive", "kmp", "rk"};
        int[] sizes = {500, 1500, 3000};
        List<Map<String, Object>> cases = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (String kind : new String[]{"random", "repetitive"}) {
            for (int n : sizes) {
                int m = n / 10;
                String text, pattern;
                if (kind.equals("random")) {
                    Random rng = new Random(seed + n);
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < n; i++) builder.append((char) ('a' + rng.nextInt(4)));
                    text = builder.toString();
                    pattern = text.substring(n / 2, n / 2 + m);
                } else {
                    text = "a".repeat(n);
                    pattern = "a".repeat(m);
                }
                Map<String, Object> input = Map.of("text", text, "pattern", pattern);
                List<Integer> expected = new ArrayList<>();
                // Built-in matching is only an independent test oracle, never a solution.
                for (int i = 0; i <= n - m; i++) {
                    if (text.startsWith(pattern, i)) expected.add(i);
                }
                for (int method = 0; method < 3; method++) {
                    if (!search(method, input).get("positions").equals(expected)) {
                        throw new AssertionError("Incorrect output before timing");
                    }
                }
                cases.add(input);
                names.add(kind);
            }
        }

        // 200 untimed calls per method and case; compilation is done by the runner.
        for (Map<String, Object> input : cases) {
            for (int round = 0; round < 200; round++) {
                for (int method = 0; method < 3; method++) sink = search(method, input);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < cases.size(); index++) {
            Map<String, Object> input = cases.get(index);
            long[][] times = new long[3][3];
            // Rotate method order to reduce a fixed ordering advantage.
            for (int run = 0; run < 3; run++) {
                for (int offset = 0; offset < 3; offset++) {
                    int method = (run + offset) % 3;
                    long start = System.nanoTime();
                    for (int repeat = 0; repeat < 100; repeat++) sink = search(method, input);
                    times[method][run] = System.nanoTime() - start;
                }
            }
            for (int method = 0; method < 3; method++) {
                long[] sorted = times[method].clone();
                Arrays.sort(sorted);
                rows.add(Map.of("case", names.get(index), "n", ((String) input.get("text")).length(),
                        "m", ((String) input.get("pattern")).length(), "algorithm", methods[method],
                        "batch_ns", times[method], "median_ns_per_call", sorted[1] / 100.0,
                        "correct", true));
            }
        }
        System.out.println(Json.write(Map.of("seed", seed, "warmup_calls", 200,
                "calls_per_run", 100, "java", System.getProperty("java.runtime.version"),
                "vm", System.getProperty("java.vm.name"), "rows", rows)));
    }
}
