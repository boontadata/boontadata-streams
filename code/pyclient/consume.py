#initial code from https://github.com/confluentinc/confluent-kafka-python

from kafka import KafkaConsumer
import os

consumer= KafkaConsumer('sampletopic', bootstrap_servers=os.environ['KAFKA_ADVERTISED_SERVERS'])

for message in consumer:
    print(message)