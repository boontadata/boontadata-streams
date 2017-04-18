# boontadata - Streams

## Introduction

An IOT devices simulator sends data to Kafka broker. It also writes its version of the truth to a Cassandra database. 

Stream engines (Spark Streaming, Flink, Kafka Streams, Storm, etc.) consume data from the broker and write their version of the truth to the same Cassandra database.

This repo is meant to be run on Docker hosts and use open source software.

Some code compares the results between the simulator's version of the truth and the stream engines versions.  

![](doc/diagrams/arc1.png)

The goals are to have code that shows how to do with different frameworks, compare the capabilities of different engines.

Additional goals include comparing performances, resilience to node failures, ...

[... more](doc/scenario.md)

## In which state is the project right now

You can consult [typical execution logs](doc/sample_execution_logs/).

An injector sends data to Kafka. It may send duplicates, send out of order, or send late. 
The injector also aggregates what it sent and saves its version of the truth to Cassandra.
One injector simulates one device. You can use several instances to simulate several devices. 
Compare.py compares what the injector sent from a device time and a send time perspective. 

The following streaming engine scenarios (cf code/runscenario.sh for more) are available: 
- flink1: consume from Apache Flink with time windows calculated based on processing time
- flink2: consume from Apache Flink with time windows calculated based on event time (extracted from the messages)
- spark1: consume from Apache Spark Streaming with time windows calculated based on processing time

## Contribute

This is, and will be, a work in progress. 
If you are interested in contributing, please tweet us [@boontadata](https://twitter.com/@boontadata) or just leverage GitHub!

## Why boontadata? 

You may want to search for ["Boonta Eve Classic"](https://www.bing.com/search?q=boonta+eve+classic).
