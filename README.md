[![Develop on Okteto](https://okteto.com/develop-okteto.svg)](https://cloud.okteto.com/deploy?repository=https://github.com/maqroll/vertx)

```
docker build -t registry.cloud.okteto.net/maqroll/vertx:0.1-SNAPSHOT .
docker login registry.cloud.okteto.net
docker push registry.cloud.okteto.net/maqroll/vertx:0.1-SNAPSHOT

-- tunnel echo port
kubectl port-forward service/hello-world 3000
```
