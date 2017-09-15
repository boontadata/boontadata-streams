# $BOONTADATA_DOCKER_REGISTRY/boontadata/flinkbase
#
# VERSION   0.2

# from https://github.com/melentye/flink-docker
FROM shouldbee/scala:2.11.7-openjdk8

ENV HADOOP_VERSION 2.7.0
ENV FLINK_VERSION 1.2.0
ENV SCALA_VERSION 2.11
ENV FLINK_ROOT_DIR /opt/flink

# Get Hadoop from Apache mirror and extract just the native
# libs. (Until we care about running HDFS with these containers, this
# is all we need.)
RUN mkdir -p /opt && \
    cd /opt && \
    curl -O http://wwwftp.ciril.fr/pub/apache/flink/flink-${FLINK_VERSION}/flink-${FLINK_VERSION}-bin-hadoop27-scala_${SCALA_VERSION}.tgz && \
        tar xvfz flink-${FLINK_VERSION}-bin-hadoop27-scala_${SCALA_VERSION}.tgz && \
    ln -s hadoop-${HADOOP_VERSION} hadoop && \
    echo Hadoop ${HADOOP_VERSION} native libraries installed in /opt/hadoop/lib/native

# Get Flink from Apache mirror.
RUN mkdir -p /opt && \
    cd /opt && \
    curl http://archive.apache.org/dist/flink/flink-${FLINK_VERSION}/flink-${FLINK_VERSION}-bin-hadoop27-scala_${SCALA_VERSION}.tgz | \
        tar -zx && \
    ln -s flink-${FLINK_VERSION} flink && \
    echo Flink ${FLINK_VERSION} installed in /opt


ADD log4j.properties logback.xml ${FLINK_ROOT_DIR}/conf/
ADD start-common.sh start-worker.sh start-master.sh ${FLINK_ROOT_DIR}/bin/
ADD flink-conf.yaml ${FLINK_ROOT_DIR}/conf/flink-conf.yaml
ENV PATH $PATH:${FLINK_ROOT_DIR}/bin
