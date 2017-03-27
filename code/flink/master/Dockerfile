# $BOONTADATA_DOCKER_REGISTRY/boontadata/flinkmaster
#
# VERSION   0.2

FROM $BOONTADATA_DOCKER_REGISTRY/boontadata/flinkbase:0.2

MAINTAINER boontadata <contact@boontadata.io>

WORKDIR /workdir
ADD code/target/flink1-0.2.jar .

CMD ["/opt/flink/bin/start-master.sh"]
