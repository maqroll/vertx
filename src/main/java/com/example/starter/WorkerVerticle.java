package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Like event-loop verticles, worker verticles are single-threaded,
// but unlike event-loop verticles, the thread may not always be the same.
public class WorkerVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(WorkerVerticle.class);

  @Override
  public void start() {
    vertx.setPeriodic(10_000, id -> {
      try {
      logger.info("Zzz...");
      // If sleep time is bigger than period, get to sleep as soon at get awake
      Thread.sleep(15_000);
      logger.info("Up!");
    } catch (InterruptedException e) {
      logger.error("Woops", e);
    }
    });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    DeploymentOptions opts = new DeploymentOptions().setInstances(2).setWorker(true);
    vertx.deployVerticle("com.example.starter.WorkerVerticle", opts);
  }
}

