package com.example.channelsAndObjects;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(ConsumerVerticle.class);
  private int count = 0;

  @Override
  public Completable rxStart() {
    vertx.eventBus()
      .<Hit>localConsumer(ProducerVerticle.CHANNEL_ID)
      .toFlowable()
      .subscribe(hit -> {
        logger.warn("{}",hit.body());
      });

    return Completable.complete();
  }
}
