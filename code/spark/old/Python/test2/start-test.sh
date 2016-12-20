#!/bin/bash

#see packages in search.maven.org
spark-submit --packages org.apache.spark:spark-streaming-kafka-0-8-assembly_2.11:2.0.2,\
com.datastax.spark:spark-cassandra-connector_2.11:1.6.3,\
TargetHolding:pyspark-cassandra:0.3.5 /workdir/test.py
