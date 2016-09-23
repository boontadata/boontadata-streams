#!/bin/bash

n=$1
# make sure command line arguments are passed to the script
if [ $# -eq 0 ]
then
	echo "send data from a number of devices."
	echo "Usage : $0 number_of_devices"
	exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

for i in $(seq $n)
do
	python $DIR/ingest.py &
done
