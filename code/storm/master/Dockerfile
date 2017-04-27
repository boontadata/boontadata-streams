# $BOONTADATA_DOCKER_REGISTRY/boontadata/stormmaster
#
# VERSION   0.2

FROM $BOONTADATA_DOCKER_REGISTRY/boontadata/stormbase:0.2

MAINTAINER boontadata <contact@boontadata.io>

WORKDIR /workdir
#ADD code/target/boontadata-storm1-0.1.jar .
ADD codesampleafac/target/trident-hackaton-0.0.1-SNAPSHOT-jar-with-dependencies.jar .
#ADD code/*.sh ./
#RUN chmod a+x *.sh

CMD [ "/bin/bash" ]