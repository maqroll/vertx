package com.example.channelsAndObjects;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class DummyMessageCodec<T> implements MessageCodec<T,T> {
  @Override
  public void encodeToWire(Buffer buffer, T t) {

  }

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
    return "DummyMessageCodec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
