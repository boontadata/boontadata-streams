package io.boontadata.spark.job1

import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.SomeColumns
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

object IotEventFactory {
  def fromString(in: String): IotEvent = {
    val parts = in.split("\\|")
    new IotEvent(parts(0), parts(1), parts(2), parts(3), parts(4).toInt, parts(5).toFloat)
  }
  def fromParts(
    messageId: String,
    deviceId: String,
    timestamp: String,
    category: String, 
    measure1: Int, 
    measure2: Float): IotEvent = {
    new IotEvent(messageId, deviceId, timestamp, category, measure1, measure2)
  }
}

class Aggregate(
  val window_time: String,
  val device_id: String,
  val category: String, 
  val m1_sum_downstream: Int, 
  val m2_sum_downstream: Float) extends Serializable {
  override def toString(): String = {
    "%s\t%s\t%s\t%s\t%s\n".format(window_time, device_id, category, m1_sum_downstream, m2_sum_downstream)
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
    val windowTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Create context with 2 second batch interval
    val sparkConf = new SparkConf()
      .setAppName("boontadata-DirectKafkaAggregateEvents")
      .set("spark.cassandra.connection.host", "cassandra1,cassandra2,cassandra3")
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    val lines = messages.map(tuple => tuple._2) // Spark receives Kafka payload as the second field of the tuple
    
    val parsed = lines.map(IotEventFactory.fromString(_))
    
    val parsedDeduplicated = parsed
      .map(e => (e.messageId, e))
      .reduceByKey((vN, vNplus1) => vNplus1)
      .map(tuple => tuple._2)

    val aggregated = parsedDeduplicated
      .map(e => ((e.deviceId, e.category), e))
      .reduceByKey(
        (vN, vNplus1)
        => 
        IotEventFactory.fromParts("", vN.deviceId, "", vN.category, 
          vN.measure1 + vNplus1.measure1, 
          vN.measure2 + vNplus1.measure2))
      .transform((rdd, time) => rdd
        .map({ case(k, e) => new Aggregate(
          windowTimeFormat.format(new java.util.Date(time.milliseconds)), 
          e.deviceId, 
          e.category, 
          e.measure1, 
          e.measure2)}))

    aggregated.print()

    aggregated.saveToCassandra("boontadata", "agg_events", 
      SomeColumns("device_id", "category", "window_time", "m1_sum_downstream", "m2_sum_downstream"))

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
