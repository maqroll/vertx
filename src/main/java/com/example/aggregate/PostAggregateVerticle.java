package com.example.aggregate;

import static com.example.aggregate.AggregateVerticle.PERIODS_IN_AGGREGATION;

import io.reactivex.Completable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.MessageProducer;
import java.time.Duration;
import java.util.Arrays;

public class PostAggregateVerticle extends AbstractVerticle {
  public static final String OUTPUT_CHANNEL = "aggregates";

  protected int PERIODS_CORRECTION = 10; // every N period send an correction
  protected int LAST_N_CORRECTIONS = 10; // last N emissions can be corrected
  // grows with time
  protected int[] aggregates = new int[LAST_N_CORRECTIONS];
  // grows from now to past
  protected int[] corrections = new int[PERIODS_IN_AGGREGATION + LAST_N_CORRECTIONS - 1];
  int[] res = new int[LAST_N_CORRECTIONS];
  protected int periodsToRecompute = PERIODS_CORRECTION;
  int idx = 0;
  MessageProducer<JsonObject> output;

  protected void recompute() {
    Arrays.parallelPrefix(corrections, (x,y) -> x+y);

    Arrays.parallelSetAll(res, i -> corrections[i+PERIODS_IN_AGGREGATION-1] +
      (i>0? corrections[i-1]*-1 : 0) +
      aggregates[(LAST_N_CORRECTIONS + idx-((1+i)%LAST_N_CORRECTIONS))%LAST_N_CORRECTIONS]
    );

    output.write(new JsonObject().put("type","correction").put("aggregates",Arrays.toString(res)));
  }

  protected void processEvent(AggregateOutput evt) {
    switch (evt.getType()) {
      case AGGREGATE:
        periodsToRecompute--;
        if (periodsToRecompute == 0) {
          recompute();
          // push recomputed results
          periodsToRecompute = PERIODS_CORRECTION;
        }

        // push aggregate
        aggregates[idx] = evt.getCount();
        idx = (idx + 1) % LAST_N_CORRECTIONS;
        output.write(new JsonObject().put("type","aggregate").put("count",evt.getCount()));

        break;
      case LATE_ARRIVAL:
        corrections[evt.getDisplacement() - 2 + periodsToRecompute]++;
        break;
    }
  }

  @Override
  public Completable rxStart() {
    output =
      vertx
        .eventBus()
        .<JsonObject>sender(
          OUTPUT_CHANNEL);

    vertx
      .eventBus()
      .<AggregateOutput>consumer(AggregateVerticle.OUTPUT_CHANNEL)
      .toFlowable()
      .subscribe(evt -> processEvent(evt.body()));

    return Completable.complete();
  }
}
