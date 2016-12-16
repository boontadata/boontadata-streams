import os
from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils

# Create a local StreamingContext with two working thread and batch interval of 1 second
sc = SparkContext(appName="ReadKafkaWithPython")
ssc = StreamingContext(sc, 2)

# consume Kafka
kstream = KafkaUtils.createDirectStream(ssc, ["sampletopic"], {"metadata.broker.list": "ks1:9092,ks2:9092,ks3:9092"})
lines = kstream.map(lambda x: x)
counts = lines.flatMap(lambda line: line.split("|")) \
    .map(lambda word: (word, 1)) \
    .reduceByKey(lambda a,b: a + b)
counts.pprint()

ssc.start()
ssc.awaitTermination()
