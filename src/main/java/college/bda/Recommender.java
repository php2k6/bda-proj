package college.bda;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.*;
import static college.bda.ModelData.*;

/** Online ranking from Spark's learned vectors. No model retraining in requests. */
public final class Recommender {
    private final ModelData model;
    private final Map<Integer, Movie> byId = new HashMap<>();
    public Recommender(Path artifacts) throws Exception {
        this(new ObjectMapper().readValue(artifacts.resolve("model.json").toFile(), ModelData.class));
    }
    public Recommender(ModelData model) {
        this.model = model;
        for (Movie movie : model.movies) byId.put(movie.movieId(), movie);
    }
    public ModelData model() { return model; }
    public List<Movie> search(String query, int limit) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        return model.movies.stream().filter(m -> m.title().toLowerCase(Locale.ROOT).contains(q))
                .sorted(Comparator.comparingLong(Movie::ratingCount).reversed().thenComparing(Movie::title))
                .limit(limit).toList();
    }
    public List<Recommendation> forUser(int id, int limit, String genre, int minimumRatings) {
        Set<Integer> seen = new HashSet<>();
        for (Rating r : model.histories.getOrDefault(id, List.of())) seen.add(r.movieId());
        return rank(model.users.get(id), seen, limit, genre, minimumRatings);
    }
    public List<Recommendation> forProfile(List<Rating> ratings, int limit, String genre, int minimumRatings) {
        validateRatings(ratings);
        Set<Integer> seen = new HashSet<>();
        for (Rating r : ratings) seen.add(r.movieId());
        return rank(fitProfile(ratings), seen, limit, genre, minimumRatings);
    }
    private void validateRatings(List<Rating> ratings) {
        if (ratings.isEmpty() || ratings.size() > 30)
            throw new IllegalArgumentException("Choose between 1 and 30 different movies.");
        Set<Integer> ids = new HashSet<>();
        for (Rating r : ratings) {
            if (!byId.containsKey(r.movieId()) || !model.items.containsKey(r.movieId()))
                throw new IllegalArgumentException("Movie has no learned factor: " + r.movieId());
            if (!ids.add(r.movieId())) throw new IllegalArgumentException("Rate each movie only once.");
            if (!Double.isFinite(r.rating()) || r.rating() < 0.5 || r.rating() > 5 || r.rating() * 2 != Math.rint(r.rating() * 2))
                throw new IllegalArgumentException("Ratings must be 0.5 to 5, in half-star steps.");
        }
    }
    /** Conditional ALS-WR update: (Y'Y + lambda*n*I)x = Y'r. */
    double[] fitProfile(List<Rating> ratings) {
        int k = model.rank;
        double[][] gram = new double[k][k];
        double[] rhs = new double[k];
        for (Rating r : ratings) {
            double[] y = model.items.get(r.movieId());
            for (int i = 0; i < k; i++) {
                rhs[i] += y[i] * r.rating();
                for (int j = 0; j < k; j++) gram[i][j] += y[i] * y[j];
            }
        }
        for (int i = 0; i < k; i++) gram[i][i] += model.regParam * ratings.size();
        return solve(gram, rhs);
    }
    private static double[] solve(double[][] a, double[] b) {
        int n = b.length;
        for (int i = 0; i < n; i++) {
            int pivot = i;
            for (int j = i + 1; j < n; j++) if (Math.abs(a[j][i]) > Math.abs(a[pivot][i])) pivot = j;
            double[] row = a[i]; a[i] = a[pivot]; a[pivot] = row;
            double t = b[i]; b[i] = b[pivot]; b[pivot] = t;
            if (Math.abs(a[i][i]) < 1e-12) throw new IllegalArgumentException("Not enough independent ratings.");
            double divisor = a[i][i];
            for (int j = i; j < n; j++) a[i][j] /= divisor;
            b[i] /= divisor;
            for (int r = 0; r < n; r++) if (r != i) {
                double factor = a[r][i];
                for (int c = i; c < n; c++) a[r][c] -= factor * a[i][c];
                b[r] -= factor * b[i];
            }
        }
        return b;
    }
    private List<Recommendation> rank(double[] vector, Set<Integer> seen, int limit,
                                      String genre, int minimumRatings) {
        if (limit < 1 || limit > 50 || minimumRatings < 0 || minimumRatings > 10000)
            throw new IllegalArgumentException("Invalid result limit or minimum rating count.");
        List<Recommendation> result = new ArrayList<>();
        for (Movie movie : model.movies) {
            if (seen.contains(movie.movieId()) || movie.ratingCount() < minimumRatings) continue;
            if (genre != null && !genre.isBlank() && !Arrays.asList(movie.genres().split("\\|")).contains(genre)) continue;
            double[] item = model.items.get(movie.movieId());
            if (vector != null && item == null) continue;
            double score = vector == null ? movie.weightedRating() : dot(vector, item);
            if (Double.isFinite(score)) result.add(new Recommendation(movie.movieId(), movie.title(), movie.genres(),
                    movie.ratingCount(), movie.averageRating(), score));
        }
        result.sort(Comparator.comparingDouble(Recommendation::score).reversed()
                .thenComparing(Comparator.comparingLong(Recommendation::ratingCount).reversed())
                .thenComparing(Recommendation::title).thenComparingInt(Recommendation::movieId));
        return result.subList(0, Math.min(limit, result.size()));
    }
    static double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }
}
