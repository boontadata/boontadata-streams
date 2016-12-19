#import pip

#try:
#    from pyspark_cassandra import streaming
#except ImportError:
#    pip.main(["install", "cassandra-driver"])
#    from pyspark_cassandra import streaming

#from pyspark_cassandra import streaming

from pyspark import SparkContext, SparkConf
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils
from uuid import uuid1

FIELD_MESSAGE_ID = 0
FIELD_DEVICE_ID = 1
FIELD_TIMESTAMP = 2
FIELD_CATEGORY = 3
FIELD_MEASURE1 = 4
FIELD_MEASURE2 = 5

def parseEvent(message):
    return message.split('|')

def main():
    conf = SparkConf() \
        .setAppName("boontadata-streams-spark") \
        .setMaster("spark://sparkm1:7077") \
        .set("spark.cassandra.connection.host", "cassandra1")

    # set up our contexts
    sc = SparkContext(conf=conf) 
    streamingContext = StreamingContext(sc, batchDuration=5)

    kafka_stream = KafkaUtils.createDirectStream(streamingContext,
        ["sampletopic"], 
        {"metadata.broker.list": "ks1:9092,ks2:9092,ks3:9092"})

    parsed = kafka_stream \
        .map(lambda event: parseEvent(event[1]))

    # remove duplicates
    parsedDeduplicated = parsed.map(lambda event: (event[FIELD_MESSAGE_ID],event)) \
        .reduceByKey(lambda x,y: y)

    aggregated = parsedDeduplicated.map(lambda event:
        (
            (event[1][FIELD_DEVICE_ID], event[1][FIELD_CATEGORY]),
            (int(event[1][FIELD_MEASURE1]), float(event[1][FIELD_MEASURE2]))
        )) \
        .reduceByKey(lambda vN,vNplus1: (vN[0] + vNplus1[0], vN[1] + vNplus1[1])) \
        .transform(lambda time,rdd: rdd \
            .map(lambda kv: {
                "window_time": time,
                "device_id": kv[0][0],
                "category": kv[0][1], 
                "m1_sum_spark": kv[1][0],
                "m2_sum_spark": kv[1][1] }))

    aggregated.pprint()
    #aggregated.saveToCassandra("boontadata", "agg_events")

    streamingContext.start()
    streamingContext.awaitTermination()

if __name__ == '__main__':
    main()
