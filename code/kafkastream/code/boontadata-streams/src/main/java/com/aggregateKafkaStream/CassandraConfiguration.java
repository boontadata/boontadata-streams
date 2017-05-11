package com.aggregateKafkaStream;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.tuplejump.kafka.connect.cassandra.CassandraCluster;

import scala.collection.JavaConversions;
import scala.collection.Seq;

public class CassandraConfiguration {
	
	private Session session;
	private Seq seed;
	
	protected CassandraConfiguration()  {
		
	
	try {
		seed = JavaConversions.asScalaBuffer(Arrays.asList(InetAddress.getByName("localhost")));
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		
	CassandraCluster cc =  new CassandraCluster(seed, 9042,null, 8000, 120000, 1000,6000, 
			new ProtocolOptions().getCompression().LZ4, ConsistencyLevel.ONE);
	session = cc.session();
	
	}
	
	public void createKeySpaceAndTable(Boolean dropTable){
		dropTable = Boolean.FALSE;
		    session.execute(
		      "CREATE KEYSPACE  if not exists boontadata WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 } "
		      + "AND DURABLE_WRITES = true ;");
		    if (dropTable = Boolean.TRUE)
		      session.execute("drop table if exists boontadata.agg_events");

		    session.execute(
		      "create table if not exists boontadata.agg_events ( window_time  text, device_id text, category text, m1_sum_ingest_sendtime bigint,"
		      + " m1_sum_ingest_devicetime bigint, m1_sum_downstream text, m2_sum_ingest_sendtime double, "
		      + "m2_sum_ingest_devicetime double, m2_sum_downstream text, PRIMARY KEY (device_id, category, window_time))"
		      + " WITH CLUSTERING ORDER BY (category ASC, window_time ASC)");
		  
		   session.execute(
		      "create table if not exists boontadata.raw_events ( message_id text, device_id text, device_time timestamp, "
		      + "send_time timestamp, category text, measure1 bigint, measure2 double, PRIMARY KEY (message_id, send_time))");
		   
		   session.execute(
		      "insert into boontadata.raw_events ( message_id, device_id, device_time, send_time , category, measure1, measure2) "
		        + " values ('sampledevice-1', 'sampledevice', 1472209316326, 1472209318532, 'sample', 100, 1234.56)") ;
		  }
	
	/*
	public PreparedStatement prepareCasssandra(){
		return session.prepare(
				"insert into boontadata.agg_events (device_id, category, window_time, m1_sum_downstream, m2_sum_downstream)"
				+ "values (?,?,?,?,?)");
	}
	*/
	
	public void WriteInCassandra(String device_id, String category, String window_time, String m1_sum_downstream, String m2_sum_downstream ){
		//BoundStatement bnd = pst.bind(device_id,category, window_time, m1_sum_downstream, m2_sum_downstream);
		session.execute(String.format("insert into boontadata.agg_events (device_id, category, window_time, m1_sum_downstream, m2_sum_downstream)"
				+ "values ('%s','%s','%s','%s','%s')",device_id,category, window_time, m1_sum_downstream, m2_sum_downstream));	
	}
	 
	public String getTimestamp(String timestamp){
		Long timeL = Long.valueOf(timestamp) + 5000L;
		Date ts = new Date(timeL);
		Calendar cal = Calendar.getInstance();
		cal.setTime(ts);
		SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		form.setTimeZone(TimeZone.getTimeZone("UTC"));
		return form.format(cal.getTime());
	}
	
		
		
	}

	
	
	
	



