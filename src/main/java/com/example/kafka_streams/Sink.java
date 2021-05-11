package com.example.kafka_streams;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Counter;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Sink extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(Sink.class);
  private static final Random RND = new Random();
  private final AtomicReference<KafkaProducer<byte[], byte[]>> producer = new AtomicReference<>();
  private final AtomicReference<Counter> counter = new AtomicReference<>();

  @Override
  public Completable rxStart() {

    producer.set(KafkaProducer.createShared(vertx, "test", new KafkaClientOptions(getConf())));

    producer.get().initTransactions(); // blocking

    vertx.sharedData()
      .rxGetLocalCounter("pending")
      .subscribe(counter -> {
        this.counter.set(counter.getDelegate());
        vertx
          .eventBus()
          .<KafkaConsumerRecord<String, String>>localConsumer("channel")
          .toFlowable()
          .map(record -> record.body())
          .buffer(100, TimeUnit.MILLISECONDS, 1000)
          .filter(kafkaConsumerRecords -> kafkaConsumerRecords.size() > 0)
          .onBackpressureBuffer()
          .flatMapCompletable(this::processBatchRetryForever, true, 1)
          .subscribe();
      }, throwable -> {
        vertx.close();
      });

    return Completable.complete();
  }

  private Completable processBatchRetryForever(List<KafkaConsumerRecord<String, String>> records) {
    // or use a WorkerVerticle directly!!
    return vertx.rxExecuteBlocking(promise -> {
      AtomicBoolean inTx = new AtomicBoolean();

      counter.get().addAndGet(records.size() * -1);

      Flowable.just(records)
        .doOnNext(r -> processBatch(r, inTx))
        .doOnError(throwable -> {
          if (inTx.get()) {
            producer.get().abortTransaction();
          }
        })
        .retryWhen(errors -> {
          // log errors is not wrong
          AtomicInteger counter = new AtomicInteger();
          return errors
            .flatMap(e -> {
              counter.incrementAndGet();
              return Flowable.timer(1 << counter.get(), TimeUnit.MILLISECONDS);
            });
        }).blockingSubscribe();
      promise.complete();
    },true).ignoreElement();
  }

  private void processBatch(List<KafkaConsumerRecord<String, String>> records, AtomicBoolean inTx) {
    long offset = records.get(records.size() - 1).offset();
    LOGGER.info("Start processing {}", offset);
    producer.get().beginTransaction();
    inTx.set(true);
    records.stream().forEach(record -> producer.get().write(KafkaProducerRecord.create("output", record.value().getBytes())));
    producer.get().write(KafkaProducerRecord.create("offsets", "key".getBytes(), Long.valueOf(offset + 1).toString().getBytes()));
    injectFaultAtRandom();
    producer.get().commitTransaction();
    LOGGER.info("End processing {}", offset);
  }

  private void injectFaultAtRandom() {
    if (RND.nextBoolean()) {
      throw new IllegalStateException();
    }
  }

  private JsonObject getConf() {
    final JsonObject props = new JsonObject();

    props.put("config", new JsonObject()
      .put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        "kafka:9092")
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
