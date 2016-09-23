FROM pyclientbase

MAINTAINER boontadata <contact@boontadata.io>

WORKDIR /workdir

ADD *.py ./
ADD *.sh ./
RUN chmod a+x *.sh

ENTRYPOINT ["init"]
