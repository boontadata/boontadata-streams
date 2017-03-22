# branches

This document explains what different branches were created for.
Branch `master` is the first one, then the newest branches are at the top.

## master

This branch always has the latest stable and global version

## flink3

fix errors in Flink scenarios
move from Flink 1.1.3 to Flink 1.2.0

## spark3

Add scenario with Spark Streaming - in Scala, with Spark 1.6 means instead of Spark 2, even if the engine is Spark 2.
As explained in branch Saprk 2, Spark 2 streaming is still in alpha.

## spark2

Add scenario with Spark Streaming - in Scala this time
tried to implement <http://spark.apache.org/docs/latest/structured-streaming-programming-guide.html>, but it was too early: <http://stackoverflow.com/questions/41303037/spark-2-0-2-sql-streaming-failed-to-find-data-source-kafka>.

## spark1

Add scenario with Spark Streaming - that was a tentative in Python

## flink2

Prepare demos around the Flink scenario. 


## multi1

Adds the following features to boontadata: 
- run on one node (dev) or several nodes
- leverages a docker registry
- docker-compose.yml is created depending on the scenarios that must be run
- adds a multi node support for Spark

## flink1

Adds a scenario where Apache Flink consumes data using event time.

