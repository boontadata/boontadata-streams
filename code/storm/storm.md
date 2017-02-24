# Storm implementation

## Framework 

Trident is used in order to have simple windowing capacity.

## Topology

The Topology is the following: 
- [SK] spout: Kafka
- [BD] bolt: deduplicate
- [BS] bolt: sum
- [BC] bolt: store into Cassandra

```
[SK] -> [BD] -> [BS] -> [BC]
```

## State is the following

Initial workload: 
- msgid: message id (string)
- devid: device id (string)
- devts: timestamp (milliseconds since epoch)
- cat: category (string)
- m1: measure1 (integer)
- m2: measure2 (float)

we will also have the tw: time window (milliseconds since epoch)


Step | Key structure | Value structure
---- | ------------- | ---------------
[SK] input  | N/A | N/A
[SK] output / [BD] input | offset | msgid, devid, devts, cat, m1, m2
[BD] output / [BS] input | tw, msgid | devid, devts, cat, m1, m2
[BS] output / [BC] input | tw, devid, cat | sum(m1), sum(m2)
[BC] output | N/A