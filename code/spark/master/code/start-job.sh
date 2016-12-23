#!/bin/bash

#hdfs dfs -rm /clustervolume/*
#hdfs dfs -copyFromLocal boontadata-spark-job1-assembly-0.1.jar /clustervolume

#spark-submit \
#    --class io.boontadata.spark.job1.DirectKafkaAggregateEvents \ 
#    --master spark://sparkm1:7077 \
#    --deploy-mode cluster \
#    --files boontadata-spark-job1-assembly-0.1.jar
#    boontadata-spark-job1-assembly-0.1.jar \ 
#    ks1:9092,ks2:9092,ks3:9092 subscribe sampletopic

#TODO: have to be able to submit to the whole cluster
spark-submit boontadata-spark-job1-assembly-0.1.jar ks1:9092,ks2:9092,ks3:9092 subscribe sampletopic
