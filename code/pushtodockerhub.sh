#!/bin/bash

echo "you must be logged in to the Docker registry as boontadata (^C or ENTER)"
read


# NB: in the following statement, 11 is the length of the "boontadata/" string
for img in `docker images | grep $BOONTADATA_DOCKER_REGISTRY | awk -v len=${#BOONTADATA_DOCKER_REGISTRY} '{ $1=substr($1, len+2+11, length($1)-len-11); print $1 ":" $2}'`
do
    docker tag $BOONTADATA_DOCKER_REGISTRY/boontadata/$img boontadata/$img
    docker push boontadata/$img
done
