# $BOONTADATA_DOCKER_REGISTRY/boontadata/cassandrainit
#
# VERSION   0.2

# FROM gcr.io/google-samples/cassandra:v8

# MAINTAINER boontadata <contact@boontadata.io>

# RUN mkdir -p /opt/data  
# ADD *.cql /opt/data/
# ADD *.sh /opt/data/
# RUN chmod a+x /opt/data/*.sh

# WORKDIR /opt/data/

# CMD ["sh","-c","init.sh"]


# $BOONTADATA_DOCKER_REGISTRY/boontadata/pyclient
#
# VERSION   0.1

FROM boontadata/pyclientbase:0.1

WORKDIR /workdir

ADD *.py ./
ADD *.sh ./
RUN chmod a+x *.sh

# CMD ["sh","-c","python init.py"]

CMD [ "/workdir/init.sh" ]

# CMD [ "sh","-c", "/workdir/init.sh 1>/workdir/log.txt 2>/workdir/log.txt" ]

