package com.example.services;

import com.example.services.reactivex.ExampleDataService;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CallRxVerticle extends AbstractVerticle {
  private AtomicLong inc = new AtomicLong();

  @Override
  public void start() throws Exception {
    vertx.setPeriodic(3_000, id -> {
      long tick = inc.getAndIncrement();
      System.out.println("------------> " + tick);
      this.update(tick);
      System.out.println("<------------ " + tick);
    });
  }

  private void update(Long aLong) { // give it a second thought to this code...
    ExampleDataService service = ExampleDataService.createProxy(vertx, "test.data-service");
    service.rxAverage()
      .delaySubscription(3, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .repeat() // while (true) { .... }
      .map(data -> "avg = " + aLong)
      .subscribe(System.out::println);
  }
}
