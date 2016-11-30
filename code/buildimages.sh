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
    filepath=$folderpath/Dockerfile
    filepath2=$folderpath/tmpDockerfile
    tagname=$(eval echo "`head -1 $filepath | awk '{print $2}'`")
    tagversion=`head -3 $filepath | tail -1| awk '{print $3}'`
    fulltag="$tagname:$tagversion"

    if test -e $filepath2; then rm $filepath2; fi
    replacestring="s/\$BOONTADATA_DOCKER_REGISTRY/${BOONTADATA_DOCKER_REGISTRY}/g"
    sed $replacestring $filepath > $filepath2

    if test $reset = "reset"
    then
        echo "will reset image $fulltag"
        docker rmi $fulltag
    fi

    docker build -t $fulltag $folderpath --file tmpDockerfile
    docker push $fulltag
}

build_and_push $BOONTADATA_HOME/code/pyclientbase
build_and_push $BOONTADATA_HOME/code/pyclient
build_and_push $BOONTADATA_HOME/code/cassandrainit
build_and_push $BOONTADATA_HOME/code/devjvm
build_and_push $BOONTADATA_HOME/code/flink/base
build_and_push $BOONTADATA_HOME/code/flink/master
build_and_push $BOONTADATA_HOME/code/flink/worker
build_and_push $BOONTADATA_HOME/code/kafka-docker
build_and_push $BOONTADATA_HOME/code/spark
