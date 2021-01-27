package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

// You can use the technique of having a verticle context and issuing calls to runOnContextwhenever
// you need to integrate non-Vert.x threading models into your applications.
public class MixedThreading extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(MixedThreading.class);

  @Override
  public void start() {
    Context context = vertx.getOrCreateContext();
    new Thread(() -> {
      try {
        run(context);
      } catch (InterruptedException e) {
        logger.error("Woops", e);
      }
    }).start();
  }

  // This example shows another important property of contexts: they are propagated when defining handlers.
  // Indeed, the block of code run withrunOnContext sets a timer handler after one second.
  // You can see that the han-dler is executed with the same context as the one that was used to define it.
  private void run(Context context) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    logger.info("I am in a non-Vert.x thread");
    context.runOnContext(v -> {
      logger.info("I am on the event-loop");
      vertx.setTimer(1000, id -> {
        logger.info("This is the final countdown");
        latch.countDown();
      });
    });

    logger.info("Waiting on the countdown latch...");
    latch.await();
    logger.info("Bye!");
  }
}
