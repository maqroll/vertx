package com.example.aggregate;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class LocalDummyMessageCodec<T> implements MessageCodec<T, T> {
  public static final String NAME = "LocalDummyMessageCodec";

  @Override
  public void encodeToWire(Buffer buffer, T t) {}

  @Override
  public T decodeFromWire(int pos, Buffer buffer) {
    return null;
  }

  @Override
  public T transform(T t) {
    return t;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
