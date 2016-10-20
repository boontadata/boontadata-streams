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
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.streaming.connectors.cassandra.CassandraTupleSink;
import org.apache.flink.streaming.connectors.cassandra.ClusterBuilder;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumerBase;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer08;
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
public class StreamingJob {

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
		env.setParallelism(4); // may change 4 into something else...

		Properties kProperties = new Properties();
		kProperties.setProperty("bootstrap.servers", "ks1:9092,ks2:9092,ks3:9092");
		kProperties.setProperty("zookeeper.connect", "zk1:2181");
		kProperties.setProperty("group.id", "ReadKafkaWithFlink");

		env
			.addSource(new FlinkKafkaConsumer08<String>(
                                "sampletopic",
                                new SimpleStringSchema(),
                                kProperties))
			.map(
				new MapFunction<String, 
					Tuple6<String, String, Long, String, Long, Float>>() {
					private static final long serialVersionUID = 2016_10_19_001L;

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
			.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessGenerator())
			.keyBy(FIELD_MESSAGE_ID)
			.timeWindow(Time.of(5000, MILLISECONDS), Time.of(5000, MILLISECONDS))
			.apply(new WindowFunction<Tuple6<String, String, Long, String, Long, Float>, 
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
			.apply(new WindowFunction<Tuple6<String, String, Long, String, Long, Float>, 
				Tuple5<Long, String, String, Long, Float>, Tuple, TimeWindow>() {
			        // sum measures 1 and 2

				@Override
				public void apply(Tuple keyTuple, TimeWindow window, Iterable<Tuple6<String, String, Long, String, Long, Float>> input, 
					Collector<Tuple5<Long, String, String, Long, Float>> out) throws Exception {

					long window_timestamp_milliseconds = window.getEnd();
					String device_id=keyTuple.getField(0); // DEVICE_ID
					String category=keyTuple.getField(1); // CATEGORY
					long sum_of_m1=0L;
					float sum_of_m2=0;

					for(Iterator<Tuple6<String, String, Long, String, Long, Float>> i=input.iterator(); i.hasNext();) {
                                                Tuple6<String, String, Long, String, Long, Float> item = i.next();
						sum_of_m1 += item.f4; // FIELD_MEASURE1
						sum_of_m2 += item.f5; // FIELD_MESAURE2
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
			.addSink(new CassandraTupleSink<Tuple5<Long, String, String, Long, Float>>(
                                "INSERT INTO agg_events"
                                        + " (window_time, device_id, category, m1_sum_flink_eventtime, m2_sum_flink_eventtime)"
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
		env.execute("io.boontadata.flink1.StreamingJob");
	}
}
