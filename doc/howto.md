# How to

## set variables

this could be added to your ~/.bashrc file 

The following values are an example, please change with your own values

```
export BOONTADATA_HOME=$HOME/boontadata-streams
export BOONTADATA_DOCKER_REGISTRY=acr34-microsoft.azurecr.io
```

## build or rebuild required images: 

$resetoption is optional and it can be reset or noreset. The default is noreset.

```
docker login $BOONTADATA_DOCKER_REGISTRY
cd $BOONTADATA_HOME/code
. buildimages.sh $resetoption
```

## start the topology and run a scenario

```
cd $BOONTADATA_HOME/code
. startscenario.sh flink
. runscenario.sh flink
```

cf [sample_execution_log.md](sample_execution_log.md) for more.

## copy patches into docker running containers

```
cd $BOONTADATA_HOME/code
docker cp flink/master/code/target/flink1-0.1.jar flink-master:/workdir
```


## Appendix

### install Docker (latest version) on a host VM (Ubuntu 16.04 LTS)

ssh into the VM and execute the following statements

```
#following https://docs.docker.com/engine/installation/linux/ubuntulinux/
sudo apt-get update
sudo apt-get -y upgrade
sudo apt-get update
sudo apt-get install apt-transport-https ca-certificates
sudo apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
sudo vi /etc/apt/sources.list.d/docker.list
#add the following line:
#echo deb https://apt.dockerproject.org/repo ubuntu-xenial main
sudo apt-get update
sudo apt-get purge lxc-docker
apt-cache policy docker-engine
sudo apt-get update
sudo apt-get install linux-image-extra-$(uname -r) linux-image-extra-virtual
sudo apt-get update
sudo apt-get -y install docker-engine
sudo service docker start
sudo docker run hello-world
sudo usermod -aG docker $USER
```

disconnect and reconnect 

```
docker run hello-world

#following https://docs.docker.com/compose/install/
sudo su
curl -L https://github.com/docker/compose/releases/download/1.8.0/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
exit
sudo chmod a+x /usr/local/bin/docker-compose
```

### develop in Java or Scala without an IDE: use the devjvm container

Here is an example where we use devjvm to develop code for the flinkmaster container:
```
devjvmimage="$BOONTADATA_DOCKER_REGISTRY/boontadata/devjvm:0.1"
docker pull $devjvmimage 
docker run --name devjvm -d \
    -v $BOONTADATA_HOME/dockervolumesforcache/maven-m2:/root/.m2 \
    -v $BOONTADATA_HOME/code/flink/master/code:/usr/src/dev \
    -w /usr/src/dev $devjvmimage
docker exec -ti devjvm /bin/bash

docker rm -f devjvm
```

Maven to generate a Flink Skeleton: 

```
PACKAGE=quickstart

mvn archetype:generate                                                          \
  -DarchetypeGroupId=org.apache.flink                           \
  -DarchetypeArtifactId=flink-quickstart-java           \
  -DarchetypeVersion=1.1.3                                                      \
  -DgroupId=xyz.nullepart.quickstart                                        \
  -DartifactId=$PACKAGE                                                         \
  -Dversion=0.1                                                                         \
  -Dpackage=xyz.nullepart.quickstart                                        \
  -DinteractiveMode=false
```

Maven to build:

```
cd quickstart
mvn clean install -Pbuild-jar 
```

generate jar on provided pom.xml

```
mvn clean package
```

### start the clusters 

$scenario can be flink, spark, anything that has a corresponding .yml file in the `compose-blocks` folder ...

```
cd $BOONTADATA_HOME/code
. startscenarios.sh $scenario
```

On Azure Container Services with Swarm. 
Ssh to the main node. Then:
```
export DOCKER_HOST=:2375

```


### inject and consume data

try for one device only:

```
docker exec -ti client1 python /workdir/ingest.py
```

once it works, try with 10 devices for instance:
```
docker exec -ti client1 bash /workdir/ingestfromdevices.sh 10
```

in another terminal, consume from Spark:
```
docker exec -ti spark1 /workdir/start-consume.sh
```

### run a Flink job

from container host 

```
cd $BOONTADATA_HOME/code
docker cp flink/master/code/target/flink1-0.1.jar flink-master:/workdir
```

from flink-master container

