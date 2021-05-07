package com.example.kafka_streams;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.KafkaClientOptions;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Sink extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(Sink.class);
  private static final Random RND = new Random();
  private final AtomicReference<KafkaProducer<byte[],byte[]>> producer = new AtomicReference<>();

  @Override
  public Completable rxStart() {

    producer.set(KafkaProducer.createShared(vertx, "test", new KafkaClientOptions(getConf())));

    producer.get().initTransactions(); // blocking

    flow(producer.get())
      .subscribe();

    return Completable.complete();
  }

  private Completable flow(KafkaProducer<byte[], byte[]> producer) {
    return vertx
      .eventBus()
      .<KafkaConsumerRecord<String,String>>localConsumer("channel")
      .toFlowable()
      .map(record -> record.body())
      .buffer(100, TimeUnit.MILLISECONDS, 1000)
      .flatMapCompletable(this::processBatchForever, true, 1);
  }

  private Completable processBatchForever(List<KafkaConsumerRecord<String,String>> records) {
    long errors=0;

    do {
      try {
        return processBatch(records);
      } catch (Throwable t) {
        errors++;
        LOGGER.info("Waiting for {} ms", 10L<<errors);
        vertx.timerStream(10L << errors).fetch(1);
      }
    } while(true);
  }

  private Completable processBatch(List<KafkaConsumerRecord<String,String>> records) {
    if (!records.isEmpty()) {
      long offset = records.get(records.size() - 1).offset();
      LOGGER.info("Start processing {}", offset);
      producer.get().beginTransaction();
      records.stream().forEach(record -> producer.get().write(KafkaProducerRecord.create("output", record.value().getBytes())));
      producer.get().write(KafkaProducerRecord.create("offsets", Long.valueOf(offset + 1).toString().getBytes()));
      injectFaultAtRandom();
      producer.get().commitTransaction();
      LOGGER.info("End processing {}", offset);
    }

    return Completable.complete();
  }

  private void injectFaultAtRandom() {
    if (RND.nextBoolean()) {
      throw new IllegalStateException();
    }
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
