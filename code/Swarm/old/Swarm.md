# Swarm 

This folder contains code to create a docker Swarm cluster that can host boontadata in a multinode.

For that, it creates a number of Azure VMs with Docker 1.12 installed, then join them as a Swarm cluster.

cf <https://docs.docker.com/engine/swarm/swarm-tutorial/create-swarm/>.

## how to

execute this script from the node which will become the master

```
docker swarm init --advertise-addr `hostname -i`
```

sample execution log: 
```
benjguin@sw34h0:~$ docker swarm init --advertise-addr `hostname -i`
Swarm initialized: current node (bm640k51pl6sw8xy2qkxanujh) is now a manager.

To add a worker to this swarm, run the following command:

    docker swarm join \
    --token SWMTKN-1-1kja3k04e9qyusbm91gz89pw9bf3rxk9ob1nfy17pv5urrh2vg-4t3b2z8e67c5veosy9v5i7hr4 \
    10.10.0.4:2377

To add a manager to this swarm, run 'docker swarm join-token manager' and follow the instructions.
```

then run the command on other nodes: 
```
docker swarm join \
--token SWMTKN-1-1kja3k04e9qyusbm91gz89pw9bf3rxk9ob1nfy17pv5urrh2vg-4t3b2z8e67c5veosy9v5i7hr4 \
10.10.0.4:2377
```

then, from the manager, here is the result: 

```
benjguin@sw34h0:~$ docker node ls
ID                           HOSTNAME  STATUS  AVAILABILITY  MANAGER STATUS
bm640k51pl6sw8xy2qkxanujh *  sw34h0    Ready   Active        Leader
byfckn0bviks1cp4ifkpy1cae    sw34h2    Ready   Active
co9x25rz81bwq2sbh5ieoo44j    sw34h1    Ready   Active
```

from the master
```
git clone https://github.com/boontadata/boontadata-streams.git
export BOONTADATA_HOME=$HOME/boontadata-streams
export BOONTADATA_DOCKER_REGISTRY=acr34-microsoft.azurecr.io
docker login $BOONTADATA_DOCKER_REGISTRY
cd $BOONTADATA_HOME/code
. startscenario.sh flink overlay
```

Swarm 1.12 does not support docker compose. It will, with Swarm mode, in a future version. 

<https://github.com/Azure/azure-quickstart-templates/tree/master/docker-swarm-cluster> or Azure Container Services may be a good option

Azure ACS does not have data disks on host nodes yet => <https://github.com/Azure/azure-quickstart-templates/tree/master/docker-swarm-cluster>