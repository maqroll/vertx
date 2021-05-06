package com.example.kafka_streams;

import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka Streams is really good but sometimes
 * either its model feels a bit awkward on some pipelines
 * or you need to go more low-level.
 * This module explores the possibility of crafting
 * transactional pipelines on vertx over Kafka.
 *
 * We don't use consumer group api to prevent rebalancing.
 * We'll go for static partition assignment.
 * We don't store committed input offsets in internal topic (__consumer_offsets).
 * Would be possible to build pipelines from inputs on read-only Kafka clusters?
 */
public class KafkaProcessing extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProcessing.class);

  @Override
  public Completable rxStart() {

    vertx.eventBus().registerCodec(new LocalDummyMessageCodec());

    DeploymentOptions deploymentOptions = new DeploymentOptions();
    return vertx.rxDeployVerticle(Sink::new, deploymentOptions).ignoreElement()
      .andThen(vertx.rxDeployVerticle(Source::new, deploymentOptions).ignoreElement());
  }
}
