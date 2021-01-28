package com.example.reactivex;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.Vertx;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;

import java.util.concurrent.TimeUnit;

// AbstractVerticle in reactivex!!!
public class RxHttpServer extends AbstractVerticle {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new RxHttpServer());
  }

  @Override
  public Completable rxStart() {
    Observable
      .interval(1, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .subscribe(n -> System.out.println("tick"));

    return vertx
      .createHttpServer()
      .requestHandler(r -> r.response().end("Ok"))
      .rxListen(8080)
      .ignoreElement(); // single -> completable (ignore result)
  }
}
