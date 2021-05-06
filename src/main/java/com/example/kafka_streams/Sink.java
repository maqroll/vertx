package com.example.kafka_streams;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Sink extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(Sink.class);

  @Override
  public Completable rxStart() {

    final KafkaProducer<byte[], byte[]> producer = KafkaProducer.createShared(vertx, "test", new KafkaClientOptions(getConf()));

    producer.initTransactions(); // blocking

    vertx
      .eventBus()
      .<KafkaConsumerRecord<String,String>>localConsumer("channel")
      .toFlowable()
      .map(record -> record.body())
      .buffer(100, TimeUnit.MILLISECONDS, 1000)
      .subscribe(records -> {
        if (!records.isEmpty()) {
          long offset = records.get(records.size() - 1).offset();
          LOGGER.info("Start processing {}", offset);
          producer.beginTransaction();
          records.stream().forEach(record -> producer.write(KafkaProducerRecord.create("output", record.value().getBytes())));
          producer.write(KafkaProducerRecord.create("offsets", Long.valueOf(offset + 1).toString().getBytes()));
          producer.commitTransaction();
          LOGGER.info("End processing {}", offset);
        }
      });

    // TODO retries
    // TODO flow

    return Completable.complete();
  }

  private void process(KafkaConsumer<String, String> consumer) {
    consumer.poll(Duration.ofMillis(100),event -> {
      LOGGER.info("Poll");
      if (event.succeeded()) {
        KafkaConsumerRecords<String, String> records = event.result();
        for (int i=0; i<records.size(); i++) {
          KafkaConsumerRecord<String, String> record = records.recordAt(i);
          LOGGER.info("{},{}", record.partition(), record.offset());
        }
      } else {
        LOGGER.error("Failed while polling", event.cause());
      }
    });
  }

  private JsonObject getConf() {
    final JsonObject props = new JsonObject();

    props.put("config", new JsonObject()
      .put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
      "localhost:29092")
      //.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "consumerGroupTest")
      .put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      .put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      .put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
      .put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
      .put("transactional.id", "unique")
      .put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
      .put(ProducerConfig.ACKS_CONFIG, "all")
      //.put("max.poll.records", "10000")
      //.put("max.partition.fetch.bytes", "52428800")
      //.put("receive.buffer.bytes", "1048576")
      .put("enable.auto.commit", "false")
    );

    return props;
  }
}
