# Spark Stream with Kafka Messaging and Avro Serialization

This is a hack day project I did for Linkedin's Inday.  We use a lot of avro serialization and kafka for message processing, but Spark is still relatively new.  This is a project that contains:
- Avro code generation
- Kafka Event Emitter
- Spark Streaming Receiver

To run this code:

1  Download Kafka and extract Kafka: http://kafka.apache.org/downloads.html.  I used 0.8.2.1

2  Run Zookeeper
```
bin/zookeeper-server-start.sh config/zookeeper.properties
```

3  Generate the avro class
```
# Download avro-tools from http://mvnrepository.com/artifact/org.apache.avro/avro-tools/1.7.7 
java -jar ~/www/data/avro-tools-1.7.7.jar compile schema src/resources/ClickEvent.avsc src/main/java/
```

4  Run Kafka and register "test" topic
```
# Another Window
bin/kafka-server-start.sh config/server.properties
# Another Window
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
```

5  Run the Spark Stream Processor to listen to events
```
sbt 'run-main main.scala.Main test 2'  -Dsbt.parser.simple=true
```

6  Run the event emitter
```bash
sbt 'run-main main.scala.ClickEmitter localhost:9092 test'  -Dsbt.parser.simple=true
```

### Output:
```bash
...
15/08/21 15:17:50 INFO DStreamGraph: Updated checkpoint data for time 1440195470000 ms
-------------------------------------------
Time: 1440195470000 ms
-------------------------------------------
(Anthony,Food,8)
(David,Food,5)
(Michelle,Board Games,8)
(Michelle,Food,10)
(Anthony,Board Games,10)
(Anthony,Legos,5)
(Joe,Books,9)
(Joe,Food,3)
(David,Books,5)
(Anthony,Books,7)
...
```
