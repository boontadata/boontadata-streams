FROM sequenceiq/spark:1.6.0
#FROM gettyimages/spark:1.6.2-hadoop-2.6

MAINTAINER boontadata <contact@boontadata.io>

WORKDIR /workdir

ADD *.py ./
ADD *.sh ./
RUN chmod a+x *.sh

ENTRYPOINT ["init"]
