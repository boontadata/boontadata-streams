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

    if test $reset = "reset"
    then
        echo "will reset image $fulltag"
        docker rmi $fulltag
    fi

    #special build steps
    case $tagname in
        "$BOONTADATA_DOCKER_REGISTRY/boontadata/flinkmaster")
            jarfile="$BOONTADATA_HOME/code/flink/master/code/target/flink1-0.1.jar"
            if test -e $jarfile
            then
                if test $reset = "reset"
                then
                    echo "resetting $jarfile"
                    rm $jarfile
                else
                    echo "will not rebuild $jarfile which already exists"
                fi
            fi

            if test ! -e $jarfile
            then
                echo "will build $jarfile"
                devjvmimage="$BOONTADATA_DOCKER_REGISTRY/boontadata/devjvm:0.1"
                echo "will compile flink job"
                docker run --name devjvm -d \
                    -v $BOONTADATA_HOME/dockervolumesforcache/maven-m2:/root/.m2 \
                    -v $BOONTADATA_HOME/code/flink/master/code:/usr/src/dev \
                    -w /usr/src/dev $devjvmimage
                docker exec -ti devjvm mvn clean package
                docker rm -f devjvm
            fi
            ;;
        *)
            ;;
    esac

    imageavailability=`docker images | grep "$tagname *$tagversion"`
    if test -n "$imageavailability"
    then
        echo "local image $fulltag already exists, no reset so no rebuild"
    else
        echo "will build $fulltag"
        if test -e $filepath2; then rm $filepath2; fi
        replacestring="s/\$BOONTADATA_DOCKER_REGISTRY/${BOONTADATA_DOCKER_REGISTRY}/g"
        sed $replacestring $filepath > $filepath2

        docker build -t $fulltag $folderpath --file $filepath2
        echo "local docker images for $tagname:"
        docker images | grep "$tagname"
        docker push $fulltag
    fi
}

#create a container that we can use to build sources as jars
build_and_push $BOONTADATA_HOME/code/devjvm

#create other containers
build_and_push $BOONTADATA_HOME/code/pyclientbase
build_and_push $BOONTADATA_HOME/code/pyclient
build_and_push $BOONTADATA_HOME/code/cassandra/base
build_and_push $BOONTADATA_HOME/code/cassandra/init
build_and_push $BOONTADATA_HOME/code/flink/base
build_and_push $BOONTADATA_HOME/code/flink/master
build_and_push $BOONTADATA_HOME/code/flink/worker
build_and_push $BOONTADATA_HOME/code/kafka-docker
build_and_push $BOONTADATA_HOME/code/spark/base
build_and_push $BOONTADATA_HOME/code/spark/master
build_and_push $BOONTADATA_HOME/code/spark/worker
build_and_push $BOONTADATA_HOME/code/zookeeper

docker images