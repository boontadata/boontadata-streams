import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer082
import org.apache.flink.streaming.util.serialization.SimpleStringSchema

object Main {
  def main(args: Array[String]) {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(5000) // checkpoint every 5000 msecs
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "ks1:9092,ks2:9092,ks3:9092") //should replace by environment variable
    properties.setProperty("zookeeper.connect", "zk1:2181") //should replace by environment variable
    properties.setProperty("group.id", "ReadKafkaWithFlink")
    
    val myConsumer = new FlinkKafkaConsumer082[String]("sampletopic", new SimpleStringSchema(), properties);
    myConsumer.assignTimestampsAndWatermarks(new TimeLagWatermarkGenerator());
    stream = env
        .addSource(myConsumer)
        .print

    env.execute("Flink Kafka Example")
  }
}

/**
 * This generator generates watermarks assuming that elements come out of order to a certain degree only.
 * The latest elements for a certain timestamp t will arrive at most n milliseconds after the earliest
 * elements for timestamp t.
 */
class BoundedOutOfOrdernessGenerator extends AssignerWithPeriodicWatermarks[String] {
    val maxOutOfOrderness = 3500L; // 3.5 seconds

    var currentMaxTimestamp: Long;

    override def extractTimestamp(element: String, previousElementTimestamp: Long): Long = {
        val timestamp = element.getCreationTime() 
        currentMaxTimestamp = max(timestamp, currentMaxTimestamp)
        timestamp;
    }

    override def getCurrentWatermark(): Watermark = {
        // return the watermark as current highest timestamp minus the out-of-orderness bound
        new Watermark(currentMaxTimestamp - maxOutOfOrderness);
    }
}

/**
 * This generator generates watermarks that are lagging behind processing time by a certain amount.
 * It assumes that elements arrive in Flink after at most a certain time.
 */
class TimeLagWatermarkGenerator extends AssignerWithPeriodicWatermarks[String] {

    val maxTimeLag = 15000L; // 15 seconds

    override def extractTimestamp(element: String, previousElementTimestamp: Long): Long = {
        // extract timestamp from the string 
    }

    override def getCurrentWatermark(): Watermark = {
        // return the watermark as current time minus the maximum time lag 
        new Watermark(System.currentTimeMillis() - maxTimeLag)
    }
}

