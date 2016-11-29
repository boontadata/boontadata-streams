#!/bin/bash

#usage: . buildimages.sh <reset: true|false>
if test $# -lt 1; then reset=false; else reset=$1; fi

if test -z $BOONTADATA_DOCKER_REGISTRY
then
    echo BOONTADATA_DOCKER_REGISTRY variable must not be null or empty
    echo you also need to login with `docker login`
    return 1
fi

if test -z $BOONTADATA_HOME
then
    echo BOONTADATA_HOME variable must not be null or empty
    return 1
fi

cd $BOONTADATA_HOME/code

docker build -t $BOONTADATA_DOCKER_REGISTRY/pyclientbase ./pyclientbase
docker push $BOONTADATA_DOCKER_REGISTRY/pyclientbase

docker build -t $BOONTADATA_DOCKER_REGISTRY/kafkaserver ./kafka-docker
docker push $BOONTADATA_DOCKER_REGISTRY/kafkaserver

docker build -t $BOONTADATA_DOCKER_REGISTRY/flink ./flink/base
docker push $BOONTADATA_DOCKER_REGISTRY/flink

