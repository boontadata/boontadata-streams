# notes by Benjamin Guinebertière

## rebuild a docker image

generate key pair in the u1709a.3-4.xyz VM

```
benjguin@benjguinu1709a:~/hf1709a$ ssh-keygen
Generating public/private rsa key pair.
Enter file in which to save the key (/home/benjguin/.ssh/id_rsa):
Enter passphrase (empty for no passphrase):
Enter same passphrase again:
Your identification has been saved in /home/benjguin/.ssh/id_rsa.
Your public key has been saved in /home/benjguin/.ssh/id_rsa.pub.
The key fingerprint is:
(...)
```

```
benjguin@benjguinu1709a:~/hf1709a$ cat ~/.ssh/id_rsa.pub
ssh-rsa AAAA(...)guinu1709a
```

copy paste the public key value to <https://(...).visualstudio.com/_details/security/keys>

```
benjguin@benjguinu1709a:~/hf1709a$ git clone ssh://(...)@(...).visualstudio.com:22/_git/BoontadataKubernetes
Cloning into 'BoontadataKubernetes'...
The authenticity of host '(...).visualstudio.com ((...))' can't be established.
RSA key fingerprint is SHA256:ohD8VZEXGWo6Ez8GSEJQ9WpafgLFsOfLOtGGQCQo6Og.
Are you sure you want to continue connecting (yes/no)? yes
Warning: Permanently added '(...).visualstudio.com,(...)' (RSA) to the list of known hosts.
remote:
remote:                    vSTs
remote:                  vSTSVSTSv
remote:                vSTSVSTSVST
remote: VSTS         vSTSVSTSVSTSV
remote: VSTSVS     vSTSVSTSV STSVS
remote: VSTSVSTSvsTSVSTSVS   TSVST
remote: VS  tSVSTSVSTSv      STSVS
remote: VS   tSVSTSVST       SVSTS
remote: VS tSVSTSVSTSVSts    VSTSV
remote: VSTSVST    SVSTSVSTs VSTSV
remote: VSTSv        STSVSTSVSTSVS
remote:                VSTSVSTSVST
remote:                  VSTSVSTs
remote:                    VSTs    (TM)
remote:
remote:  Microsoft (R) Visual Studio (R) Team Services
remote:
remote: Found 276 objects to send. (177 ms)
Receiving objects: 100% (276/276), 306.78 KiB | 0 bytes/s, done.
Resolving deltas: 100% (58/58), done.
Checking connectivity... done.
```

still from u1709a.3-4.xyz VM:

```
docker login
cd ~/hf1709a/BoontadataKubernetes/code
docker build -t benjguin/boontadata-kafka:0.1 ./kafka-docker/
docker push benjguin/boontadata-kafka:0.1
```

from the laptop: 
```
kubectl delete deployments,services,pods,persistentvolumeclaims --all

kubectl create -f kafka1-deployment.yaml,kafka1-service.yaml,zookeeper-deployment.yaml,zookeeper-service.yaml 

kubectl create -f zookeeper-deployment.yaml,zookeeper-service.yaml,kafka1-deployment.yaml,kafka1-service.yaml,kafka2-deployment.yaml,kafka2-service.yaml,kafka3-deployment.yaml,kafka3-service.yaml
```


## create an ACS Kubernetes cluster and deploy things to it

Start the Kubernetes cluster 

```bash
az group create --location westus2 --name a_hf1709rg2

az acs create --orchestrator-type Kubernetes -g a_hf1709rg2 -n k8nsboontadata --admin-username benjguin --agent-count 3 --agent-vm-size Standard_E2_v3 --location westus2 -d  hf1709benjguin2

az acs kubernetes get-credentials --resource-group=a_hf1709rg2 --name=k8nsboontadata

kubectl cluster-info

kubectl get nodes

cd code/kompose-files/

kubectl create -f cassandra1-deployment.yaml,cassandra1-service.yaml,cassandrainit-deployment.yaml,cassandrainit-service.yaml,flink-master-deployment.yaml,flink-master-service.yaml,flink-worker1-deployment.yaml,flink-worker1-service.yaml,flink-worker2-deployment.yaml,flink-worker2-service.yaml,kafka1-claim0-persistentvolumeclaim.yaml,kafka1-deployment.yaml,kafka1-service.yaml,kafka2-claim0-persistentvolumeclaim.yaml,kafka2-deployment.yaml,kafka2-service.yaml,kafka3-claim0-persistentvolumeclaim.yaml,kafka3-deployment.yaml,kafka3-service.yaml,pyclient-deployment.yaml,pyclient-service.yaml,zookeeper-deployment.yaml,zookeeper-service.yaml 

kubectl get pods,services,deployments

kubectl get pods
kubectl logs cassandra1-2391604453-9b93r

kubectl proxy
```

browse to <http://127.0.0.1:8001/ui>

```bash
^C

kubectl exec -ti pyclient-1920408398-ckj72 /bin/bash
kubectl delete deployments,services,persistentvolumeclaims --all
```

to remove the cluster: 

```bash
az acs delete -g a_hf1709rg2 -n k8nsboontadata
```


## reuse Carlos ACS cluster

```bash
az account set -s fe(...)80
```

which fails

```
The subscription of 'fe(...)80' does not exist or has more than one match in cloud 'AzureCloud'.
```

so the following may not work either: 

```bash
az acs kubernetes get-credentials --resource-group=boontadata --name=boontadatak8s
```

get the config file from Teams and issue a bunch of cammands: 

```bash
cd /mnt/c/afac
kubectl get clusters
export KUBECONFIG=$KUBECONFIG:/mnt/c/afac/config
kubectl config get-clusters
kubectl config
kubectl config -h
kubectl cluster-info
kubectl get-context
kubectl current-context
kubectl get current-context
kubectl config current-context
```

