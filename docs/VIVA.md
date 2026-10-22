# Short viva notes

## Explain the project in one minute

MovieMatch reads movie ratings with Java and Spark. It cleans and joins the CSV files, calculates statistics and trains an ALS model. ALS represents each user and movie as a short vector. Their dot product estimates the user's preference. The Java server returns unseen films sorted by that score. We can also enter a new profile and inspect the dataset analysis. Evaluation uses held-out ratings and reports both error and coverage.

## Questions

**Why Spark for this small file?** It lets us demonstrate the same DataFrame, RDD and MLlib APIs used for larger data. This sample runs locally and does not prove cluster performance.

**What are the four Vs here?** Volume means the number of ratings; velocity would matter for a live stream, which this batch project does not implement; variety appears in CSV, JSON and Parquet; veracity is handled through validation and cleaning.

**Which RDD transformations and actions are used?** `mapToPair` makes user/count pairs, `reduceByKey` combines counts, and `count` triggers execution. DataFrames handle the remaining ETL.

**What is lazy evaluation?** Transformations describe work. Actions such as count or model fitting cause Spark to execute it. Persisted DataFrames can be reused.

**What is ALS?** Alternating Least Squares fixes movie factors while solving user factors, then reverses the process. Repeated updates minimise prediction error with regularisation.

**What are rank and regularisation?** Rank is the number of latent factors (12 here). Regularisation discourages overly large factors and helps control overfitting (0.1 here).

**How is a new profile handled?** Keep the learned movie factors fixed and solve a regularised least-squares problem for that user's vector. The whole model need not be retrained.

**Why can a score exceed 5?** ALS is unconstrained regression. We show raw model scores and do not label them as stars or probabilities.

**What is cold start?** A user or film absent from training has no learned factors. Such test rows are excluded by Spark; our report also shows the excluded count and prediction coverage.

**What is RMSE?** The square root of mean squared rating error. Our result is about 0.9316 on scored holdout rows. It is not a recommendation accuracy percentage.

**Why partition Parquet by year?** It groups rating records by year and allows queries filtering a year to skip unrelated partitions. Parquet stores typed columns.

**Where are NoSQL and Hadoop used?** The main demo exports document-shaped recommendations and uses Spark with local files. HDFS, Hive and MongoDB examples are optional; they are not running services in this submission.

**What would you improve?** Use a global time cutoff and validation set, evaluate precision/recall at 10, add metadata for new movies and replace in-memory catalogue scanning when scaling.

## Useful commands

`setup.cmd` builds and installs dependencies. `train.cmd` runs real Spark processing. `run.cmd` starts the web app. `artifacts/metrics.json` contains the measured results; `TrainPipeline.java` is the main BDA implementation.
