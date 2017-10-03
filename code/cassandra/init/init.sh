#!/bin/bash

echo "about to sleep.."
sleep 60
echo "awake now..."
python /workdir/init.py
# cqlsh cassandra1 -f /data/init.cql
