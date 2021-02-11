private.pem:
	openssl genrsa -out private.pem 2048

private_key.pem: private.pem
	openssl pkcs8 -topk8 -inform PEM -in private.pem -out private_key.pem -nocrypt

public_key.pem: private.pem
	openssl rsa -in private.pem -outform PEM -pubout -out public_key.pem

keys: public_key.pem private_key.pem

clean:
	rm -f private.pem private_key.pem public_key.pem

offsets:
	docker exec -it clickhouse_avro_kafka_1 kafka-console-consumer --bootstrap-server localhost:9092 --topic __consumer_offsets --formatter "kafka.coordinator.group.GroupMetadataManager\$$OffsetsMessageFormatter" --from-beginning

reset:
	docker exec -it clickhouse_avro_kafka_1 kafka-consumer-groups --bootstrap-server localhost:9092 --group tb-ingestion --topic data --reset-offsets --to-earliest --execute
