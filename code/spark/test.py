import pip
try:
    from cassandra.cluster import Cluster
except ImportError:
    pip.main(["install", "cassandra-driver"])
    from cassandra.cluster import Cluster

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

csession = None
ccluster = None

def parseEvent(message):
    return message.split('|')

# NB: could not make the standard saveToCassandra method work in this context where Spark and Cassandra are in different clusters
#please, contribute if you know how to enhance this
def transformAndSaveRowToCassandra(time, kv):
    r= {
        "window_time": time,
        "device_id": kv[0][0],
        "category": kv[0][1], 
        "m1_sum_spark": kv[1][0],
        "m2_sum_spark": kv[1][1] }

    if csession is None:
        ccluster=Cluster(['cassandra1', 'cassandra2', 'cassandra3'])
        csession=ccluster.connect('boontadata')

    csession.execute("INSERT INTO agg_events "
        + "(window_time, device_id, category, m1_sum_spark, m2_sum_spark) " \
        + "VALUES ('{0}', '{1}', '{2}', {3}, {4})"
        .format(
            str(r['window_time']), 
            str(r['device_id']), 
            str(r['category']),
            int(r['m1_sum_spark']),
            float(r['m2_sum_spark'])))
    return r

def main():
    #connect to Spark
    conf = SparkConf() \
        .setAppName("boontadata-streams-spark") \
        .setMaster("spark://sparkm1:7077")
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
            .map(lambda kv: transformAndSaveRowToCassandra(time, kv)))

    aggregated.pprint()

    #could not have the correct jars, or the correct architecture to make the following line working:
    #aggregated.saveToCassandra("boontadata", "agg_events")

    streamingContext.start()
    streamingContext.awaitTermination()
    if ccluster is not None:
        ccluster.shutdown()
    print("OK.")

if __name__ == '__main__':
    main()
