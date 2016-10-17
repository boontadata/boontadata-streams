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

	public static void main(String[] args) throws Exception {
		// set up the streaming execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.enableCheckpointing(5000); // checkpoint every 5000 msecs

		Properties properties = new Properties();
		properties.setProperty("bootstrap.servers", "ks1:9092,ks2:9092,ks3:9092") //should replace by environment variable
		properties.setProperty("zookeeper.connect", "zk1:2181") //should replace by environment variable
		properties.setProperty("group.id", "ReadKafkaWithFlink")

		// cf https://ci.apache.org/projects/flink/flink-docs-master/dev/connectors/kafka.html
		FlinkKafkaConsumer082<String> kConsumer = new FlinkKafkaConsumer082<String>( 
		    "sampletopic", 
		    new SimpleStringSchema(), 
		    properties);

		// cf https://ci.apache.org/projects/flink/flink-docs-release-1.1/apis/streaming/connectors/cassandra.html
		CassandraSink cWriter = CassandraSink.addSink(input)
			.setHost("cassandra1:9042")
			.setQuery("INSERT INTO example.values (id, counter) values (?, ?);")
			.setClusterBuilder(new ClusterBuilder() {
				@Override
				public Cluster buildCluster(Cluster.Builder builder) {
					return builder.addContactPoint("127.0.0.1").build();
				}
			})
			.build();

		DataStream<String> messageStream = env
			.addSource(kConsumer)
			.addSink(cWriter)
			.print();

		messageStream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessGenerator());

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
public class BoundedOutOfOrdernessGenerator extends AssignerWithPeriodicWatermarks<String> {

    private final long maxOutOfOrderness = 5000; // 5 seconds

    private long currentMaxTimestamp;

    @Override
    public long extractTimestamp(MyEvent element, long previousElementTimestamp) {
        long timestamp = XXX // get processing timestamp from current event // element.getCreationTime();
        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp);
        return timestamp;
    }

    @Override
    public Watermark getCurrentWatermark() {
        // return the watermark as current highest timestamp minus the out-of-orderness bound
        return new Watermark(currentMaxTimestamp - maxOutOfOrderness);
    }
}

