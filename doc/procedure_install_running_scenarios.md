## Things to know before starting
1- Technologies
	- docker
	- docker-compose
	- docker repository

2- Real Time Scenarios: 
	- Flink event_time
	- Flink process_time
	- Spark process_time



## set variables
The following values are an example, please change with your own values
```
echo 'export BOONTADATA_HOME=$HOME/boontadata-streams' >> ~/.bashrc
echo 'export BOONTADATA_DOCKER_REGISTRY=boontadata-microsoft.azurecr.io' >> ~/.bashrc
```

## sign in the $BOONTADATA_DOCKER_REGISTRY
```
docker login $BOONTADATA_DOCKER_REGISTRY 
```

## install Docker (latest version) on a host VM (Ubuntu 16.04 LTS)

ssh into the VM and execute the following statements

```
#following https://docs.docker.com/engine/installation/linux/ubuntulinux/
sudo apt-get update
sudo apt-get -y upgrade
sudo apt-get update
sudo apt-get install apt-transport-https ca-certificates
sudo apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
echo 'deb https://apt.dockerproject.org/repo ubuntu-xenial main' | sudo tee -a /etc/apt/sources.list.d/docker.list
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
sudo reboot
```
 
ssh into the VM and execute the following statements

```
docker run hello-world
```

####following https://docs.docker.com/compose/install/

```
sudo su
curl -L https://github.com/docker/compose/releases/download/1.8.0/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
exit
sudo chmod a+x /usr/local/bin/docker-compose
```


## build or rebuild required images: 

$resetoption is optional and it can be reset or noreset. The default is noreset.
You'll need first to sign in to the $BOONTADATA_DOCKER_REGISTRY 

```
docker login $BOONTADATA_DOCKER_REGISTRY
cd $BOONTADATA_HOME/code
. buildimages.sh $resetoption
```

## start the topology and run a scenario

- represents the running of flink process time

```
cd $BOONTADATA_HOME/code
. startscenario.sh flink
. runscenario.sh flink2
```

cf [sample_execution_log.md](sample_execution_log.md) for more.

## copy patches into docker running containers

```
cd $BOONTADATA_HOME/code
docker cp flink/master/code/target/flink1-0.1.jar flink-master:/workdir
```



