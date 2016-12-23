package io.boontadata.spark.job1

import org.apache.spark.sql.SparkSession

object DirectKafkaAggregateEvents {
  val FIELD_MESSAGE_ID = 0
  val FIELD_DEVICE_ID = 1
  val FIELD_TIMESTAMP = 2
  val FIELD_CATEGORY = 3
  val FIELD_MEASURE1 = 4
  val FIELD_MEASURE2 = 5
  val VERSION = "v161223a"

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

    // Create DataSet representing the stream of input lines from kafka
    val lines = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      .option("subscribe", topics)
      .load()
      .selectExpr("CAST(value AS STRING)")
      .as[String]

    // Generate running word count
    val wordCounts = lines.flatMap(_.split(" ")).groupBy("value").count()

    // Start running the query that prints the running counts to the console
    val query = wordCounts.writeStream
      .outputMode("complete")
      .format("console")
      .start()

    query.awaitTermination()
  }

}

