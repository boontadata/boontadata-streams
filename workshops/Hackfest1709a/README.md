# Hackfest 2017-09 a - move to Kubernetes

## Introduction

you can get familiar with boontadata by reading the [PyParis2017 workshop](../PyParis2017/README.md).

## run locally

- create an Ubuntu VM (latest LTS) with a size of D11_v2_promo or E2_V3
- [install Docker](InstallDocker.md)
- set some variables [1]
- get the code from GitHub: https://github.com/boontadata/boontadata-streams, branch: hf1709a. [1]
- build
- run

[1] Environment variables and get code: 

```bash
cd $HOME
export BOONTADATA_HOME=$HOME/boontadata-streams
export BOONTADATA_DOCKER_REGISTRY=boontadata.local
git clone https://github.com/boontadata/boontadata-streams.git
cd $BOONTADATA_HOME
git checkout hf1709a
```
