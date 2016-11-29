#!/bin/bash

if test $# -eq 0
then
    command=$0
    echo "usage: $command <scenario>"
    echo "scenario can be flink, spark, ..."
    return 0
fi

scenario=$1

if test -z $BOONTADATA_HOME
then
    echo BOONTADATA_HOME variable must not be null or empty
    return 1
fi

cd $BOONTADATA_HOME/code
if test -e docker-compose.yml; then rm -v docker-compose.yml; fi
echo composing docker-compose.yml from compose-blocks
cp compose-blocks/common-start.yml docker-compose.yml
cat compose-blocks/${scenario}.yml >> docker-compose.yml
cat compose-blocks/common-end.yml >> docker-compose.yml

export HOSTIP=`hostname -i`

#DOCKER_NETWORK_TYPE
#- bridge for single host node 
#- overlay for multi host nodes 
export DOCKER_NETWORK_TYPE=bridge

echo starting scenario $scenario 

docker-compose up -d

echo use docker-compose ps to see the containers, docker-compose down to shut down the containers.
return 0
