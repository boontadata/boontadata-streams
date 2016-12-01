# $BOONTADATA_DOCKER_REGISTRY/boontadata/pyclient
#
# VERSION   0.1

FROM $BOONTADATA_DOCKER_REGISTRY/boontadata/pyclientbase:0.1

MAINTAINER boontadata <contact@boontadata.io>

WORKDIR /workdir

ADD *.py ./
ADD *.sh ./
RUN chmod a+x *.sh

ENTRYPOINT ["init"]
