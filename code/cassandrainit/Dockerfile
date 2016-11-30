# $BOONTADATA_DOCKER_REGISTRY/boontadata/cassandra
#
# VERSION   0.1

FROM cassandra:3.7

MAINTAINER boontadata <contact@boontadata.io>

RUN mkdir /data  
ADD *.cql /data/
ADD *.sh /data/
RUN chmod a+x /data/*.sh

CMD ["/data/init.sh", ""]
