-- Run from the project root in a Spark SQL session.
CREATE OR REPLACE TEMPORARY VIEW ratings
USING parquet OPTIONS (path 'artifacts/parquet/ratings');
CREATE OR REPLACE TEMPORARY VIEW movies
USING parquet OPTIONS (path 'artifacts/parquet/movies');
SELECT COUNT(*) AS ratings, COUNT(DISTINCT userId) AS users FROM ratings;
SELECT m.title, COUNT(*) AS rating_count, ROUND(AVG(r.rating), 2) AS mean_rating
FROM ratings r JOIN movies m ON r.movieId = m.movieId
GROUP BY m.movieId, m.title HAVING COUNT(*) >= 50
ORDER BY mean_rating DESC, rating_count DESC LIMIT 10;
SELECT year, COUNT(*) AS ratings FROM ratings GROUP BY year ORDER BY year;
