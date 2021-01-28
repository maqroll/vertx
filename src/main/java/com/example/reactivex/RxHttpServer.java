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
    // RxHelper.scheduler ensures that timer events get triggered from appropiate thread
    // If you omit scheduler the subscription got executed on some RxComputationThreadPool
    Observable
      .interval(1, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .subscribe(n -> System.out.println("tick @" + Thread.currentThread().getName()));

    return vertx
      .createHttpServer()
      .requestHandler(r -> r.response().end("Ok"))
      .rxListen(8080)
      .ignoreElement(); // single -> completable (ignore result)
  }
}
