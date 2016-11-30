#!/bin/bash

#usage: . buildimages.sh <reset|noreset>
if test $# -lt 1; then reset=noreset; else reset=$1; fi

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

build_and_push()
{
    folderpath=$1
    filepath=$1/Dockerfile
    tagname=$(eval echo "`head -1 $filepath | awk '{print $2}'`")
    tagversion=`head -3 $filepath | tail -1| awk '{print $3}'`
    fulltag="$tagname:$tagversion"

    if test $reset = "reset"
    then
        echo "will reset image $fulltag"
        docker rmi $fulltag
    fi

    docker build -t $fulltag $folderpath
    docker push $fulltag
}

build_and_push $BOONTADATA_HOME/code/cassandrainit
build_and_push $BOONTADATA_HOME/code/devscala
build_and_push 
build_and_push pyclientbase ./pyclientbase
build_and_push kafkaserver ./kafka-docker
build_and_push flink ./flink/base

