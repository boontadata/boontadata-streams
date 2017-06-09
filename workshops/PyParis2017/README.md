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

## Create my first docker-compose infrastructure

## Get and run boontadata

## Additional Python coding

## Conclusion
