# How to

before `docker-compose up -d`, the following must have been done

## to update code in the host

`git clone` or copy from your laptop where you edit files: 
```
rsync -ave ssh code u2.3-4.xyz:~/boontadata
```


## to reset cache: do the following: 

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


## to build or rebuild required images: 

```
docker build -t pyclientbase ./pyclientbase && \
docker build -t kafkaserver ./kafka-docker
```

## to start the clusters 

```
export HOSTIP=`hostname -i`
docker-compose up -d
```

## to inject and consume data

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


## clean volumes

```
docker volume ls | awk '{print $2}' | xargs docker volume rm
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
sudo vi /etc/apt/sources.list.d/docker.list
# add the following line:
# echo deb https://apt.dockerproject.org/repo ubuntu-xenial main
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

# following https://docs.docker.com/compose/install/
sudo su
curl -L https://github.com/docker/compose/releases/download/1.8.0/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
exit
sudo chmod a+x /usr/local/bin/docker-compose
```