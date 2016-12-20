// scalastyle:off println
package io.boontadata.spark.job1

import kafka.serializer.StringDecoder

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Usage: DirectKafkaWordCount <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <topics> is a list of one or more kafka topics to consume from
 *
 * Example:
 *    $ bin/run-example streaming.DirectKafkaWordCount broker1-host:port,broker2-host:port \
 *    topic1,topic2
 */
object DirectKafkaAggregateEvents {
  val FIELD_MESSAGE_ID = 0
  val FIELD_DEVICE_ID = 1
  val FIELD_TIMESTAMP = 2
  val FIELD_CATEGORY = 3
  val FIELD_MEASURE1 = 4
  val FIELD_MEASURE2 = 5

  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println(s"""
        |Usage: DirectKafkaAggregateEvents <brokers> <topics>
        |  <brokers> is a list of one or more Kafka brokers
        |  <topics> is a list of one or more kafka topics to consume from
        |
        """.stripMargin)
      System.exit(1)
    }

    Utility.setStreamingLogLevels()

    val Array(brokers, topics) = args

    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("boontadata-DirectKafkaAggregateEvents")
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    // Get the lines, split them into words, count the words and print
    val lines = messages.map(tuple => tuple._2)
    val parsed = lines.flatMap(_.split("|"))
    val parsedDeduplicated = parsed.map(event => 
      (event[FIELD_MESSAGE_ID],event)).
        reduceByKey(x,y => y)
    val aggregated = parsedDeduplicated.map(event =>
      (
        (event[1][FIELD_DEVICE_ID], event[1][FIELD_CATEGORY]),
        (int(event[1][FIELD_MEASURE1]), float(event[1][FIELD_MEASURE2]))
      )).
        reduceByKey(vN,vNplus1 => (vN._1 + vNplus1._1, vN._2 + vNplus1._2)).
        transform(time,x => x.
          map(kv => {
            "window_time": time,
            "device_id": kv[0][0],
            "category": kv[0][1], 
            "m1_sum_spark": kv[1][0],
            "m2_sum_spark": kv[1][1] }))

    aggregated.pprint()
    aggregated.foreachRDD(lambda rdd: rdd.saveToCassandra("boontadata", "agg_events"))
    


    val wordCounts = words.map(x => (x, 1L)).reduceByKey(_ + _)
    wordCounts.print()

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
// scalastyle:on println