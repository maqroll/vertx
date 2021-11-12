package com.example.aggregate;

import io.reactivex.Completable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.MessageProducer;
import java.time.Duration;

public class AggregateVerticle extends AbstractVerticle {
  public static final int PERIODS_IN_AGGREGATION = 2; // 5 minutes
  public static final String INPUT_CHANNEL = "input";
  public static final String OUTPUT_CHANNEL = "output";

  Duration PERIOD = Duration.ofSeconds(1L);
  long PERIOD_MILLIS = PERIOD.toMillis();
  int PERIODS_LAG = 0;
  int TOTAL_BUCKETS = PERIODS_IN_AGGREGATION + PERIODS_LAG;
  int[] buckets = new int[TOTAL_BUCKETS];
  long tick;
  int currBucket = -1;
  long max,min;
  int currAggregate = 0;
  MessageProducer<AggregateOutput> output;

  protected long getTick() {
    return tick;
  }

  private void emitLate(int displacement) {
    AggregateOutput agg = new AggregateOutput();
    agg.setType(Type.LATE_ARRIVAL);
    agg.setDisplacement(displacement);

    output.write(agg);
  }

  protected void emitAggregate() {
    AggregateOutput agg = new AggregateOutput();
    agg.setType(Type.AGGREGATE);
    agg.setCount(currAggregate);
    agg.setTs(tick + PERIOD_MILLIS);

    output.write(agg);
  }

  protected void advanceTick() {
    tick = System.currentTimeMillis();
    // [ tick, tick + PERIOD_MILLIS )
    max = tick + PERIOD_MILLIS;
    min = tick + PERIOD_MILLIS - (PERIOD_MILLIS * PERIODS_IN_AGGREGATION);

    currBucket = (currBucket + 1) % TOTAL_BUCKETS;
    currAggregate -= buckets[currBucket];
    buckets[currBucket] = 0;
  }

  protected void processEvent(Event evt) {
    final long ts = evt.getTs();
    if (ts < min) {
      // too late -> forward
      // TODO we need to take into account more data
    } else if (ts >= max) {
      buckets[currBucket]++;
    } else {
      final long d = max - ts;
      final int displacement = (int) (d / PERIOD_MILLIS);
      buckets[(PERIODS_IN_AGGREGATION + currBucket - displacement)%PERIODS_IN_AGGREGATION]++;
      currAggregate++;

      if (displacement > 0) {
        emitLate(displacement);
      }
    }
  }

  @Override
  public Completable rxStart() {
    // Discard the drift for the time being.
    // In our experience DRIFT is MINIMAL.
    if (!config().getBoolean("test",false)) {
      vertx.setPeriodic(PERIOD.toMillis(), id -> {
        emitAggregate();
        advanceTick();
      });
    }

    output =
      vertx
        .eventBus()
        .<AggregateOutput>sender(
          OUTPUT_CHANNEL).deliveryOptions(new DeliveryOptions().setCodecName(LocalDummyMessageCodec.NAME));

    vertx
      .eventBus()
      .<Event>consumer(INPUT_CHANNEL)
      .toFlowable()
      .subscribe(evt -> processEvent(evt.body()));

    advanceTick();
    return Completable.complete();
  }
}
