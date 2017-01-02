package io.boontadata.spark.job1

import kafka.serializer.StringDecoder
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

class IotEvent(
  val messageId: String,
  val deviceId: String,
  val timestamp: String,
  val category: String, 
  val measure1: Int, 
  val measure2: Float) extends Serializable {
  override def toString(): String = {
    "%s\t%s\t%s\t%s\t%s\t%s\n".format(messageId, deviceId, timestamp, category, measure1, measure2)
  }
}

object IotEvent extends Serializable {
  def fromString(in: String): IotEvent = {
    val parts = in.split("\\|")
    new IotEvent(parts(0), parts(1), parts(2), parts(3), parts(4).toInt, parts(5).toFloat)
  }
}

object DirectKafkaAggregateEvents {
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

    val lines = messages.map(tuple => tuple._2) // Spark receives Kafka payload as the second field of the tuple
    
    val parsed = lines.map(IotEvent.fromString(_))
    
    val parsedDeduplicated = parsed
      .map(event => (event.messageId, event))
      .reduceByKey((vN, vNplus1) => vNplus1)
      .map({ case (k: String, event: IotEvent) => event})

    val aggregated = parsedDeduplicated
      .map({ case event: IotEvent => ((event.deviceId, event.category), event)})
      .reduceByKey({ case (vN: IotEvent, vNplus1: IotEvent) 
        => 
        val deviceId = vN.deviceId
        val category = vN.category 
        val timestamp = vN.timestamp
        val sumM1 = vN.measure1 + vN.measure1
        val sumM2 = vN.measure2 + vN.measure2
        })
      .map(tuple => tuple._2)

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

    aggregated.print()

    /*

    */

    //parsedDeduplicated.print()
    //parsed.foreachRDD(_.collect().foreach(println))
    //aggregated.pprint()
    //aggregated.saveToCassandra("boontadata", "agg_events")

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
