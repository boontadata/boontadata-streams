package com.aggregateKafkaStream;


import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.TimeWindows;




public class Main extends StreamingConfig 
{
    public static void main( String[] args )
    {
        Main main = new Main();
        main.run();
    }

	private void run() {
		IoTEvent iot = new IoTEvent();
		cassandraconf.createKeySpaceAndTable(Boolean.TRUE);
		
		KStreamBuilder builder = new KStreamBuilder();
		KStream<String, String> ms = builder.stream("sampletopic");

		
		// Parsing the message splitting the separator: |
		 ms
		 .mapValues(mess -> mess.split("\\|"))
		 
		 // Parse in IoTEvent
		 .mapValues(value -> {
				iot.setMessageId(value[0]);
				iot.setDeviceId(value[1]);
				iot.setTimestamp(value[2]);
				iot.setCategory(value[3]);
				iot.setMeasure1(value[4]);
				iot.setMeasure2(value[5]);
				
				return iot.getMessageId();
		 })
		 
		 // Getting new Key : messageId
		 .map((key, value)-> {
		//	 kvs.put(key, value);
			 return new KeyValue<>(value,  new IoTEvent(iot.messageId, iot.deviceId, 
						iot.timestamp, iot.category, iot.measure1, iot.measure2).toString());
		 })
		 
		 	/*
		 	 * Deduplication using the distinct of sql
			kcs = KStreamKcql.KStreamConverter(arg0);
			*/
			
		
		  // Deduplication 	
		  // Group By this Key and reduce with a window time
		
		.groupByKey()
	//	.count(TimeWindows.of(TimeUnit.SECONDS.toSeconds(5)),"count")
		.reduce((val1, val2) ->  val2, TimeWindows.of(TimeUnit.SECONDS.toSeconds(5000)),"distinct")
		.toStream()
		
		.map((key, value) -> {
			String[] val = value.split(",");
			return new KeyValue<>(val[1] + "," + val[3], value);
			})
		
		// Aggregation by key on deviceId, category 
		
		.groupByKey()
		//.count("count")	
		.aggregate(() -> 0 + "," + 0 , (aggkey,value,aggregate) -> { 
			String[] pVal = value.split(",");
			String[] pAgg = aggregate.split(",");
			int res = Integer.parseInt(pVal[4]) + Integer.parseInt(pAgg[0]);
			float resf = Float.valueOf(pVal[5]) + Float.valueOf(pAgg[1]);
			return res + "," + resf;
					},TimeWindows.of(TimeUnit.SECONDS.toSeconds(5000)) ,Serdes.String(), "aggregation")
		
		.toStream()
	//	.foreach((key, value) -> System.out.println(key + "=>" + value));
		
		// write in cassandra
			
		.foreach((key, value) -> {
			String e = StringUtils.remove(String.valueOf(key), "[");
			String k = StringUtils.remove(e, "]");
			String device_id = k.split(",")[0];
			String cat_timest = k.split(",")[1];
			String category = cat_timest.split("@")[0].trim();
			String window_time = cassandraconf.getTimestamp(cat_timest.split("@")[1]);
			String m1_sum_downstream = value.split(",")[0];
			String m2_sum_downstream = value.split(",")[1];
		    cassandraconf.WriteInCassandra(device_id, category, window_time, m1_sum_downstream, m2_sum_downstream);
			});
		 
		
		

		KafkaStreams streams = new KafkaStreams(builder, config);
		streams.start();
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
		
	}
}
