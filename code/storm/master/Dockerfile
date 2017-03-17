# $BOONTADATA_DOCKER_REGISTRY/boontadata/stormmaster
#
# VERSION   0.1

FROM $BOONTADATA_DOCKER_REGISTRY/boontadata/stormbase:0.1

MAINTAINER boontadata <contact@boontadata.io>

WORKDIR /workdir
ADD code/*.sql .
#ADD code/*.sh ./
#RUN chmod a+x *.sh

CMD [ "/bin/bash" ]