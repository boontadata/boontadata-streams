package io.boontadata.spark.job1

import kafka.serializer.StringDecoder

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

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
    val parsed = lines.map(_.split("|"))
    val parsedDeduplicated = parsed.map(event =>  
      (event(FIELD_MESSAGE_ID),event))
        .reduceByKey((x,y) => y)
    /*
    val aggregated = parsedDeduplicated.map(event =>
      (
        (event._2[FIELD_DEVICE_ID], event._2[FIELD_CATEGORY]),
        (int(event._2[FIELD_MEASURE1]), float(event._2[FIELD_MEASURE2]))
      ))
        .reduceByKey(vN,vNplus1 => (vN._1 + vNplus1._1, vN._2 + vNplus1._2))
        .transform(time,x => x
          .map(kv => new {
            val window_time = time
            val device_id = kv._1._1
            val category = kv._1._2 
            val m1_sum_spark = kv._2._1
            val m2_sum_spark = kv._2._2 }))
    */

    parsed.print()
    parsed.foreachRDD(_.map(case (x: String, y: String) => 
      s"$x | $y").collect().foreach(println))

    parsedDeduplicated.print()
    //parsed.foreachRDD(_.collect().foreach(println))
    //aggregated.pprint()
    //aggregated.saveToCassandra("boontadata", "agg_events")

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
