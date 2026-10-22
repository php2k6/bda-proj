#!/usr/bin/env bash
# Optional: requires an existing Hadoop cluster and configured hdfs command.
set -euo pipefail
cd "$(dirname "$0")/.."
destination="${1:-/user/$USER/bda-moviematch}"
hdfs dfs -mkdir -p "$destination/input" "$destination/parquet"
hdfs dfs -put -f data/ml-latest-small/ratings.csv data/ml-latest-small/movies.csv "$destination/input/"
hdfs dfs -put -f artifacts/parquet/ratings artifacts/parquet/movies "$destination/parquet/"
hdfs dfs -ls "$destination/parquet"
echo "Use this HDFS destination in sql/hive_tables.sql: $destination"
