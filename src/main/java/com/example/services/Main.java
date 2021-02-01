package com.example.services;

import io.vertx.core.Vertx;

public class Main {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle("com.example.services.DataVerticle")
      /*.compose(a -> {
        return vertx.deployVerticle("com.example.services.CallVerticle");
      })*/
      .compose(a -> {
        return vertx.deployVerticle("com.example.services.CallRxVerticle");
    });

  }
}