```
flink run -c io.boontadata.flink1.StreamingJob /workdir/flink1-0.1.jar --topic sampleTopic --bootstrap.servers ks1:9092,ks2:9092,ks3:9092 --zookeeper.connect zk1:2181 --group.id myGroup 
```

## connect to a few dashboards

Once you've started the containers, and establised an ssh tunnel with this kind of command:

```
ssh -D 127.0.0.1:8034 mycontainerhostvm.on.azure.tld
```

and set a proxy to `127.0.0.1:8034` or the address you chose, then you can connect to the following

role | url
:----|:----
Apache Flink Web Dashboard | http://0.0.0.0:34010/#/overview
Apache Spark Web Dashboard | http://0.0.0.0:34110

## clean volumes

```
docker volume ls | awk '{print $2}' | xargs docker volume rm
```

### update code in the host

`git clone` or copy from your laptop where you edit files: 
```
rsync -ave ssh code u2.3-4.xyz:~/boontadata-streams
```

### summary of the most usefull commands while developing with Flink

```
rsync -ave ssh u2.3-4.xyz:~/sdc1/boontadata-streams/code/flink/master/code /mnt/c/afac/code
rsync -ave ssh /mnt/c/dev/_git/GitHub/boontadata/boontadata-streams/code/flink/master/code/src u2.3-4.xyz:~/sdc1/boontadata-streams/code/flink/master/code
scp /mnt/c/dev/_git/GitHub/boontadata/boontadata-streams/code/flink/master/code/pom.xml u2.3-4.xyz:~/sdc1/boontadata-streams/code/flink/master/code
scp /mnt/c/dev/_git/GitHub/boontadata/boontadata-streams/code/pyclient/ingest.py u2.3-4.xyz:~/sdc1/boontadata-streams/code/pyclient
scp /mnt/c/dev/_git/GitHub/boontadata/boontadata-streams/code/pyclient/compare.py u2.3-4.xyz:~/sdc1/boontadata-streams/code/pyclient

docker run --name devscala -d -v $BOONTADATA_HOME/code/flink/master/code:/usr/src/dev -w /usr/src/dev devscala 
docker exec -ti devscala /bin/bash
mvn clean package
vi src/main/java/io/boontadata/flink1/DevJob.java

cd $BOONTADATA_HOME/code
docker cp flink/master/code/target/flink1-0.1.jar flink-master:/workdir
docker cp pyclient/compare.py client1:/workdir
docker exec -ti client1 python /workdir/ingest.py

docker exec -ti flink-master /bin/bash
flink run -c io.boontadata.flink1.StreamingJob /workdir/flink1-0.1.jar -d
flink list
flink cancel xxx

docker exec -ti cassandra2 cqlsh
use boontadata;
select count(*) from debug;
select * from debug limit 100;
truncate table debug;
truncate table agg_events;
truncate table raw_events;
select window_time, device_id, category, m1_sum_ingest_devicetime, m1_sum_ingest_sendtime, m1_sum_flink_eventtime from agg_events limit 100;
select window_time, device_id, category, m2_sum_ingest_devicetime, m2_sum_ingest_sendtime, m2_sum_flink_eventtime from agg_events limit 100;

select window_time, device_id, category, 
  m2_sum_ingest_devicetime, m2_sum_ingest_sendtime, m2_sum_flink_eventtime 
from agg_events 
where category='cat-1'
order by category, device_id, window_time
limit 100;


ssh -D 127.0.0.1:8034 u2.3-4.xyz
http://0.0.0.0:34010/#/overview
```

### reset cache: do the following: 

```
docker images | grep "code_" | awk '{print $1}' | xargs --no-run-if-empty docker rmi
docker rmi kafkaserver
docker rmi pyclientbase
```

or 

```
docker images | awk '{print $1}' | xargs --no-run-if-empty docker rmi
docker images | awk '{print $3}' | xargs --no-run-if-empty docker rmi
```

### sample run on Azure Container Services

```
ssh acs34mgmt.eastus.cloudapp.azure.com
git clone https://github.com/boontadata/boontadata-streams.git
git pull origin multi1
git checkout multi1
cd boontadata-streams/
export BOONTADATA_HOME=`pwd`
export BOONTADATA_DOCKER_REGISTRY=acr34-microsoft.azurecr.io
docker login $BOONTADATA_DOCKER_REGISTRY
export DOCKER_HOST=172.16.0.5:2375
cd code
. startscenario.sh flink overlay
```

