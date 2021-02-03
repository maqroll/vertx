package com.example.channels;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class ConsumerEvenVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(ConsumerEvenVerticle.class);
  private int count = 0;

  @Override
  public Completable rxStart() {
    vertx.eventBus()
      .<Integer>consumer(ProducerVerticle.CHANNEL_ID)
      .toFlowable()
      .subscribe(val -> {
        int i = val.body().intValue();

        if (i%2 == 0) {
          count++;
          logger.info("{} even", count);
        }
    });

    return Completable.complete();
  }

}
