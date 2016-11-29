# branches

This document explains what different branches were created for

## master

This branch always has the latest stable and global version

## flink1

Adds a scenario where Apache Flink consumes data using event time.

## multi1

Adds the following features to boontadata: 
- run on one node (dev) or several nodes
- leverages a docker registry
- docker-compose.yml is created depending on the scenarios that must be run
- adds a multi node support for Spark