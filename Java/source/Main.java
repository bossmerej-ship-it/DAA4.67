import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class Main {
    static Object handle(Map<String, Object> request) {
        return switch ((String) request.get("task")) {
            case "ping" -> Map.of("status", "ready");
            case "lcs" -> Algorithms.lcs(request);
            case "merge" -> Algorithms.merge(request);
            case "tree" -> Algorithms.tree(request);
            case "naive" -> Algorithms.naive(request);
            case "kmp" -> Algorithms.kmp(request);
            case "rk" -> Algorithms.rk(request);
            case "suffix" -> Algorithms.suffix(request);
            case "aho" -> Algorithms.aho(request);
            default -> throw new IllegalArgumentException("Unknown task");
        };
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            Map<String, Object> request = (Map<String, Object>) Json.parse(line);
            System.out.println(Json.write(handle(request)));
        }
    }
}
