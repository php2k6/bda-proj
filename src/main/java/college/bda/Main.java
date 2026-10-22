package college.bda;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Command-line entry point. Serving the exported model does not start Spark. */
public final class Main {
    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "serve" : args[0];
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 1; i < args.length; i += 2) {
            if (!args[i].startsWith("--") || i + 1 >= args.length)
                throw new IllegalArgumentException("Options need a value, for example --port 8080");
            options.put(args[i].substring(2), args[i + 1]);
        }
        Path root = Path.of(options.getOrDefault("root", ".")).toAbsolutePath().normalize();
        switch (command) {
            case "serve" -> new WebServer(root, Integer.parseInt(options.getOrDefault("port", "8080"))).start();
            case "download" -> DataDownloader.download(root);
            case "train" -> {
                // Keeps the small demonstration JAR independent of Spark at serving time.
                Class.forName("college.bda.TrainPipeline").getMethod("run", Path.class, Map.class)
                        .invoke(null, root, options);
            }
            default -> throw new IllegalArgumentException("Commands: serve, download, train");
        }
    }
}
