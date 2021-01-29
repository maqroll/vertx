package com.example.services;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class DataVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    new ServiceBinder(vertx)
      .setAddress("test.data-service")
      .register(ExampleDataService.class, ExampleDataService.create(vertx));
  }
}
