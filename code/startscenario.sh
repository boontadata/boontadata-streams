#!/bin/bash

if test $# -eq 0
then
    command=$0
    echo "usage: $command <scenario> [<docker_network_type>]"
    echo "<scenario> can be flink, spark, ..."
    echo "<docker_network_type> is bridge by default. Other value is overlay."
    return 0
fi

scenario=$1

#DOCKER_NETWORK_TYPE
#- bridge for single host node 
#- overlay for multi host nodes 
if test $# -ge 2
then
    export DOCKER_NETWORK_TYPE=$2
else
    export DOCKER_NETWORK_TYPE=bridge
fi
echo "Network type will be : ${DOCKER_NETWORK_TYPE}. (bridge is for one host VM, overlay is for multiple host VMs)"

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


echo starting scenario $scenario 

docker-compose up -d

echo use docker-compose ps to see the containers, docker-compose down to shut down the containers.
return 0
