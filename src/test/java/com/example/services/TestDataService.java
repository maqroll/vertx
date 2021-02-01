package com.example.services;

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

@ExtendWith(VertxExtension.class)
class SensorDataServiceTest {
  private ExampleDataService dataService;

  // The Vertx Extension class takes care of async operations by waiting for VertxTestContext to report either a success or a failure.
  // To avoid having tests wait forever, there is a timeout (30 seconds by default)
  @BeforeEach
  void prepare(Vertx vertx, VertxTestContext ctx) {
    // deploy new DataVerticle()
    // wait until deployment finish
    // check that deployment went ok and create proxy
    vertx.deployVerticle(new DataVerticle(), ctx.succeeding(id -> {
      dataService = ExampleDataService.createProxy(vertx, "test.data-service");
      ctx.completeNow();
      // vertx is async
      // so returning from prepare doesn't mean in any case that prepare is finished
      // ctx.completeNow() does
    }));
  }

  @Test
  void noSensor(VertxTestContext ctx) {
    Checkpoint failsToGet = ctx.checkpoint();
    Checkpoint zeroAvg = ctx.checkpoint();

    // Checkpoints  are  flagged  to  mark  that  the  test  execution  reached  certain  lines.
    // When all declared CHECKPOINTS have been FLAGGED, the test completes successfully.
    // The test fails when an assertion fails, when an unexpected exception is thrown,
    // or when a(configurable) delay elapses and not all checkpoints have been flagged.
    dataService.get("abc", ctx.failing(err -> ctx.verify(() -> {
      assertThat(err.getMessage()).startsWith("No value has been observed");
      failsToGet.flag();
    })));
    dataService.average(ctx.succeeding(data -> ctx.verify(() -> {
      double avg = data.getDouble("average");
      assertThat(avg).isCloseTo(0.0d, withPercentage(1.0d));
      zeroAvg.flag();
    })));
  }

}
