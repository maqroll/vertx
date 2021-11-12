package com.example.promises_and_futures;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.reactivex.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Retry extends AbstractVerticle {
  private static final Random RND = new Random();
  private static final Logger LOGGER = LoggerFactory.getLogger(Retry.class);

  @Override
  public Completable rxStart() {

    Flowable.interval(5, TimeUnit.SECONDS)
      .flatMapSingle(this::process,true, 1)
      .retryWhen(errors -> errors.delay(1, TimeUnit.MILLISECONDS))
      .subscribe();

    return Completable.complete();
  }

  private Single<Long> process(Long l) {
    LOGGER.warn("Processing {}",l);
    if (RND.nextBoolean()) {
      LOGGER.error("Failing {}",l);
      throw new IllegalStateException();
    }
    return Single.just(l);
  }
}
