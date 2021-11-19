## Quarkus Chat with Me Server

![images/chat.gif](images/chat.gif)

Allows `users` to chat to `support` people.

To become a support user, `connect` using `support<anything>`. Everyone else is a user. Try with multiple browser tabs. For now, only 1 `support` person is allocated per `user`. 

Stores messages in Kafka that are streamed to Materialized database views in real-time. Could also use Trino, Flink here.

### Run locally

Kafka, Materialize
```bash
podman-compose up -d
```

Chat Server
```bash
mvn quarkus:dev
```

Connect to `http://localhost:8080/` using multiple browser tabs.

### Kappa Architecture for data

Uses Kafka as the message store. Materialize as the streaming realtime database.

Raw chat data
```bash
kafkacat -b localhost:9092 -t chats -o beginning -C -f '\nKey (%K bytes): %k
  Value (%S bytes): %s
  Timestamp: %T
  Partition: %p
  Offset: %o
  Headers: %h'
```

Realtime Materialized schema
```bash
psql -h localhost -p 6875 -U materialize materialize -f ./load.sql
psql -h localhost -p 6875 -U materialize materialize -f ./drop.sql
```

Database queries
```bash
psql -h localhost -p 6875 -U materialize materialize -c "select * from CHAT_ALL;"
psql -h localhost -p 6875 -U materialize materialize -c "select * from CHAT_TOTALS;"
psql -h localhost -p 6875 -U materialize materialize -c "select * from CHAT_ALL where key in ('bob-support') order by timestamp desc;"
```

e.g.
```bash
watch -c "psql -h localhost -p 6875 -U materialize materialize -c 'select * from CHAT_ALL order by timestamp desc;'"

     key     |         timestamp          | username | supportname |        	                 message
-------------+----------------------------+----------+-------------+-----------------------------------------------------------------
 bob-support | 2021-11-19 06:24:36.317+00 | bob      | support     | User bob left
 bob-support | 2021-11-19 06:24:34.595+00 | bob      | support     | >> bob: ah, right ! that worked thanks. see ya !
 bob-support | 2021-11-19 06:24:21.352+00 | bob      | support     | >> support: well, its easy .. you just goto here http://help-me
 bob-support | 2021-11-19 06:24:06.389+00 | bob      | support     | >> bob: err .. how do i do that ?
 bob-support | 2021-11-19 06:23:58.809+00 | bob      | support     | >> support: oh, thats sounds bad .. can you check online ?
 bob-support | 2021-11-19 06:23:36.52+00  | bob      | support     | >> bob: i have lost my app?
 bob-support | 2021-11-19 06:23:18.044+00 | bob      | support     | >> support: hi bob, how can i help ?
 bob-support | 2021-11-19 06:23:07.32+00  | bob      | support     | >> bob: hi
(8 rows)
```

### Deploy to OpenShift

This creates a demo in the `chat` namespace.

Deploy kafka operator as cluster-admin
```bash
oc apply -k openshift/kafka/base
```

Deploy kafka CR
```bash
oc apply -k openshift/kafka/dev
```

Deploy materialize
```bash
oc apply -k openshift/materialize/dev
```

Deploy chat
```bash
oc apply -k openshift/chat/dev
```

### Debug Tools for OpenShift

For debug purposes - kafka tools
```bash
oc -n quarkus-saga run tools --image=debezium/tooling --command -- bash -c 'sleep infinity'
oc -n quarkus-saga rsh tools
kafkacat -b chat-cluster-kafka-bootstrap:9092 -L
```

Watch processed topic
```bash
kafkacat -b chat-cluster-kafka-bootstrap:9092 -t chats -o beginning -C -f '\nKey (%K bytes): %k
  Value (%S bytes): %s
  Timestamp: %T
  Partition: %p
  Offset: %o
  Headers: %h'
```

Port forward materialize port
```bash
oc port-forward svc/materialize 6875:6875 &
psql -h localhost -p 6875 -U materialize materialize -c 'select * from CHAT_ALL order by timestamp desc;'
```


