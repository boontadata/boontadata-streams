name := "boontadata-spark-job1"
version := "0.1"
scalaVersion := "2.11.7"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.0.2" % "provided"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.0.2" % "provided"
libraryDependencies += "org.apache.spark" % "spark-streaming-kafka-0-8_2.11" % "2.0.2"
libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.1.0"
libraryDependencies += "com.datastax.spark"  % "spark-cassandra-connector_2.11" % "2.0.0-M3"

// META-INF discarding
assemblyMergeStrategy in assembly := { 
   {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
   }
}

