package com.example.services;

import io.vertx.core.AbstractVerticle;

public class CallVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    vertx.setPeriodic(3_000, this::update);
  }

  private void update(Long aLong) {
    ExampleDataService service = ExampleDataService.createProxy(vertx, "test.data-service");
    service.average(ar -> {
      if (ar.succeeded()) {
        System.out.println("Average = " + ar.result());
      } else {
        ar.cause().printStackTrace();
      }
    });
  }
}
