# Install Docker on the Ubuntu 16.04 VM

Instructions from Docker can be found here: <https://store.docker.com/editions/community/docker-ce-server-ubuntu>, plus <https://docs.docker.com/engine/installation/linux/linux-postinstall/#manage-docker-as-a-non-root-user>

Here are a set of instructions from that documentation. 

```bash
sudo apt-get -y install \
  apt-transport-https \
  ca-certificates \
  curl

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -

sudo add-apt-repository \
       "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
       $(lsb_release -cs) \
       stable"

sudo apt-get update

sudo apt-get -y install docker-ce
```

```bash
sudo docker run hello-world
```

```bash
sudo groupadd docker
sudo usermod -aG docker $USER
```

disconnect (^D) and reconnect

```bash
docker run hello-world
```

Also install `docker-compose`:

```bash
sudo apt install -y docker-compose
```
