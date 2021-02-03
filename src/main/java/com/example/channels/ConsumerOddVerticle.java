package com.example.channels;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerOddVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(ConsumerOddVerticle.class);
  private int count = 0;

  @Override
  public Completable rxStart() {
    vertx.eventBus()
      .<Integer>consumer(ProducerVerticle.CHANNEL_ID)
      .toFlowable()
      .subscribe(val -> {
        int i = val.body().intValue();

        if (i % 2 != 0) {
          count++;
          logger.info("{} odds", count);
        }
      });

    return Completable.complete();
  }
}
