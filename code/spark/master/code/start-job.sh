#!/bin/bash

hdfs dfs -rm /clustervolume/*
hdfs dfs -copyFromLocal boontadata-spark-job1-assembly-0.1.jar /clustervolume

spark-submit \
    --class io.boontadata.spark.job1.DirectKafkaAggregateEvents \
    --deploy-mode cluster \
    --master spark://sparkm1:6066 \
    /clustervolume/boontadata-spark-job1-assembly-0.1.jar \
    ks1:9092,ks2:9092,ks3:9092 sampletopic
