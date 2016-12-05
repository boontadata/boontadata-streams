#!/bin/bash

if test -z "`which docker`"
then
    #following https://docs.docker.com/engine/installation/linux/ubuntulinux/
    sudo apt-get update
    sudo apt-get -y upgrade
    sudo apt-get update
    sudo apt-get install apt-transport-https ca-certificates
    sudo apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
    echo "deb https://apt.dockerproject.org/repo ubuntu-xenial main" | sudo tee /etc/apt/sources.list.d/docker.list
    sudo apt-get update
    sudo apt-get purge lxc-docker
    apt-cache policy docker-engine
    sudo apt-get update
    sudo apt-get install -y linux-image-extra-$(uname -r) linux-image-extra-virtual
    sudo apt-get update
    sudo apt-get install -y docker-engine
    sudo service docker start
    sudo docker run hello-world
    sudo usermod -aG docker $USER

    #following https://docs.docker.com/compose/install/
    sudo curl -L "https://github.com/docker/compose/releases/download/1.9.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod a+x /usr/local/bin/docker-compose
fi
