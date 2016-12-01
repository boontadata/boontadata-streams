CREATE KEYSPACE IF NOT EXISTS boontadata
    WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 }
    AND DURABLE_WRITES = true;

USE boontadata;

CREATE TABLE IF NOT EXISTS boontadata.raw_events (
    message_id text,
    device_id text,
    device_time timestamp,
    send_time timestamp,
    category text,
    measure1 bigint,
    measure2 double,
    PRIMARY KEY (message_id, send_time));

INSERT INTO boontadata.raw_events 
    (message_id, device_id, device_time, send_time, category, measure1, measure2) 
    VALUES ('sampledevice-1', 'sampledevice', 1472209316326, 1472209318532, 'sample', 100, 1234.56);

CREATE TABLE IF NOT EXISTS boontadata.agg_events (
    window_time text,
    device_id text,
    category text,
    m1_sum_ingest_sendtime bigint,
    m1_sum_ingest_devicetime bigint,
    m1_sum_spark_processingtime bigint,
    m1_sum_flink_eventtime bigint,
    m2_sum_ingest_sendtime double,
    m2_sum_ingest_devicetime double,
    m2_sum_spark_processingtime double,
    m2_sum_flink_eventtime double,
    PRIMARY KEY (device_id, category, window_time))
    WITH CLUSTERING ORDER BY (category ASC, window_time ASC);

CREATE TABLE IF NOT EXISTS boontadata.debug (
    id text,
    message text,
    PRIMARY KEY (id));

INSERT INTO boontadata.debug 
    (id, message) 
    VALUES ('sample-1', 'this is a sample debug message');
