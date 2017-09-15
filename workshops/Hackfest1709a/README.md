# Hackfest 2017-09 a - move to Kubernetes

## Introduction

you can get familiar with boontadata by reading the [PyParis2017 workshop](../PyParis2017/README.md).

## run locally

- create an Ubuntu VM (latest LTS) with a size of D11_v2_promo or E2_V3
- [install Docker](InstallDocker.md)
- set some variables [1]
- get the code from GitHub: https://github.com/boontadata/boontadata-streams, branch: hf1709a. [1]
- build [2]
- run [2]

[1] Environment variables and get code: 

```bash
cd $HOME
export BOONTADATA_HOME=$HOME/boontadata-streams
export BOONTADATA_DOCKER_REGISTRY=boontadata.local
git clone https://github.com/boontadata/boontadata-streams.git
cd $BOONTADATA_HOME
git checkout hf1709a
```

[2] build and run

```bash
cd $BOONTADATA_HOME/code
. buildimages.sh noreset pullfromdockerhub
. startscenario.sh flink
docker-compose ps
cat docker-compose.yml
```

wait to have

```
             Name                             Command                            State                             Ports
-------------------------------------------------------------------------------------------------------------------------------------
cassandra1                        /docker-entrypoint.sh cass ...    Up                                0.0.0.0:34060->7000/tcp,
                                                                                                      0.0.0.0:34061->7001/tcp,
                                                                                                      0.0.0.0:34062->7199/tcp,
                                                                                                      0.0.0.0:34063->9042/tcp,
                                                                                                      0.0.0.0:34064->9160/tcp
cassandra2                        /docker-entrypoint.sh cass ...    Up                                0.0.0.0:34070->7000/tcp,
                                                                                                      0.0.0.0:34071->7001/tcp,
                                                                                                      0.0.0.0:34072->7199/tcp,
                                                                                                      0.0.0.0:34073->9042/tcp,
                                                                                                      0.0.0.0:34074->9160/tcp
cassandra3                        /docker-entrypoint.sh cass ...    Up                                0.0.0.0:34080->7000/tcp,
                                                                                                      0.0.0.0:34081->7001/tcp,
                                                                                                      0.0.0.0:34082->7199/tcp,
                                                                                                      0.0.0.0:34083->9042/tcp,
                                                                                                      0.0.0.0:34084->9160/tcp
cinit                             /docker-entrypoint.sh /dat ...    Exit 0
client1                           init                              Up
flink-master                      /opt/flink/bin/start-master.sh    Up                                0.0.0.0:34011->6123/tcp,
                                                                                                      0.0.0.0:34010->8081/tcp
flink-worker1                     /opt/flink/bin/start-worker.sh    Up                                0.0.0.0:34012->6121/tcp,
                                                                                                      0.0.0.0:34013->6122/tcp
flink-worker2                     /opt/flink/bin/start-worker.sh    Up                                0.0.0.0:34014->6121/tcp,
                                                                                                      0.0.0.0:34015->6122/tcp
ks1                               start-kafka.sh                    Up                                0.0.0.0:34001->9092/tcp
ks2                               start-kafka.sh                    Up                                0.0.0.0:34002->9092/tcp
ks3                               start-kafka.sh                    Up                                0.0.0.0:34003->9092/tcp
zk1                               /bin/sh -c /usr/sbin/sshd  ...    Up                                0.0.0.0:34050->2181/tcp,
                                                                                                      0.0.0.0:34053->22/tcp,
                                                                                                      0.0.0.0:34051->2888/tcp,
                                                                                                      0.0.0.0:34052->3888/tcp
```

- `cinit` created the schema of the Apache Cassandra database in the [`cassandra1`, `cassandra2`, `cassandra3`] Apache Cassandra cluster.
- `client1` will send messages to the [`ks1`, `ks2`, `ks3`, `zk1`] Apache Kafka cluster.
- `client1` will aggregate the messages it sent and write the results to the the [`cassandra1`, `cassandra2`, `cassandra3`] Apache Cassandra cluster.
- The [`flink-master`, `flink-worker1`, `flink-worker2`] Apache Flink cluster will read data from the [`ks1`, `ks2`, `ks3`, `zk1`] Apache Kafka cluster.
- The [`flink-master`, `flink-worker1`, `flink-worker2`] Apache Flink cluster will write its own aggregations to the [`cassandra1`, `cassandra2`, `cassandra3`] Apache Cassandra cluster.
- `client1` will read and compare aggregation results from `client1` and Apache Flink.

```bash
. runscenario.sh flink2
```

clear: 

```bash
docker-compose down
```

a similar scenario exists with Apache Spark Streaming instead of Apache Flink.

## about the registry

the `BOONTADATA_DOCKER_REGISTRY` environment variable points to the docker registry you want to use. 

It can be docker hub, a private registry like Azure Container registry, or you may want to keep everything local. For this last case, the special name `boontadata.local` can be used. 

This variable is used in `code/buildimages.sh`. The test about `boontadata.local` is in line 101.

## goal

The goal is to have the same scenario running on Kubernetes on multiple nodes, including Azure Container Instances.

This includes several tasks:

**Transform some container definitions** so that they can work while being hosted in several hosts. 
For instance, the Kafka nodes require the `/var/run/docker.sock:/var/run/docker.sock` volume.

**translate the docker-compose yml definition into a Kubernetes yml definition**.
NB: the yaml file itself is created from building blocks in the `startscenario.sh` bash script.

**Deploy a Kubernetes cluster to run this**.

**Also include a pod made of Azure Container Instances**.

## how to build all

should we need to build all, we may want to use this: 

```bash
cd $BOONTADATA_HOME/code
docker images | awk '{print $1 ":" $2}' | xargs --no-run-if-empty docker rmi
. buildimages.sh reset nopull
```

Still, some base image may not be built that way (FlinkBase) as mirror files changed urls, but the images used by boontadata are available in the docker hub registry.
Fixing this may not be a priority for now. It will probably involve upgrading the Flink version we use which also has implication on the Java dependencies...


