# Camel Cassandra Component OCP Example

How to run this example on the CDK...

```
oc new-project cassandra
oc new-app --docker-image=registry.access.redhat.com/openshift3/metrics-cassandra --name=cassandra
mvn -Pf8-local-deploy
```