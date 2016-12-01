# $BOONTADATA_DOCKER_REGISTRY/boontadata/pyclientbase
#
# VERSION   0.1

FROM continuumio/anaconda3

MAINTAINER boontadata <contact@boontadata.io>

RUN apt-get update && \
	apt-get install -y \
		git \
		build-essential \
		vim \
		telnet

RUN pip install kafka-python && \
	pip install cassandra-driver 

ENTRYPOINT ["init"]
