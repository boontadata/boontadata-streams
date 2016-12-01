# $BOONTADATA_DOCKER_REGISTRY/boontadata/cassandrainit
#
# VERSION   0.2

FROM $BOONTADATA_DOCKER_REGISTRY/boontadata/cassandra:0.2

MAINTAINER boontadata <contact@boontadata.io>

RUN mkdir /data  
ADD *.cql /data/
ADD *.sh /data/
RUN chmod a+x /data/*.sh

CMD ["/data/init.sh", ""]
