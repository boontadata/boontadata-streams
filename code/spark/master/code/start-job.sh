#!/bin/bash

spark-submit boontadata-spark-job1_2.11-0.1.jar \
    --master spark://sparkm1:7077 \
    --deploy-mode cluster \
    io.boontadata.spark.job1.DirectKafkaAggregateEvents ks1,ks2,ks3 sampletopic
