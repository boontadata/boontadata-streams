# PyParis 2017

Title: Program in Python against big data clusters from one VM, thanks to Docker

Abstract: You want to develop in Python against big data clusters but you don’t want to run a whole infrastructure? During this workshop, we’ll run boontadata samples starting from a blank Virtual Machine, with clusters like Apache Kafka, Cassandra, Spark or Flink for instance. 

Date and duration: 13-JUN-2017, 90 minutes

This folder contains the material prepared for this workshop. 

## Prerequisites

You can follow the tutorial by just watching or you can do it with the speaker. 
If you choose the second option, you need to be prepared. Here are the prerequistes.

We'll work on an Ubuntu machine that has ~12GB of RAM. 

You'll also need to bring your laptop with an HTML 5 browser and an ssh client. 


## Agenda

Duration (min) | What | Comments
--------------:|------|----------
10 | Introduction |
20 | Prepare the VM and install Docker | includes Azure subscription creation if needed
20 | Create my first docker-compose infrastructure | 
20 | Get and run boontadata | get sources form git, run 2 different topologies, docker exec into nodes, ssh tunnel to have the UI fo some frameworks like Spark
15 | Additional Python coding | Take some sample code and run it against Spark cluster
5 | Conclusion

## Introduction

## Prepare the VM and install Docker

Explanations will be done on an Azure VM running Ubuntu LTS 16.04. 

Equivalent steps could also be followed on other clouds. You may also run the same on your laptop if it has some RAM and supports Docker. 

If you don't have an Azure subscription a code can be provided to you. You can follow the following documentation: [
How to create an Azure PASS subscription and an organizational account at the same time](https://github.com/DXFrance/data-hackathon/blob/master/doc/AzurePASSorg.md)

Then, [create an Azure VM](createAzureVM.md). The one we create here has 14 GB of RAM, and 2 cores. It's running Ubuntu LTS XXX?

[Install docker in the VM](installDocker.md). 

## Create my first docker-compose infrastructure

In order to familiarize ourselves with the basics of Docker, we'll create a small 3-node infrastructure and play with it.

In the VM, copy the folder that you have in this folder: `simpleinfra`.

Inspect files.

build a Docker image from the current folder (the one where you have the `Dockerfile` file):

```bash
docker build -t pyp17image1 .
```

build a second image based on the same sources
```bash
docker build -t pyp17image2 .
```

this second build took no time because it leveraged cache from the first build. See, the ids are the same for both images:
```bash
docker images
```

start the infrastructure, and list the running nodes

```bash
docker-compose up -d
docker-compose ps
```

connect to node 1: 
```bash
docker exec -ti n1 /bin/sh
```

from node 1 issue a few commands:
```bash
ls -als
./hw.sh
ping -c 4 n2
ping -c 4 n3
```

Then disconnect from node 1 (^D), and shutdown the infrastructure

```bash
docker-compose down
```

## Get and run boontadata

From the VM

```bash
cd ~/
git clone https://github.com/boontadata/boontadata-streams.git
export BOONTADATA_HOME=$HOME/boontadata-streams
export BOONTADATA_DOCKER_REGISTRY=boontadata.local
cd $BOONTADATA_HOME
git checkout pyparis2017
cd code
```

```bash
. buildimages.sh noreset pullfromdockerhub
```

Pull from an Azure VM takes about 5 to 7 minutes

while it's pulling images and starting you can inspect the following files in the `code` directory: 
- pyclientbase/Dockerfile
- pyclient\Dockerfile
- pyclient\ingest.py
- pyclient\compare.py

```bash
. startscenario.sh flink
docker-compose ps
```

open a tunnel 
```bash
ssh -D 127.0.0.1:8034 $pyp17vmname.westeurope.cloudapp.azure.com
```

and browse to <http://http://0.0.0.0:34010/>

```bash
. runscenario.sh flink2
```

```bash
docker-compose down
```

```bash
. startscenario.sh spark
docker-compose ps
```

To be continued

## Additional Python coding

## Conclusion
