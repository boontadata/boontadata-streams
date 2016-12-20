# $BOONTADATA_DOCKER_REGISTRY/boontadata/devjvm
#
# VERSION   0.1

#FROM openjdk:8
FROM ubuntu:16.04

RUN apt update && \
  apt -y upgrade && \
  apt install -y vim && \
  apt install -y software-properties-common && \
  add-apt-repository -y ppa:openjdk-r/ppa && \
  apt update && \
  apt install -y openjdk-8-jdk && \ 
  apt install -y maven && \
  apt install -y scala && \
  apt install -y curl && \
  apt install -y apt-transport-https && \
  echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
  apt update && \
  apt install -y sbt && \
  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

WORKDIR /usr/src/dev

ENTRYPOINT ["init"]
