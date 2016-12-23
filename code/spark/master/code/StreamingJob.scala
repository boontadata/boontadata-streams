package io.boontadata.spark.job1

import org.apache.spark.sql.SparkSession

object DirectKafkaAggregateEvents {
  val FIELD_MESSAGE_ID = 0
  val FIELD_DEVICE_ID = 1
  val FIELD_TIMESTAMP = 2
  val FIELD_CATEGORY = 3
  val FIELD_MEASURE1 = 4
  val FIELD_MEASURE2 = 5
  val VERSION = "v161223b"

  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println(s"""
        |Usage: DirectKafkaAggregateEvents <brokers> <subscribeType> <topics>
        |  <brokers> is a list of one or more Kafka brokers
        |  <topics> is a list of one or more kafka topics to consume from
        |
        """.stripMargin)
      System.exit(1)
    }

    println(s"starting io.boontadata.spark.job1.DirectKafkaAggregateEvents ${VERSION}")

    val Array(bootstrapServers, topics) = args

    val spark = SparkSession
      .builder
      .appName("boontadata-spark-job1")
      .getOrCreate()

    import spark.implicits._
    import org.apache.spark.sql.expressions.scalalang.typed._

    // Create DataSet representing the stream of input lines from kafka
    val lines = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      .option("subscribe", topics)
      .load()
      .selectExpr("CAST(value AS STRING)")
      .as[String]

    // !!! this is pure pseudo code at this stage, inspired by some Scala code I could read previously!!
    val parsedDeduplicated = lines
      .flatMap(value: String => 
        Array(
          messageId: String, 
          deviceId: String,
          timestampAsString: String, 
          category: String,
          m1AsString: String,
          m2AsString: String
          ) = _.split("|"))
      .map(
        case timestampAsString: String => timestamp: Timestamp = Timestamp.parse(timestampAsString),
        case mm1AsString1: String => m1: Long = Long.parse(mm1AsString1),
        case m2AsString: String => m2: Float = Float.parse(m2AsString)
      )
      .groupBy(window($"timestamp", "5 seconds", "5 seconds"), $"messageId")
      .agg(_) // get the latest one in the group to deduplicate

    val aggregated = parsedDeduplicated
      .groupBy(window($"timestamp", "5 seconds", "5 seconds"), $"deviceId", $"category")
      .agg(typed.sum(_.m1), typed.sum(_.m2))

    // Start running the query that prints the running counts to the console
    val query = wordCounts.writeStream
      .outputMode("append")
      .format("cassandra")
      .start()

    query.awaitTermination()
  }

}

