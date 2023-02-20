# try-mongodb-sharded
try-mongodb-sharded using bitnami/mongodb-sharded

References: https://github.com/bitnami/charts/tree/main/bitnami/mongodb-sharded

## Steps to deploy sharded mongodb on k8s using bitnami/mongodb-sharded helm chart with default values.

```
    kubectl create namespace mongodb
    kubectl config set-context --current --namespace=mongodb
```

```
    helm repo add my-repo https://charts.bitnami.com/bitnami
    helm install mongodb-sharded bitnami/mongodb-sharded
```

### Install sharded mongodb using helm chart with custom values.

```
helm install mongodb-sharded \
  --set shards=4,configsvr.replicaCount=3,shardsvr.dataNode.replicaCount=2 \
    my-repo/mongodb-sharded
```

### set mongodb root password
```
export MONGODB_ROOT_PASSWORD=$(kubectl get secret --namespace mongodb mongodb-sharded -o jsonpath="{.data.mongodb-root-password}" | base64 -d)
```

### To connect to your database with client , run the following command:

```
kubectl run --namespace mongodb mongodb-sharded-client --rm --tty -i --restart='Never' --image docker.io/bitnami/mongodb-sharded:6.0.4-debian-11-r10 --command -- mongosh admin --host mongodb-sharded --authenticationDatabase admin -u root -p $MONGODB_ROOT_PASSWORD
```

### To connect to your database with mongosh , run the following command:

```

kubectl port-forward --namespace mongodb svc/mongodb-sharded  27017:27017 &
    mongosh --host 127.0.0.1 --authenticationDatabase admin -p $MONGODB_ROOT_PASSWORD
```

### List shards

```
    use admin
    db.runCommand(
        {
            listShards: 1
        }
    )
```

### 


### Create database and enable Sharding
```
    use test
    sh.enableSharding("test")
```

### Create collection and Shard
```
  db.createCollection("employees")
  sh.shardCollection("test.employees" , { "employeeId" : 1 , "employeeName" : 1})
```

### Add Test Data and check shard distribution

```
for (var i = 1; i <= 10000; i++) {
   db.employees.insertOne( { employeeId : i, employeeName:"Test "+i } )
}
```

```
    db.employees.getShardDistribution()
```