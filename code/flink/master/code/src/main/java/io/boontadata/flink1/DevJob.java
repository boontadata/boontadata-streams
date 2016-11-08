package io.boontadata.flink1;

import com.datastax.driver.core.Cluster;
import java.lang.Float;
import java.lang.Long;
import java.util.Iterator;
import java.util.Properties;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple;

// DEBUG
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
//import org.apache.flink.streaming.connectors.kafka.KafkaSink;
import org.apache.flink.streaming.util.serialization.DeserializationSchema;
import org.apache.flink.streaming.util.serialization.SerializationSchema;
//import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.streaming.connectors.cassandra.CassandraTupleSink;
import org.apache.flink.streaming.connectors.cassandra.ClusterBuilder;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer082;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.Window;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Skeleton for a Flink Streaming Job.
 *
 * For a full example of a Flink Streaming Job, see the SocketTextStreamWordCount.java
 * file in the same package/directory or have a look at the website.
 *
 * You can also generate a .jar file that you can submit on your Flink
 * cluster.
 * Just type
 * 		mvn clean package
 * in the projects root directory.
 * You will find the jar in
 * 		target/quickstart-0.1.jar
 * From the CLI you can then run
 * 		./bin/flink run -c io.boontadata.flink1.StreamingJob target/quickstart-0.1.jar
 *
 * For more information on the CLI see:
 *
 * http://flink.apache.org/docs/latest/apis/cli.html
 */
public class DevJob {

	private static final Integer FIELD_MESSAGE_ID = 0;
	private static final Integer FIELD_DEVICE_ID = 1;
	private static final Integer FIELD_TIMESTAMP = 2;
	private static final Integer FIELD_CATEGORY = 3;
	private static final Integer FIELD_MEASURE1 = 4;
	private static final Integer FIELD_MESAURE2 = 5; 

	public static void main(String[] args) throws Exception {
		// set up the streaming execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.enableCheckpointing(5000); // checkpoint every 5000 msecs
		env.setParallelism(2); // not more than the number of nodes...


		Properties kProperties = new Properties();
		kProperties.setProperty("bootstrap.servers", "ks1:9092,ks2:9092,ks3:9092");
		kProperties.setProperty("zookeeper.connect", "zk1:2181");
		kProperties.setProperty("metadata.broker.list", "zk1:2181");
		kProperties.setProperty("group.id", "flinkGroup");
		kProperties.setProperty("auto.offset.reset", "earliest");
		kProperties.setProperty("max.partition.fetch.bytes", "256");
		kProperties.setProperty("enable.auto.commit", "false");

		env
			.addSource(new FlinkKafkaConsumer082<>(
                                "sampletopic",
                                new SimpleStringSchema(),
                                kProperties))
			.rebalance()
			.map (
				new MapFunction<String, String>() {
					@Override
					public String map(String value) throws Exception {
						return "Kafka and Flink say: " + value;
					}
				}
			)
			.print();

		env.execute("161107g");
	}

	public static class SimpleStringGenerator implements SourceFunction<String> {
		private static final long serialVersionUID = 3420161107002L;
		boolean running = true;
		long i = 0;
		
		@Override
		public void run(SourceContext<String> ctx) throws Exception {
			while(running) {
				ctx.collect("element-"+ (i++));
				Thread.sleep(1000);
			}
		}	

		@Override
		public void cancel() {
			running = false;
		}
	}

}
