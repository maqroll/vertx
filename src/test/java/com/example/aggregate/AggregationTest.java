package com.example.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class AggregationTest {
  @Test
  public void test(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    vertx.eventBus().registerCodec(new LocalDummyMessageCodec());

    AggregateVerticle aggVerticle = new AggregateVerticle();

    DeploymentOptions options = new DeploymentOptions().setConfig(
      new JsonObject().put("test",true)
    );

    vertx.deployVerticle(aggVerticle, options, testContext.succeeding(idAgg -> {
      Event evt = new Event();
      evt.setTs(aggVerticle.getTick() + 1);

      vertx.eventBus().<AggregateOutput>localConsumer(AggregateVerticle.OUTPUT_CHANNEL).handler(event -> {
        assertEquals(Type.AGGREGATE,event.body().getType());
        assertEquals(1,event.body().getCount());
        testContext.completeNow();
      });

      aggVerticle.processEvent(evt);

      aggVerticle.emitAggregate();
      aggVerticle.advanceTick();

    }));

    testContext.awaitCompletion(10, TimeUnit.SECONDS);
  }
}
