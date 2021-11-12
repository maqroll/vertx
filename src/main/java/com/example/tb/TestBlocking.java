package com.example.tb;

import io.vertx.core.Vertx;

public class TestBlocking {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.executeBlocking(fut -> {
      vertx.setTimer(1000L, l -> {
        System.out.println("out");
        fut.complete();
      });
    });
    System.out.println("end");
  }
}
