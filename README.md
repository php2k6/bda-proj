# MovieMatch — Movie Recommendation Engine

Big Data Analytics mini project using **Java 17, Apache Spark 3.5.7 and MovieLens**. Spark cleans the CSV files, calculates dataset statistics and trains an ALS collaborative filtering model. A small Java web server shows recommendations and analytics.

## Start on Windows

```bat
setup.cmd
run.cmd
```

Open **http://127.0.0.1:8080**. Keep the terminal open; Ctrl+C stops the server. Setup finds or installs JDK 17, downloads a private Maven installation, builds the project, runs the tests and installs Spark libraries. Internet is needed for first setup. JDK installation may require the normal Windows installer prompt.

The dataset, trained factors and executable JAR are included. `run.cmd` performs setup if required. To train again from the CSV files:

```bat
train.cmd
```

## Linux or macOS

```bash
bash setup.sh
bash run.sh
# Rebuild the model later:
bash train.sh
```

The shell setup supports Ubuntu/Debian through apt and macOS through an existing Homebrew installation. Other systems need JDK 17, curl and unzip installed first. Windows Git Bash delegates to the PowerShell scripts. Maven and library versions are pinned. Linux/macOS scripts have syntax checks but were not executed on those operating systems; the full project was tested on Windows with Java 17.

Allow roughly 2 GB free disk space and 4 GB RAM for setup and training. Spark uses two local worker threads and a 2 GB JVM heap. Serving the included model is much lighter. No Python, Node, MongoDB, Hadoop cluster or manually downloaded winutils.exe is required.

## Try it in class

1. Select User 1 and recommend films. Change the user to compare recommendations.
2. Select Drama and set the minimum rating count to 50. Already rated films remain excluded.
3. Open **My ratings**, load the example, or search for films and rate them. Click **Recommend movies**.
4. Open **Dataset analytics** to show the distribution of ratings, popular movies and measured evaluation.

ALS scores are raw model estimates, not probabilities or star ratings. They can exceed 5. The movie average beside each score is a separate dataset statistic. New profiles remain in the browser tab and are not permanently saved.

![Recommendation screen](docs/screenshots/recommendations.png)

## Dataset

Official GroupLens `ml-latest-small`: **100,836 ratings, 610 anonymous users, 9,742 catalogue movies**. Ratings range from 0.5 to 5 in half-star steps. This is an educational sample, not a claim of processing a production-scale dataset.

| File | Fields used |
|---|---|
| `ratings.csv` | userId, movieId, rating, timestamp |
| `movies.csv` | movieId, title, genres |
| `tags.csv`, `links.csv` | Included in the original download; not used by this model |

