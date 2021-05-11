package com.example.kafka_streams;

import io.reactivex.Completable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Counter;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.MessageProducer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class Source extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(Source.class);

  @Override
  public Completable rxStart() {

    final KafkaConsumer<String, String> consumer = KafkaConsumer
      .create(vertx, new KafkaClientOptions(getConf()));

    MessageProducer<KafkaConsumerRecord<String,String>> toSink =
      vertx
        .eventBus()
        .<KafkaConsumerRecord<String,String>>sender(
          "channel",
          new DeliveryOptions().setLocalOnly(true).setCodecName(LocalDummyMessageCodec.NAME));

    vertx.sharedData().getDelegate().getLocalCounter("pending").onComplete(asyncCounter -> {
      if (asyncCounter.succeeded()) {
        Counter counter = asyncCounter.result();

        // Cheap flow control
        vertx.periodicStream(5).toFlowable().subscribe(l -> {
          counter.get().onSuccess(res -> {
            if (res > 1000) {
              LOGGER.warn("Pausing");
              consumer.pause();
            } else {
              consumer.resume();
            }
          });
        });

        consumer
          .assign(new TopicPartition("input", 0)) // static assignment
          .toFlowable()
          .subscribe(record -> {
            LOGGER.info("{},{}", record.partition(), record.offset());

            toSink.write(record);
            counter.incrementAndGet();
          });
      } else {
        vertx.close();
      }
    });

    return Completable.complete();
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
      //.put("max.poll.records", "10000")
      //.put("max.partition.fetch.bytes", "52428800")
      //.put("receive.buffer.bytes", "1048576")
      .put("enable.auto.commit", "false")
    );

    return props;
  }
}
