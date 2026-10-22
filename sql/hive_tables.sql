-- Optional exercise; not used by the local application.
-- First run scripts/hdfs-demo.sh. Replace YOUR_USER in both locations.
CREATE DATABASE IF NOT EXISTS moviematch;
USE moviematch;
CREATE EXTERNAL TABLE IF NOT EXISTS ratings (
  userId INT, movieId INT, rating DOUBLE, `timestamp` BIGINT
) PARTITIONED BY (year INT) STORED AS PARQUET
LOCATION '/user/YOUR_USER/bda-moviematch/parquet/ratings';
MSCK REPAIR TABLE ratings;
CREATE EXTERNAL TABLE IF NOT EXISTS movies (
  movieId INT, title STRING, genres STRING
) STORED AS PARQUET
LOCATION '/user/YOUR_USER/bda-moviematch/parquet/movies';
SELECT m.title, COUNT(*) AS n, AVG(r.rating) AS average_rating
FROM ratings r JOIN movies m ON m.movieId = r.movieId
GROUP BY m.title HAVING COUNT(*) >= 50
ORDER BY average_rating DESC LIMIT 10;
