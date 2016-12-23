# $BOONTADATA_DOCKER_REGISTRY/boontadata/sparkmaster
#
# VERSION   0.1

FROM $BOONTADATA_DOCKER_REGISTRY/boontadata/sparkbase:0.1

MAINTAINER boontadata <contact@boontadata.io>

WORKDIR /workdir
ADD code/target/scala-2.11/boontadata-spark-job1-assembly-0.1.jar ./
ADD code/*.sh ./
RUN chmod a+x *.sh

CMD [ "/bin/bash" ]
