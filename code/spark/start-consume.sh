#!/bin/bash

#see packages in search.maven.org
spark-submit --packages org.apache.spark:spark-streaming-kafka-assembly_2.10:1.6.0 /workdir/consume.py
