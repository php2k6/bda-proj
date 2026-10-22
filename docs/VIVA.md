# Project explanation

## What it does

- Reads movie details and user ratings from CSV files.
- Learns user preferences and scores unseen movies.
- Lets users select a profile or enter their own ratings.
- Filters suggestions by genre and rating count.
- Shows dataset statistics and prediction errors.

## How it works

1. Clean ratings and remove duplicate user/movie pairs.
2. Join ratings with movie names and genres.
3. Use ALS to learn factor vectors for users and movies.
4. Score each movie using the dot product of its vector and the user vector.
5. Remove rated movies and apply the filters.
6. Show the highest-scoring remaining movies.

## Short viva answers

- ALS means Alternating Least Squares. It alternately updates user and movie factors.
- Rank is the number of factors; 12 here.
- Regularisation limits factor size to reduce overfitting; 0.1 here.
- For a new user, a vector is calculated from entered ratings with movie factors fixed.
- Cold start means a user or movie has no learned factors.
- RMSE measures prediction error. Lower is better; it is not percentage accuracy.
- Coverage is the share of test ratings scored; 91.66% here.
- ALS scores can exceed 5 because the model has no fixed upper limit.
- RDD operations used: mapToPair, reduceByKey and count.
- Parquet stores cleaned records as typed columns, with ratings partitioned by year.
- The tested application runs locally. Hadoop and database examples are optional.

## Demo

- Run setup.cmd, then run.cmd.
- Recommend movies for User 1, then change the user or genre.
- Open My ratings, load the example and recommend movies.
- Open Dataset analytics and explain RMSE and coverage.

## Improvements

- Measure precision and recall for top-10 suggestions.
- Use a global date cutoff for testing.
- Add movie metadata to handle new films.
- Save new profiles if permanent accounts are needed.
