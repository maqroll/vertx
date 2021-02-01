package com.example.steps.public_api;

import io.vertx.reactivex.core.Vertx;

public class Main {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx
      .rxDeployVerticle("com.example.steps.public_api.Server")
      .subscribe(id -> {
        },
        e -> {
          e.printStackTrace();
          System.exit(-1);
        });
  }
}
