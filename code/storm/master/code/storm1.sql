CREATE EXTERNAL TABLE IOTMESSAGES 
    (msgid STRING PRIMARY KEY, deviceid STRING, devicetime: TIMESTAMP PRIMARY KEY, category: STRING, measure1 INT, measure2 FLOAT) 
    LOCATION 'kafka://zk1:2181/brokers?sampletopic' 
    TBLPROPERTIES '{"producer":{"bootstrap.servers":"ks1:9092,ks2:9092,ks3:9092","acks":"1","key.serializer":"org.apache.org.apache.storm.kafka.ByteBufferSerializer","value.serializer":"org.apache.org.apache.storm.kafka.ByteBufferSerializer"}}'

CREATE EXTERNAL TABLE AGGREGATES
    (deviceid STRING PRIMARY KEY, category STRING PRIMARY KEY, timewindow STRING PRIMARY KEY, sumofmeasure1 INT, sumofmeasure2 FLOAT) 
    LOCATION 'cassandra:xxx' 
    TBLPROPERTIES 'xxx'

/* aggregate */
SELECT
    System.TimeStamp AS wt, di, c, SUM(m1) AS sm1, SUM(m2) AS sm2, count(*) AS nbevents
INTO
    [docdb]
FROM
    (
        /* remove duplicates */
        SELECT
            /* id=message_id, di=device_id, dt=device_time, c=category, m1=measure1, m2=measure2 */
            id, di, dt, c, m1, m2, COUNT(*) AS dummy
        FROM
            [iothub] TIMESTAMP BY DATEADD(millisecond, dt, '1970-01-01T00:00:00Z')
        GROUP BY id, di, dt, c, m1, m2, TumblingWindow(second, 5)
    )
GROUP BY
    di, c, TumblingWindow(second, 5)

/*
17 MAR 2017
Stop here - per https://storm.apache.org/releases/1.0.3/storm-sql.html:
Current Limitations
Aggregation, windowing and joining tables are yet to be implemented. Specifying parallelism hints in the topology is not yet supported. All processors have a parallelism hint of 1.
Users also need to provide the dependency of the external data sources in the extlib directory. Otherwise the topology will fail to run because of ClassNotFoundException.
The current implementation of the Kafka connector in StormSQL assumes both the input and the output are in JSON formats. The connector has not yet recognized the INPUTFORMAT and OUTPUTFORMAT clauses yet.
*/