Source: [MovieLens](https://grouplens.org/datasets/movielens/latest/). The original licence is in [data/ml-latest-small/README.txt](data/ml-latest-small/README.txt). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The dataset and derived artifacts retain its research/noncommercial conditions.

## Processing and model

```text
ratings.csv + movies.csv
          |
  Spark cleaning, deduplication and join
          |
  RDD counts + DataFrame / SQL analytics
          |
  Per-user chronological split -> ALS -> holdout metrics
          |
  Refit ALS on all cleaned ratings
          |
  Factor JSON + analytics + Parquet + recommendation documents
          |
  Java HTTP API -> local browser
```

- IDs must be positive 32-bit integers; ratings must be finite half-star values from 0.5 to 5; timestamps must be nonnegative.
- Keep the latest record for a duplicate user/movie pair; ties use the higher rating. Remove references to unknown movies.
- ALS learns 12 factors per user/movie, with 10 iterations, regularisation 0.1 and seed 42. Parameters were fixed for this demonstration; no tuning on the test set is claimed.
- Existing users are ranked by the dot product of learned factors. Rated movies are removed, followed by genre and support filters.
- A new profile estimates one user vector using regularised least squares with fixed movie factors. It does not retrain the entire Spark model.
- An unknown user ID uses a popularity score shrunk toward the global average with a prior weight of 50.

## Actual evaluation

For each user, order ratings by timestamp and movie ID, train on the earliest floor(80%) and test on the remaining rows. This is **not a global time cutoff**. Evaluate on known-factor rows, then train a separate final model on all the data for the app.

| Measurement | Result |
|---|---:|
| Training / test ratings | 80,419 / 20,417 |
| Test rows scored | 18,715 |
| Cold-start rows excluded | 1,702 |
| Prediction coverage | 91.66% |
| ALS RMSE | 0.9316 |
| ALS MAE | 0.7176 |
| Training global-mean baseline RMSE | 1.0569 |
| Parquet write/read rating count | 100,836 |

Baseline and ALS use the same scored test rows. Predictions are not clipped. Lower RMSE means lower rating-prediction error; it does not establish top-10 recommendation quality. Ranking precision/recall and new-profile quality have not been measured. Exact run metadata and hashes are in `artifacts/metrics.json`.

## Files to explain during the viva

| Location | Purpose |
|---|---|
| `src/main/java/college/bda/TrainPipeline.java` | Spark ETL, SQL, RDDs, ALS, evaluation, export |
| `Recommender.java` in the same package | Factor scoring, filtering, new profiles and fallback |
| `WebServer.java` | Local HTTP server and JSON endpoints |
| `JavaLocalFileSystem.java` | Windows local-filesystem adapter using Java NIO |
| `src/test/java/college/bda/RecommenderTest.java` | 16 recommendation and validation tests |
| `web/` | HTML, CSS and JavaScript interface |
| `artifacts/model.json` | Final factors, histories and movie catalogue |
| `artifacts/analytics.json`, `metrics.json` | Actual Spark aggregates and evaluation |
| `artifacts/parquet/` | Cleaned movies and ratings partitioned by rating year |
| `artifacts/recommendations.ndjson` | One document per user, suitable for a NoSQL import |
| `sql/`, `scripts/hdfs-demo.sh` | SQL and optional Hadoop/Hive/MongoDB exercises |
| `report/` | Submission report in Word format |
| `docs/VIVA.md` | Short explanation and viva questions |

## Syllabus connection

Spark RDD transformations/actions, DataFrames, SQL joins and grouping, Parquet partitioning, and MLlib ALS are implemented and tested. Structured CSV and semi-structured JSON illustrate data variety. JSON recommendation documents demonstrate a document-oriented schema.

HDFS upload, Hive external tables and MongoDB import are **optional examples requiring separately configured services**, not services installed or used in the local demo. Sqoop, Pig, HBase, ZooKeeper and YARN deployment are not implemented. The application does not need every ecosystem component for this task.

## API and options

```text
GET  /api/summary
GET  /api/users
GET  /api/movies?q=matrix
GET  /api/recommend?user=1&limit=10&genre=Drama&minimum=20
POST /api/profile
     {"ratings":[{"movieId":1,"rating":4.5}],"limit":10,"minimum":20}
```

The server listens only on `127.0.0.1`. Use `run.cmd --port 8081` or `bash run.sh --port 8081` for another port. To compare training settings without replacing the submitted artifacts:

```bat
train.cmd --rank 8 --iterations 10 --regularization 0.1 --output experiments/rank8
```

Alternate input uses `--input <folder-containing-both-CSV-files>`. It also accepts an HDFS URI with configured Hadoop access. The export collects factor tables and histories into driver memory, and the Parquet output is local: full cluster deployment needs shared output storage and distributed serving changes.

## Limitations and next work

MovieLens is historical data with selection bias. The temporal split is per user, so another user's later rating can occur in training; a global chronological split would be a stronger future-time experiment. Cold-start films cannot be scored by ALS without factors. Genre is a filter, not a training feature. Popularity/support filters can reduce discovery. Serving scans the small catalogue in memory and is not designed for millions of items. No claim of a live Hadoop cluster, live movie data or production scalability is made.

Possible extensions: compare popularity ranking with ALS using precision@10, add a global time cutoff, tune settings on a validation split, combine content features for new films, and move storage/serving to a cluster.

## References

- Harper and Konstan, *The MovieLens Datasets: History and Context* (2015): https://doi.org/10.1145/2827872
- Apache Spark ALS documentation: https://spark.apache.org/docs/3.5.7/ml-collaborative-filtering.html
- Apache Spark programming guide: https://spark.apache.org/docs/3.5.7/rdd-programming-guide.html

