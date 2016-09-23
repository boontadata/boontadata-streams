from cassandra.cluster import Cluster

cluster=Cluster(['cassandra1', 'cassandra2', 'cassandra3'])
session=cluster.connect('boontadata')

session.execute("INSERT INTO raw_events (message_id, device_id, device_time, category, measure1, measure2) VALUES ('pysample-1', 'pysample', 1472222537503, "test", 100, 1234.56)")
rows = session.execute("SELECT * FROM raw_events")
for row in rows:
    print(row)

print("OK")
cluster.shutdown()
