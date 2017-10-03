# $BOONTADATA_DOCKER_REGISTRY/boontadata/pyclient
#
# VERSION   0.1

FROM boontadata/pyclientbase:0.1

WORKDIR /workdir

ADD *.py ./
ADD *.sh ./
RUN chmod a+x *.sh

ENTRYPOINT ["init"]
