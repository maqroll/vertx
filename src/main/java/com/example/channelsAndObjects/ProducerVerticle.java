package com.example.channelsAndObjects;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.eventbus.DeliveryOptions;
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
    vertx.eventBus().registerCodec(new DummyMessageCodec());

    Flowable
      .interval(10, TimeUnit.MILLISECONDS, RxHelper.scheduler(vertx))
      .map(l -> rnd.nextInt(100))
      .subscribe( e -> {
        // Thread.sleep(1000);
        Hit hit = new Hit(23, 69);
        vertx.eventBus().send(CHANNEL_ID, hit, new DeliveryOptions().setLocalOnly(true).setCodecName("DummyMessageCodec"));
        });

    return Completable.complete();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx
      .rxDeployVerticle(new ConsumerVerticle())
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle(new ProducerVerticle()))
      .subscribe();
  }
}
