package com.example.services;

import com.example.services.reactivex.ExampleDataService;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;

import java.util.concurrent.TimeUnit;

public class CallRxVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    vertx.setPeriodic(3_000, this::update);
  }

  private void update(Long aLong) { // give it a second thought to this code...
    ExampleDataService service = ExampleDataService.createProxy(vertx, "test.data-service");
    service.rxAverage()
      .delaySubscription(3, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .repeat()
      .map(data -> "avg = " + data.getDouble("average"))
      .subscribe(System.out::println);
  }
}
