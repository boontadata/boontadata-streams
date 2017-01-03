package io.boontadata.flink1;

import com.datastax.driver.core.Cluster;
import java.lang.Double;
import java.lang.Long;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.Window;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.cassandra.CassandraTupleSink;
import org.apache.flink.streaming.connectors.cassandra.ClusterBuilder;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer082;
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
public class StreamingJob {
	private static final String VERSION = "170103a";

	private static final Integer FIELD_MESSAGE_ID = 0;
	private static final Integer FIELD_DEVICE_ID = 1;
	private static final Integer FIELD_TIMESTAMP = 2;
	private static final Integer FIELD_CATEGORY = 3;
	private static final Integer FIELD_MEASURE1 = 4;
	private static final Integer FIELD_MEASURE2 = 5; 

	public static void main(String[] args) throws Exception {
		String timeCharacteristic = "EventTime";
		if (args.length > 0) {
			timeCharacteristic = args[0];
		}

		// set up the streaming execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.enableCheckpointing(5000); // checkpoint every 5000 msecs
		env.setParallelism(2);
		
		if (timeCharacteristic.equals("EventTime")) {
			env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
		} else if (timeCharacteristic.equals("ProcessingTime")) {
			env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
		} else if (timeCharacteristic.equals("IngestionTime")) {
			env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime);
		}

		Format windowTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// properties about Kafka
		Properties kProperties = new Properties();
		kProperties.setProperty("bootstrap.servers", "ks1:9092,ks2:9092,ks3:9092");
		kProperties.setProperty("zookeeper.connect", "zk1:2181");
		kProperties.setProperty("group.id", "flinkGroup");


		// get data from Kafka, parse, and assign time and watermarks
		DataStream<Tuple6<String, String, Long, String, Long, Double>> stream_parsed_with_timestamps = env 
			.addSource(new FlinkKafkaConsumer082<>(
                                "sampletopic",
                                new SimpleStringSchema(),
                                kProperties))
			.rebalance()
			.map(
				new MapFunction<String, 
					Tuple6<String, String, Long, String, Long, Double>>() {
					private static final long serialVersionUID = 34_2016_10_19_001L;

					@Override
					public Tuple6<String, String, Long, String, Long, Double> map(String value) throws Exception {
						String[] splits = value.split("\\|");
						return new Tuple6<String, String, Long, String, Long, Double>(
							splits[FIELD_MESSAGE_ID], 
							splits[FIELD_DEVICE_ID],
							Long.parseLong(splits[FIELD_TIMESTAMP]),
							splits[FIELD_CATEGORY],
							Long.parseLong(splits[FIELD_MEASURE1]),
							Double.parseDouble(splits[FIELD_MEASURE2])
						);
					}
				}
			)
			.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessGenerator());

		// deduplicate on message ID
		WindowedStream stream_windowed_for_deduplication = stream_parsed_with_timestamps
			.keyBy(FIELD_MESSAGE_ID)
			.timeWindow(Time.of(5000, MILLISECONDS), Time.of(5000, MILLISECONDS));

		DataStream<Tuple6<String,String,Long,String,Long,Double>> stream_deduplicated = stream_windowed_for_deduplication			
			.apply(new WindowFunction<Tuple6<String, String, Long, String, Long, Double>, 
				Tuple6<String, String, Long, String, Long, Double>, Tuple, TimeWindow>() {
				// remove duplicates. cf http://stackoverflow.com/questions/35599069/apache-flink-0-10-how-to-get-the-first-occurence-of-a-composite-key-from-an-unbo
				
				@Override
				public void apply(Tuple tuple, TimeWindow window, Iterable<Tuple6<String, String, Long, String, Long, Double>> input, 
					Collector<Tuple6<String, String, Long, String, Long, Double>> out) throws Exception {
					out.collect(input.iterator().next());
				}
			});

		// Group by device ID, Category
		WindowedStream stream_windowed_for_groupby = stream_deduplicated
			.keyBy(FIELD_DEVICE_ID, FIELD_CATEGORY)
			.timeWindow(Time.of(5000, MILLISECONDS), Time.of(5000, MILLISECONDS));

		// add debug information on stream_windowed_for_groupby
		stream_windowed_for_groupby
			.apply(new WindowFunction<Tuple6<String, String, Long, String, Long, Double>, 
				Tuple2<String, String>, Tuple, TimeWindow>() {

				@Override
				public void apply(Tuple keyTuple, TimeWindow window, Iterable<Tuple6<String, String, Long, String, Long, Double>> input, 
					Collector<Tuple2<String, String>> out) throws Exception {

					for(Iterator<Tuple6<String, String, Long, String, Long, Double>> i=input.iterator(); i.hasNext();) {
                                                Tuple6<String, String, Long, String, Long, Double> value = i.next();

						out.collect(new Tuple2<String, String>(
							"v" + VERSION + "- stream_windowed_for_groupby - " + Instant.now().toString(), 
							"MESSAGE_ID=" + value.getField(0).toString() + ", "
							+ "DEVICE_ID=" + value.getField(1).toString() + ", "
							+ "TIMESTAMP=" + value.getField(2).toString() + ", "
							+ "time window start=" + (new Long(window.getStart()).toString()) + ", "
							+ "time window end=" + (new Long(window.getEnd()).toString()) + ", "
							+ "CATEGORY=" + value.getField(3).toString() + ", "
							+ "M1=" + value.getField(4).toString() + ", "
							+ "M2=" + value.getField(5).toString()
						));
					}
				}
			})
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


		// calculate sums for M1 and M2
		DataStream<Tuple5<String, String, String, Long, Double>> stream_with_aggregations = stream_windowed_for_groupby
			.apply(new WindowFunction<Tuple6<String, String, Long, String, Long, Double>, 
				Tuple5<String, String, String, Long, Double>, Tuple, TimeWindow>() {
			        // sum measures 1 and 2

				@Override
				public void apply(Tuple keyTuple, TimeWindow window, Iterable<Tuple6<String, String, Long, String, Long, Double>> input, 
					Collector<Tuple5<String, String, String, Long, Double>> out) throws Exception {

					long window_timestamp_milliseconds = window.getEnd();
					String device_id=keyTuple.getField(0); // DEVICE_ID
					String category=keyTuple.getField(1); // CATEGORY
					long sum_of_m1=0L;
					Double sum_of_m2=0.0d;

					for(Iterator<Tuple6<String, String, Long, String, Long, Double>> i=input.iterator(); i.hasNext();) {
                                                Tuple6<String, String, Long, String, Long, Double> item = i.next();
						sum_of_m1 += item.f4; // FIELD_MEASURE1
						sum_of_m2 += item.f5; // FIELD_MEASURE2
					}

					out.collect(new Tuple5<String, String, String, Long, Double>(
								windowTimeFormat.format(new Date(window_timestamp_milliseconds)),
								device_id, 
								category,
								sum_of_m1,
								sum_of_m2
							));
				}
			});

		// send aggregations to destination
		stream_with_aggregations
			.addSink(new CassandraTupleSink<Tuple5<String, String, String, Long, Double>>(
                                "INSERT INTO boontadata.agg_events"
                                        + " (window_time, device_id, category, m1_sum_downstream, m2_sum_downstream)"
                                        + " VALUES (?, ?, ?, ?, ?);",
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

		// execute program
		env.execute("io.boontadata.flink1.StreamingJob v" + VERSION + " (" + timeCharacteristic + ")");
	}
}
