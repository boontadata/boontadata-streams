# branches

This document explains what different branches were created for.
Branch `master` is the first one, then the newest branches are at the top.

## master

This branch always has the latest stable and global version

## swarm1

Create a Swarm cluster from VMs in Azure with attached data disks so that scenarios like MapR are possible.


## flink2

Prepare demos around the Flink scenario. 
Have 2 different Flink scenarios: Processing time, and event time.

## multi1

Adds the following features to boontadata: 
- run on one node (dev) or several nodes
- leverages a docker registry
- docker-compose.yml is created depending on the scenarios that must be run
- adds a multi node support for Spark

## flink1

Adds a scenario where Apache Flink consumes data using event time.

