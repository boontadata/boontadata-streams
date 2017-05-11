package com.aggregateKafkaStream;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;


public abstract class StreamingConfig extends IoTEvent{ 
	
	protected StreamsConfig config;
	protected CassandraConfiguration cassandraconf;
	
	protected StreamingConfig(){
		
		Properties settings = new Properties();
		settings.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams");
		settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		settings.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, "localhost:33472");
		settings.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		settings.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		settings.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
		
		config = new StreamsConfig(settings);
		cassandraconf = new CassandraConfiguration() {
		};
		
	}
	
    
}
