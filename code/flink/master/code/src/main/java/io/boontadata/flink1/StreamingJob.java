package io.boontadata.flink1;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.Window;
import org.apache.flink.util.Collector;

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

	Int FIELD_MESSAGE_ID = 0;
	Int FIELD_DEVICE_ID = 1;
	Int FIELD_TIMESTAMP = 2;
	Int FIELD_CATEGORY = 3;
	Int FIELD_MEASURE1 = 4;
	Int FIELD_MESAURE2 = 5; 

	public static void main(String[] args) throws Exception {
		// set up the streaming execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.enableCheckpointing(5000); // checkpoint every 5000 msecs
		env.setParallelism(4); // may change 4 into something else...

		Properties kProperties = new Properties();
		kProperties.setProperty("bootstrap.servers", "ks1:9092,ks2:9092,ks3:9092")
		kProperties.setProperty("zookeeper.connect", "zk1:2181")
		kProperties.setProperty("group.id", "ReadKafkaWithFlink")

		// cf https://ci.apache.org/projects/flink/flink-docs-master/dev/connectors/kafka.html
		FlinkKafkaConsumer082<String> kConsumer = new FlinkKafkaConsumer082<String>( 
		    "sampletopic", 
		    new SimpleStringSchema(), 
			kProperties);

		// cf https://ci.apache.org/projects/flink/flink-docs-release-1.1/apis/streaming/connectors/cassandra.html
		CassandraSink cWriter = CassandraSink.addSink(inputXXX)
			.setQuery("INSERT INTO agg_events"
				+ " (window_time, device_id, category, m1_sum_flink_eventtime, m2_sum_flink_eventtime)"
				+ " VALUES (?, ?, ?, ?, ?);")
			.setClusterBuilder(new ClusterBuilder() {
				@Override
				public Cluster buildCluster(Cluster.Builder builder) {
					return builder
						.addContactPoint("cassandra1").withPort(9042)
						.addContactPoint("cassandra2").withPort(9042)
						.addContactPoint("cassandra3").withPort(9042)
						.build();
				}
			})
			.build();

		env
			.addSource(kConsumer)
			.map(
				new MapFunction<String, 
					Tuple6<String, String, Long, String, Long, Float>>() {
					private static final long serialVersionUID = 324779859156071509341591370454649244437L;

					@Override
					public Tuple6<String, String, Long, String, Long, Float> map(String value) throws Exception {
						String[] splits = value.split("|");
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
			.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessGenerator());
			.keyBy(FIELD_MESSAGE_ID)
			.timeWindow(Time.of(5000, MILLISECONDS), Time.of(5000, MILLISECONDS))
			.apply(new WindowFunction<Iterable<Tuple6<String, String, Long, String, Long, Float>>, 
				Tuple6<String, String, Long, String, Long, Float>, Tuple, TimeWindow>() {
				// remove duplicates. cf http://stackoverflow.com/questions/35599069/apache-flink-0-10-how-to-get-the-first-occurence-of-a-composite-key-from-an-unbo
				
				@Override
				public void apply(Tuple tuple, TimeWindow window, Iterable<Tuple6<String, String, Long, String, Long, Float>> input, 
					Collector<Tuple6<String, String, Long, String, Long, Float>> out) throws Exception {
					out.collect(input.iterator().next());
				}
			})
 			.keyBy(FIELD_DEVICE_ID, FIELD_CATEGORY)
			.timeWindow(Time.of(5000, MILLISECONDS), Time.of(5000, MILLISECONDS))
			.apply(new WindowFunction<Iterable<Tuple6<String, String, Long, String, Long, Float>>, 
				Tuple5<Long, String, String, Long, Float>, Tuple, TimeWindow>() {
			    // sum measures 1 and 2

				@Override
				public void apply(Tuple tuple, TimeWindow window, Iterable<Tuple6<String, String, Long, String, Long, Float>> input, 
					Collector<Tuple5<Long, String, String, Long, Float>> out) throws Exception {

					Long window_timestamp_milliseconds = window.XXX();
					String device_id=input[0].f1; // FIELD_DEVICE_ID
					String category=input[0].f3; // FIELD_CATEGORY
					Long sum_of_m1=0L;
					Float sum_of_m2=0.;

					for(Tuple6<String, String, Long, String, Long, Float> i in input) {
						sum_of_m1 += i.f4; // FIELD_MEASURE1
						sum_of_m2 += i.f5; // FIELD_MESAURE2
					}

					out.collect(new Tuple5<>(
								window_timestamp_milliseconds,
								device_id, 
								category,
								sum_of_m1,
								sum_of_m2
							));
				}
			})
			.addSink(cWriter)
			.print();

		// execute program
		env.execute("io.boontadata.flink1.StreamingJob");
	}
}

// cf https://ci.apache.org/projects/flink/flink-docs-release-1.1/apis/streaming/event_timestamps_watermarks.html
/**
 * This generator generates watermarks assuming that elements come out of order to a certain degree only.
 * The latest elements for a certain timestamp t will arrive at most n milliseconds after the earliest
 * elements for timestamp t.
 */
public class BoundedOutOfOrdernessGenerator extends AssignerWithPeriodicWatermarks<Tuple6<String, String, Long, String, Long, Float>> {

    private final long maxOutOfOrderness = 5000; // 5 seconds

    private long currentMaxTimestamp;

    @Override
    public long extractTimestamp(Tuple6<String, String, Long, String, Long, Float> element, long previousElementTimestamp) {
        long timestamp = element.f2; // get processing timestamp from current event
        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp);
        return timestamp;
    }

    @Override
    public Watermark getCurrentWatermark() {
        // return the watermark as current highest timestamp minus the out-of-orderness bound
        return new Watermark(currentMaxTimestamp - maxOutOfOrderness);
    }
}
