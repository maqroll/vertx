package com.example.heat;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

public class Main {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle("com.example.heat.HeatSensor", new DeploymentOptions().setInstances(4));
    vertx.deployVerticle("com.example.heat.Listener");
    vertx.deployVerticle("com.example.heat.SensorData");
    vertx.deployVerticle("com.example.heat.HttpServer");
  }
}
