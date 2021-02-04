package com.example.channels;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ProducerVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(ProducerVerticle.class);
  public static final String CHANNEL_ID = "channel";
  private final Random rnd = new Random();

  @Override
  public Completable rxStart() {
    // No back-pressure
    // It generates next one after processing + interval.
    Flowable
      .interval(1, TimeUnit.MILLISECONDS, RxHelper.scheduler(vertx))
      .map(l -> rnd.nextInt(100))
      .subscribe( e -> {
        // Thread.sleep(1000);
        vertx.eventBus().publish(CHANNEL_ID, e);
        });

    return Completable.complete();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx
      .rxDeployVerticle(new ConsumerEvenVerticle())
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle(new ConsumerOddVerticle()))
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle(new ProducerVerticle()))
      .subscribe();
  }
}
