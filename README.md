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
kubectl delete pod mongodb-sharded-client

kubectl run --namespace mongodb mongodb-sharded-client --rm --tty -i --restart='Never' --image docker.io/bitnami/mongodb-sharded:6.0.4-debian-11-r10 --command -- mongosh admin --host mongodb-sharded --authenticationDatabase admin -u root -p $MONGODB_ROOT_PASSWORD
```

### To connect to your database with mongosh , run the following command:

```

kubectl port-forward --namespace mongodb svc/mongodb-sharded  27017:27017 &
    mongosh --host 127.0.0.1 --authenticationDatabase admin -p $MONGODB_ROOT_PASSWORD
```

```
    nmap -p 27017 localhost
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
  sh.shardCollection("test.employees" , { "employeeId" : "hashed"})
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

### Queries
```
    Stats of collection: 
        db.employees.stats()
    Indexes of collection: 
        db.employees.getIndexes()
    Execution stats of query with employeeId query: 
        db.employees.find({employeeId:1001}).explain("executionStats")
    Execution stats of query with employeeId $in query: 
        db.employees.find({employeeId:{ $in: [ 255, 99,1005,6321 ] }}).explain("executionStats")

    Execution stats of query with employeeId $gt,$lt query resulting in COLLSCAN stage:
        db.employees.find({employeeId:{$gt:30,$lt:250}}).explain("executionStats")

        mongotop 30 <options> <connection-string> <polling interval in seconds>
```

```
  db.createCollection("Brand")
  sh.shardCollection("test.Brand" , { "state" : "hashed", "schoolId":1})

for (var i = 1; i <= 1000; i++) {
   randomNumber = Math.round(Math.random() * 100)
   db.Brand.insertOne( { "appName": "Saral OCR App",
    "themeColor1": "#59a6a6",
    "themeColor2": "#cee4e4",
    "logoImage": "/9j/4AAQSkZJRgABAQEASABIAAD/7QA4UGhvdG9zaG9wIDMuMAA4QklNBAQAAAAAAAA4QklNBCUAAAAAABDUHYzZjwCyBOmACZjs+EJ+/+ICNElDQ19QUk9GSUxFAAEBAAACJGFwcGwEAAAAbW50clJHQiBYWVogB",
    "state": "up"+randomNumber,
    "schoolId":"09670"+randomNumber } )
}

db.Brand.getShardDistribution()

db.Brand.find({state: 'up79', schoolId:'0967079'}).explain("executionStats")
db.Brand.find({ state: { $exists: false } }).explain("executionStats")
```


### General MongoDB Monitoring commands
1.  Server status `db.serverStatus()`
2.  Server locks `db.serverStatus().locks`
3.  Server Meomory Allocation `db.serverStatus().tcmalloc`
4.  Salted Challenge Response Authentication Mechanism (SCRAM) used by default 
    `db.serverStatus().scramCache`
5.  Server Metrics `db.serverStatus().metrics`
6.  To enable/disable free monitoring `db.enableFreeMonitoring()` `db.disableFreeMonitoring()`
7.  `db.runCommand( { serverStatus: 1, mirroredReads: 1,latchAnalysis: 1 } )`
8.  Check profiling status of MongoDB `db.getProfilingStatus()`. Level 0 - off , Level 1 - profiler collects data for operations that take longer than the value of slowms , Level 2 - The profiler collects data for all operations.
9.  Set profiling status of MongoDB `db.setProfilingLevel(2)`
10. `db.system.profile` collection will have all the profile results. Use below queries to troublehsoot the hotspots.
    1. all queries that do a COLLSCAN  `db.system.profile.find({"planSummary":{$eq:"COLLSCAN"},"op" : {$eq:"query"}}).sort({millis:-1})`
    2. Find any query or command doing range or full scans `db.system.profile.find({"nreturned":{$gt:1}})`
    3. Find the source of the top ten slowest queries 
        ```Json
            db.system.profile.find({"op" : {$eq:"query"}}, {"query" : NumberInt(1),"millis": NumberInt(1)}
            ).sort({millis:-1},{$limit:10}).pretty()
        ```
    4.  Find the source of the top ten slowest aggregations 
        `db.system.profile.find({"op" : {$eq:"command"}}, {
        "command.pipeline" : NumberInt(1),
        "millis": NumberInt(1)
        }
        ).sort({millis:-1},{$limit:10}).pretty()`
    5.  Find all queries that take more than ten milliseconds, in descending order, displaying both queries and aggregations
        `db.system.profile.find({"millis":{$gt:10}}) .sort({millis:-1})`
    6.  Commenting your queries 
        ```Json
            db.Customers.find({
            "Name.Last Name" : "Johnston"
            }, {
            "_id" : NumberInt(0),
            "Full Name" : NumberInt(1)
            }).sort({
            "Name.First Name" : NumberInt(1)
            }).comment( "Find all Johnstons and display their full names alphabetically" );
        ```
    7.  