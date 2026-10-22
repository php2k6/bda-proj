package college.bda;

import java.util.*;

/** Plain document structure exported after Spark training. */
public class ModelData {
    public int rank;
    public double regParam;
    public double globalMean;
    public Map<Integer, double[]> users = new TreeMap<>();
    public Map<Integer, double[]> items = new TreeMap<>();
    public Map<Integer, List<Rating>> histories = new TreeMap<>();
    public List<Movie> movies = new ArrayList<>();
    public record Rating(int movieId, double rating) { }
    public record Movie(int movieId, String title, String genres, long ratingCount,
                        double averageRating, double weightedRating) { }
    public record Recommendation(int movieId, String title, String genres, long ratingCount,
                                 double averageRating, double score) { }
}
