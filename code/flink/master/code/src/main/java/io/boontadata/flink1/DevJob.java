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
import java.time.Instant;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple1;
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
import org.apache.flink.streaming.util.serialization.DeserializationSchema;
import org.apache.flink.streaming.util.serialization.SerializationSchema;
import org.apache.flink.streaming.connectors.cassandra.CassandraPojoSink;

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

public class DevJob {

	private static final Integer FIELD_MESSAGE_ID = 0;
	private static final Integer FIELD_DEVICE_ID = 1;
	private static final Integer FIELD_TIMESTAMP = 2;
	private static final Integer FIELD_CATEGORY = 3;
	private static final Integer FIELD_MEASURE1 = 4;
	private static final Integer FIELD_MESAURE2 = 5; 
	private static final String VERSION = "161109a";

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
			.map(
				new MapFunction<String, 
					Tuple6<String, String, Long, String, Long, Float>>() {
					private static final long serialVersionUID = 34_2016_10_19_001L;

					@Override
					public Tuple6<String, String, Long, String, Long, Float> map(String value) throws Exception {
						String[] splits = value.split("\\|");
						return new Tuple6<String, String, Long, String, Long, Float>(
							splits[FIELD_MESSAGE_ID], 
							splits[FIELD_DEVICE_ID],
							Long.parseLong(splits[FIELD_TIMESTAMP]),
							splits[FIELD_CATEGORY],
							Long.parseLong(splits[FIELD_MEASURE1]),
							Float.parseFloat(splits[FIELD_MESAURE2])
						);
					}
				}
			)
			.map(
				new MapFunction<Tuple6<String, String, Long, String, Long, Float>, Tuple2<String, String>>() {
					@Override
					public Tuple2<String,String> map(Tuple6<String, String, Long, String, Long, Float> value) throws Exception {
						return new Tuple2<String,String>(
							"DevJob-v" + VERSION + "-" + Instant.now().toString(), 
							"MESSAGE_ID=" + value.getField(0).toString() + ", "
							+ "DEVICE_ID=" + value.getField(1).toString() + ", "
							+ "TIMESTAMP=" + value.getField(2).toString() + ", "
							+ "CATEGORY=" + value.getField(3).toString() + ", "
							+ "M1=" + value.getField(4).toString() + ", "
							+ "M2=" + value.getField(5).toString());
					}
				}
			)
			.addSink(new CassandraTupleSink<Tuple2<String, String>>(
                                "INSERT INTO boontadata.debug"
                                        + " (id, message)"
                                        + " VALUES (?, ?);",
                                new ClusterBuilder() {
                                        @Override
                                        public Cluster buildCluster(Cluster.Builder builder) {
                                                return builder
                                                        .addContactPoint("cassandra1").withPort(9042)
                                                        .addContactPoint("cassandra2").withPort(9042)
                                                        .addContactPoint("cassandra3").withPort(9042)
                                                        .build();
                                        }
                                }));

		env.execute("DevJob-v" + VERSION);
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
