package com.example.aggregate;

import lombok.Data;

@Data
public class AggregateOutput {
  private Type type;

  // AGGREGATE
  private long ts;
  private int count;

  // LATE ARRIVAL
  private int displacement;
}
