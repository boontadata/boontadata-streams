#!/bin/bash

az account list
read -t30 -n1 -r -p 'Either Break (^C), or wait for 30 seconds or press any key to continue...' key
#az group create --location "East US" --name boontadataswarmrg
az group deployment create \
        --resource-group boontadataswarmrg \
        --template-file standard-acs-swarm-template.json \
        --name boontadataswarmdeployment
