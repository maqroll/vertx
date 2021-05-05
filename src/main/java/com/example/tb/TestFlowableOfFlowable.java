package com.example.tb;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TestFlowableOfFlowable extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(TestFlowableOfFlowable.class);

  WebClient webClient;
  ConfigRetriever retriever;

  @Override
  public Completable rxStart() {
    Flowable
      .range(1,1000)
      .groupBy( e -> (int) e/3)
      .subscribe(
        g -> {
          g.subscribe(i -> {
            System.out.println(g.getKey() + "," + i);
          });
        }
      ) ;

    Flowable
      .range(1,1000)
      .buffer(1, TimeUnit.SECONDS, 10)
      .subscribe(
        e -> {
          System.out.println("->" + e);
          Flowable
            .fromIterable(e)
            .subscribe(i ->
            {
              //Ignore
            });

        }
      );

    return Completable.complete();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx
          .rxDeployVerticle(new TestFlowableOfFlowable())
      .ignoreElement()
      .subscribe();
  }
}
