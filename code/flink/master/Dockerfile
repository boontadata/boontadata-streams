FROM melentye/flink

MAINTAINER boontadata <contact@boontadata.io>

RUN apt-get update && \
	apt-get install -y \
		maven

WORKDIR /workdir

ADD code/* ./

CMD ["/opt/flink/bin/start-master.sh"]
