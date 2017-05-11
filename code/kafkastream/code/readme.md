#### This is an example of kafka streaming.
##### A kafka job reads from Kafka topic, manipulates data as kstream/Ktable and writes to Cassandra.


#### Usage:

1. Inside `setup` directory, run `docker-compose up -d` to launch instances of `zookeeper`, `kafka` and `cassandra`

2. Wait for a few seconds and then run `docker-compose ps` to make sure all the three services are running.

3. Then run ` sudo pip install -r requirements.txt`

4. Run the spark-app using `java -cp boontadata-streams-1.0-SNAPSHOT-jar-with-dependencies.jar com.aggregateKafkaStream.Main` in a console. This app will listen on topic (check Main.java) and writes it to Cassandra.
   * After have done the command `maven clean package` inside the directory contening the pom.xml file, the jar is created at ${project_directory}/target

5. Run `python ingest.py` generates some random iot data and publishes it to a topic in kafka.

6. To check if the data has been published in cassandra.
  * Go to cqlsh `docker exec -it cas_01_test cqlsh `
  * And then run `select * from boontadata.agg_events  ;`

7. Finally, to compare to result of spark streaming vs iot in cassandra
  * Run the command `python compare.py`

Credits:

* This project on kafka stream is independent from the boontadata architecture, in order to make it works in such environment it's necessary to create a kstream scenario
