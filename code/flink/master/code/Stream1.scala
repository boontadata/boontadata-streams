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
    myConsumer.assignTimestampsAndWatermarks(new CustomWatermarkEmitter());
    stream = env
        .addSource(myConsumer)
        .print

    //val stream = env
    //  .addSource(new FlinkKafkaConsumer082[String]("sampletopic", new SimpleStringSchema(), properties))
    //  .print

    env.execute("Flink Kafka Example")
  }
}