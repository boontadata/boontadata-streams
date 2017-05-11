package com.aggregateStructuredStream

import java.sql.Timestamp

import com.datastax.driver.core.Session

object Statements extends Serializable {

  def cql(device_id: String, category:String, window_time: String, m1_sum_downstream: String, m2_sum_downstream: String): String = s"""
       insert into boontadata.agg_events (device_id, category, window_time,m1_sum_downstream,m2_sum_downstream)
       values('$device_id','$category', '$window_time', '$m1_sum_downstream','$m2_sum_downstream')"""

  def createKeySpaceAndTable(session: Session, dropTable: Boolean = false) = {
    session.execute(
      """CREATE KEYSPACE  if not exists boontadata WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 } AND DURABLE_WRITES = true ;""")
    if (dropTable)
      session.execute("""drop table if exists boontadata.agg_events""")

    session.execute(
      """create table if not exists boontadata.agg_events ( window_time  text, device_id text, category text, m1_sum_ingest_sendtime bigint, m1_sum_ingest_devicetime bigint, m1_sum_downstream text, m2_sum_ingest_sendtime double, m2_sum_ingest_devicetime double, m2_sum_downstream text, PRIMARY KEY (device_id, category, window_time)) WITH CLUSTERING ORDER BY (category ASC, window_time ASC)""")
  
   session.execute(
      """create table if not exists boontadata.raw_events ( message_id text, device_id text, device_time timestamp, send_time timestamp, category text, measure1 bigint, measure2 double, PRIMARY KEY (message_id, send_time))""")
   
   session.execute(
      """insert into boontadata.raw_events ( message_id, device_id, device_time, send_time , category, measure1, measure2)
         values ('sampledevice-1', 'sampledevice', 1472209316326, 1472209318532, 'sample', 100, 1234.56)""") 
  }
}
