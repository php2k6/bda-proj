# Third party material

## MovieLens
`data/ml-latest-small` is distributed by GroupLens Research, University of Minnesota. The original README and its usage licence are included in that folder. The trained factors, analysis, Parquet data and recommendations are transformations of that dataset and remain subject to those terms. This project is for educational, research and noncommercial use; see the original README for the exact conditions.

Citation: F. Maxwell Harper and Joseph A. Konstan. 2015. The MovieLens Datasets: History and Context. ACM Transactions on Interactive Intelligent Systems 5(4), Article 19. https://doi.org/10.1145/2827872

Source: https://grouplens.org/datasets/movielens/latest/

## Libraries
Apache Spark and Apache Maven use the Apache License 2.0. Jackson uses the Apache License 2.0; dependency metadata is in `pom.xml`. The distribution JAR includes Jackson and the applicable META-INF licence files. Spark dependencies are downloaded during setup and are not bundled in the repository. Java is supplied by the installed JDK; setup uses Eclipse Temurin on Windows when a JDK is missing.
