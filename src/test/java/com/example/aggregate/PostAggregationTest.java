package com.example.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class PostAggregationTest {
  @Test
  public void aggregates(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    vertx.eventBus().registerCodec(new LocalDummyMessageCodec());

    PostAggregateVerticle aggVerticle = new PostAggregateVerticle();

    AtomicInteger messages = new AtomicInteger();
    final int expectedMessages = aggVerticle.LAST_N_CORRECTIONS + aggVerticle.LAST_N_CORRECTIONS/ aggVerticle.PERIODS_CORRECTION;

    vertx.eventBus().<JsonObject>localConsumer(PostAggregateVerticle.OUTPUT_CHANNEL).handler(event -> {
      messages.getAndIncrement();

      if (messages.get() == expectedMessages) {
        testContext.completeNow();
      }
    });

    vertx.deployVerticle(aggVerticle, testContext.succeeding(idAgg -> {
      AggregateOutput agg = new AggregateOutput();
      agg.setType(Type.AGGREGATE);

      for (int i=0; i< aggVerticle.LAST_N_CORRECTIONS; i++) {
        agg.setCount(i);
        aggVerticle.processEvent(agg);
      }

      for (int i=0; i< aggVerticle.LAST_N_CORRECTIONS; i++) {
        assertEquals(i,aggVerticle.aggregates[i]);
      }
    }));

    testContext.awaitCompletion(10, TimeUnit.SECONDS);
  }

  @Test
  public void aggregatesWithLateArrivals(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    vertx.eventBus().registerCodec(new LocalDummyMessageCodec());

    PostAggregateVerticle aggVerticle = new PostAggregateVerticle();

    AtomicInteger messages = new AtomicInteger();
    final int expectedMessages = 11;

    vertx.eventBus().<JsonObject>localConsumer(PostAggregateVerticle.OUTPUT_CHANNEL).handler(event -> {
      messages.getAndIncrement();

      if ("correction".equals(event.body().getString("type"))) {
          assertThat(event.body().getString("aggregates").equals("[2, 1, 1, 1, 1, 1, 1, 1, 1, 0]"));
      }

      if (messages.get() == expectedMessages) {
        testContext.completeNow();
      }
    });

    vertx.deployVerticle(aggVerticle, testContext.succeeding(idAgg -> {
      AggregateOutput agg = new AggregateOutput();
      agg.setType(Type.AGGREGATE);
      agg.setCount(1);

      aggVerticle.processEvent(agg);
      aggVerticle.processEvent(agg);
      aggVerticle.processEvent(agg);
      aggVerticle.processEvent(agg);
      aggVerticle.processEvent(agg);
      aggVerticle.processEvent(agg);
      aggVerticle.processEvent(agg);
      aggVerticle.processEvent(agg);
      aggVerticle.processEvent(agg);

      AggregateOutput correction = new AggregateOutput();
      correction.setType(Type.LATE_ARRIVAL);
      correction.setDisplacement(1);
      aggVerticle.processEvent(correction);

      aggVerticle.processEvent(agg);

      for (int i=0; i < 10; i++) {
        assertEquals(1,aggVerticle.aggregates[i]);
      }
    }));

    testContext.awaitCompletion(10, TimeUnit.SECONDS);
  }
}
