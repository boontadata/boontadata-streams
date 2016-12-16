# notes while implementing the Spark case

## DataFrames

We use DataFrames (not Datasets) with Python because of this excerpt from <http://spark.apache.org/docs/latest/sql-programming-guide.html#datasets-and-dataframes>:

> A Dataset is a distributed collection of data. Dataset is a new interface added in Spark 1.6 that provides the benefits of RDDs (strong typing, ability to use powerful lambda functions) with the benefits of Spark SQL’s optimized execution engine. A Dataset can be constructed from JVM objects and then manipulated using functional transformations (map, flatMap, filter, etc.). The Dataset API is available in Scala and Java. **Python does not have the support for the Dataset API. But due to Python’s dynamic nature, many of the benefits of the Dataset API are already available** (i.e. you can access the field of a row by name naturally row.columnName). The case for R is similar.
>
> A DataFrame is a Dataset organized into named columns. It is conceptually equivalent to a table in a relational database or a data frame in R/Python, but with richer optimizations under the hood. DataFrames can be constructed from a wide array of sources such as: structured data files, tables in Hive, external databases, or existing RDDs. **The DataFrame API is available in Scala, Java, Python, and R.** In Scala and Java, a DataFrame is represented by a Dataset of Rows. In the Scala API, DataFrame is simply a type alias of Dataset[Row]. While, in Java API, users need to use Dataset<Row> to represent a DataFrame.

## receive from Kafka: direct approach rather than receiver based approach

cf <https://spark.apache.org/docs/1.3.1/streaming-kafka-integration.html>

> This approach has the following advantages over the received-based approach (i.e. Approach 1).
>
> - Simplified Parallelism: No need to create multiple input Kafka streams and union-ing them. With directStream, Spark Streaming will create as many RDD partitions as there is Kafka partitions to consume, which will all read data from Kafka in parallel. So there is one-to-one mapping between Kafka and RDD partitions, which is easier to understand and tune.
> 
> - Efficiency: Achieving zero-data loss in the first approach required the data to be stored in a Write Ahead Log, which further replicated the data. This is actually inefficient as the data effectively gets replicated twice - once by Kafka, and a second time by the Write Ahead Log. This second approach eliminate the problem as there is no receiver, and hence no need for Write Ahead Logs.
> 
> - Exactly-once semantics: The first approach uses Kafka’s high level API to store consumed offsets in Zookeeper. This is traditionally the way to consume data from Kafka. While this approach (in combination with write ahead logs) can ensure zero data loss (i.e. at-least once semantics), there is a small chance some records may get consumed twice under some failures. This occurs because of inconsistencies between data reliably received by Spark Streaming and offsets tracked by Zookeeper. Hence, in this second approach, we use simple Kafka API that does not use Zookeeper and offsets tracked only by Spark Streaming within its checkpoints. This eliminates inconsistencies between Spark Streaming and Zookeeper/Kafka, and so each record is received by Spark Streaming effectively exactly once despite failures.
> 
> Note that one disadvantage of this approach is that it does not update offsets in Zookeeper, hence Zookeeper-based Kafka monitoring tools will not show progress. However, you can access the offsets processed by this approach in each batch and update Zookeeper yourself (see below).

## resources

- <https://spark.apache.org/docs/1.3.1/api/python/pyspark.streaming.html#pyspark.streaming.kafka.KafkaUtils>

## sample code

from <http://www.slideshare.net/helenaedelson/lambda-architecture-with-spark-spark-streaming-kafka-cassandra-akka-and-scala> slide 84

```
val kafkaStream = KafkaUtils.createStream[K, V, KDecoder, VDecoder]
    (scc, kafkaParams, topicMap, StorageLevel.DISK_ONLY_2)
    .map(transform)
    .map(RawSeatherData(_))

/** Saves the raw data to Cassandra. */
kafkaStream.saveToCassandra(keyspace, raw_ws_data)
```

from <http://rustyrazorblade.com/2015/05/spark-streaming-with-python-and-kafka/> 
and also <https://github.com/rustyrazorblade/killranalytics/blob/intro_streaming_python2/killranalytics/spark/raw_event_stream_processing.py>:
```
import pyspark_cassandra
import pyspark_cassandra.streaming

from pyspark_cassandra import CassandraSparkContext

from pyspark.sql import SQLContext
from pyspark import SparkContext, SparkConf
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils
from uuid import uuid1

import json
```

```
# set up our contexts
sc = CassandraSparkContext(conf=conf)
sql = SQLContext(sc)
stream = StreamingContext(sc, 1) # 1 second window

kafka_stream = KafkaUtils.createStream(stream, \
                                       "localhost:2181", \
                                       "raw-event-streaming-consumer",
                                        {"pageviews":1})
```

```
parsed = kafka_stream.map(lambda (k, v): json.loads(v))

summed = parsed.map(lambda event: (event['site_id'], 1)).\
                reduceByKey(lambda x,y: x + y).\
                map(lambda x: {"site_id": x[0], "ts": str(uuid1()), "pageviews": x[1]})

summed.saveToCassandra("killranalytics", "real_time_data")

stream.start()
stream.awaitTermination()
```

```
VERSION=0.1.4

spark-submit --packages org.apache.spark:spark-streaming-kafka_2.10:1.3.1 \
    --jars /Users/jhaddad/dev/pyspark-cassandra/target/pyspark_cassandra-${VERSION}.jar  \
    --driver-class-path /Users/jhaddad/dev/pyspark-cassandra/target/pyspark_cassandra-${VERSION}.jar \
    --py-files  /Users/jhaddad/dev/pyspark-cassandra/target/pyspark_cassandra-${VERSION}-py2.7.egg \
    --conf spark.cassandra.connection.host=127.0.0.1 \
    --master spark://127.0.0.1:7077 \
    $1
```

```
./sub killranalytics/spark/raw_event_stream_processing.py
```
