package college.bda;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class DataDownloader {
    public static final String URL = "https://files.grouplens.org/datasets/movielens/ml-latest-small.zip";
    public static void download(Path root) throws Exception {
        Path data = root.resolve("data");
        Files.createDirectories(data);
        Path archive = data.resolve("movielens-download.zip");
        System.out.println("Downloading MovieLens from GroupLens...");
        try (InputStream in = URI.create(URL).toURL().openStream()) {
            Files.copy(in, archive, StandardCopyOption.REPLACE_EXISTING);
        }
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                Path target = data.resolve(entry.getName()).normalize();
                if (!target.startsWith(data)) throw new IllegalArgumentException("Invalid archive entry");
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(data.resolve("source.json").toFile(),
                Map.of("url", URL, "sha256", sha256(archive), "dataset", "ml-latest-small"));
        Files.delete(archive);
        System.out.println("Saved data/ml-latest-small, including the original licence README.");
    }
    public static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536];
            for (int n; (n = in.read(buffer)) > 0;) digest.update(buffer, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
