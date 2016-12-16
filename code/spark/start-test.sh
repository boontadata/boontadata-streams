#!/bin/bash

#see packages in search.maven.org
spark-submit --packages org.apache.spark:spark-streaming-kafka-0-8-assembly_2.11:2.0.2 /workdir/test.py
