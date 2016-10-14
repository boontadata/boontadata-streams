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
  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

WORKDIR /usr/src/dev

ENTRYPOINT ["init"]
