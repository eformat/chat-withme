## Quarkus Chat with Me Server

Allows `users` to chat to `support` people.

To become a support user, `connect` using `support<anything>`. Everyone else is a user. 

### Run locally

Chat Server
```bash
mvn quarkus:dev
```

Connect to `http://localhost:8080/` using multiple browser tabs.

### Deploy to OpenShift

```bash
# create local image
mvn clean package -Pnative -Dquarkus.native.container-runtime=podman -Dquarkus.native.container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3.0.0-Final-java17
podman build . -t chat
# push image to openshift
oc new-project chat
export HOST=$(oc get route default-route -n openshift-image-registry --template='{{ .spec.host }}')
podman login -u $(oc whoami) -p $(oc whoami -t) $HOST
podman tag localhost/chat:latest ${HOST}/$(oc project -q)/chat:latest
podman push ${HOST}/$(oc project -q)/chat:latest
# run the app
oc new-app chat
oc expose svc/chat
oc patch route/chat --type=json -p '[{"op":"add", "path":"/spec/tls", "value":{"termination":"edge","insecureEdgeTerminationPolicy":"Redirect"}}]'
```
