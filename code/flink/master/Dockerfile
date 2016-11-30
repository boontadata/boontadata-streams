# $BOONTADATA_DOCKER_REGISTRY/boontadata/flinkmaster
#
# VERSION   0.1

FROM $BOONTADATA_DOCKER_REGISTRY/boontadata/flinkbase:0.1

MAINTAINER boontadata <contact@boontadata.io>

#RUN apt-get update && \
#	apt-get install -y \
#		maven

WORKDIR /workdir

#ADD code/* ./
#RUN chmod a+x *.sh

CMD ["/opt/flink/bin/start-master.sh"]
