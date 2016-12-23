#!/bin/bash

if test $# -eq 0
then
    command=$0
    echo "usage: $command <sourcespath>"
    echo "<sourcespath> exemple: $BOONTADATA_HOME/code/spark/master/code"
    return 0
fi

sourcespath=$1

devjvmimage="$BOONTADATA_DOCKER_REGISTRY/boontadata/devjvm:0.1"
docker run --name devjvm -d \
    -v $BOONTADATA_HOME/dockervolumesforcache/maven-m2:/root/.m2 \
    -v $BOONTADATA_HOME/dockervolumesforcache/sbt-ivy2:/root/.ivy2 \
    -v $BOONTADATA_HOME/dockervolumesforcache/sbt-sbt:/root/.sbt \
    -v $sourcespath:/usr/src/dev \
    -w /usr/src/dev $devjvmimage

echo "devjvm container can be used to edit the sources"
return 0
