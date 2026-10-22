package college.bda;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

public final class WebServer {
    private final Path root;
    private final int port;
    private final ObjectMapper json = new ObjectMapper();
    private final Recommender recommender;
    public WebServer(Path root, int port) throws Exception {
        this.root = root; this.port = port;
        if (!Files.exists(root.resolve("artifacts/model.json")))
            throw new IllegalStateException("Model not found. Keep the artifacts folder beside the JAR, or run train.cmd.");
        recommender = new Recommender(root.resolve("artifacts"));
    }
    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Movie Recommender: http://127.0.0.1:" + port);
        System.out.println("Press Ctrl+C to stop. The model runs locally; no account or API key is needed.");
    }
    private void handle(HttpExchange x) throws IOException {
        try {
            String path = x.getRequestURI().getPath();
            Map<String, String> p = query(x.getRequestURI().getRawQuery());
            if (path.startsWith("/api/")) {
                if (!x.getRequestMethod().equals(path.equals("/api/profile") ? "POST" : "GET")) {
                    send(x, 405, Map.of("error", "Method not allowed")); return;
                }
                switch (path) {
                    case "/api/summary" -> {
                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("analytics", json.readTree(root.resolve("artifacts/analytics.json").toFile()));
                        payload.put("metrics", json.readTree(root.resolve("artifacts/metrics.json").toFile()));
                        payload.put("genres", recommender.model().movies.stream().flatMap(m -> Arrays.stream(m.genres().split("\\|")))
                                .filter(g -> !g.startsWith("(")).distinct().sorted().toList());
                        send(x, 200, payload);
                    }
                    case "/api/users" -> send(x, 200, recommender.model().histories.entrySet().stream()
                            .map(e -> Map.of("userId", e.getKey(), "ratingCount", e.getValue().size())).toList());
                    case "/api/movies" -> send(x, 200, recommender.search(p.getOrDefault("q", ""), 20));
                    case "/api/recommend" -> {
                        int user = Integer.parseInt(p.getOrDefault("user", "1"));
                        int limit = Integer.parseInt(p.getOrDefault("limit", "10"));
                        int minimum = Integer.parseInt(p.getOrDefault("minimum", "20"));
                        boolean known = recommender.model().users.containsKey(user);
                        send(x, 200, Map.of("method", known ? "ALS collaborative filtering" : "Popularity fallback",
                                "userId", user, "ratedCount", recommender.model().histories.getOrDefault(user, List.of()).size(),
                                "recommendations", recommender.forUser(user, limit, p.get("genre"), minimum)));
                    }
                    case "/api/profile" -> {
                        byte[] bytes = x.getRequestBody().readNBytes(16385);
                        if (bytes.length > 16384) throw new IllegalArgumentException("Request is too large.");
                        JsonNode payload = json.readTree(bytes);
                        if (payload == null || !payload.path("ratings").isArray())
                            throw new IllegalArgumentException("A ratings array is required.");
                        List<ModelData.Rating> ratings = new ArrayList<>();
                        for (JsonNode r : payload.path("ratings")) {
                            if (!r.path("movieId").canConvertToInt() || !r.path("rating").isNumber())
                                throw new IllegalArgumentException("Each rating needs a movieId and a numeric rating.");
                            ratings.add(new ModelData.Rating(r.get("movieId").asInt(), r.get("rating").asDouble()));
                        }
                        send(x, 200, Map.of("method", "ALS profile from your ratings", "ratedCount", ratings.size(),
                                "recommendations", recommender.forProfile(ratings, payload.path("limit").asInt(10),
                                payload.path("genre").asText(""), payload.path("minimum").asInt(20))));
                    }
                    default -> send(x, 404, Map.of("error", "Endpoint not found"));
                }
            } else {
                Map<String, String> files = Map.of("/", "index.html", "/app.js", "app.js", "/style.css", "style.css");
                String file = files.get(path);
                if (file == null) { send(x, 404, Map.of("error", "Page not found")); return; }
                String type = file.endsWith("js") ? "text/javascript" : file.endsWith("css") ? "text/css" : "text/html";
                byte[] body = Files.readAllBytes(root.resolve("web").resolve(file));
                x.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
                x.sendResponseHeaders(200, body.length);x.getResponseBody().write(body);x.close();
            }
        } catch (IllegalArgumentException e) { send(x, 400, Map.of("error", e.getMessage())); }
        catch (Exception e) { e.printStackTrace();send(x, 500, Map.of("error", "Could not process the request.")); }
    }
    private void send(HttpExchange x, int status, Object value) throws IOException {
        byte[] body = json.writeValueAsBytes(value);
        x.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(status, body.length);x.getResponseBody().write(body);x.close();
    }
    private static Map<String, String> query(String raw) {
        Map<String, String> result = new HashMap<>();
        if (raw != null) for (String item : raw.split("&")) {
            String[] parts = item.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }
}
