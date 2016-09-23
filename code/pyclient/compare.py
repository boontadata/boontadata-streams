from cassandra.cluster import Cluster
from cassandra.protocol import NumpyProtocolHandler
from cassandra.query import tuple_factory
import datetime
import math
import numpy
import os
import pandas
import time
import uuid

#connect to Cassandra
cluster=Cluster(['cassandra1', 'cassandra2', 'cassandra3'])
session=cluster.connect('boontadata')
session.row_factory = tuple_factory
session.client_protocol_handler = NumpyProtocolHandler

# this is an example. Many other things could be compared.
result = session.execute(
    "SELECT window_time, device_id, category, "
    + "m1_sum_ingest_sendtime, m1_sum_ingest_devicetime, m1_sum_spark_processingtime "
    + "FROM agg_events ")
df = pandas.DataFrame(result[0])
df['delta_m1_sum_ingest_send_device'] = df.apply(lambda row: row.m1_sum_ingest_sendtime - row.m1_sum_ingest_devicetime, axis=1)

#disconnect from Cassandra
cluster.shutdown()


print('Comparing ingest device and send for m1_sum')
print('-------------------------------------------')
print("{} exceptions out of {}"
    .format(
        len(df.query('delta_m1_sum_ingest_send_device != 0').index),
        len(df.index)
    ))
print()
print('Exceptions are:')
print(
    df.query('delta_m1_sum_ingest_send_device != 0')
    .loc[:,['window_time', 'device_id', 'category', 'm1_sum_ingest_sendtime', 'm1_sum_ingest_devicetime', 'delta_m1_sum_ingest_send_device']]
    )
