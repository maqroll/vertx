echo "Waiting for Kafka to come online..."

cub kafka-ready -b kafka:9092 1 20

kafka-topics \
  --bootstrap-server kafka:9092 \
  --topic input \
  --replication-factor 1 \
  --partitions 1 \
  --config retention.ms=3600000 \
  --create

kafka-topics \
  --bootstrap-server kafka:9092 \
  --topic output \
  --replication-factor 1 \
  --partitions 1 \
  --config retention.ms=3600000 \
  --create

kafka-topics \
  --bootstrap-server kafka:9092 \
  --topic offsets \
  --replication-factor 1 \
  --partitions 1 \
  --config "cleanup.policy=compact" \
  --create

sleep infinity
