package com.example.tb;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.streams.Pipe;
import io.vertx.reactivex.core.streams.ReadStream;
import io.vertx.reactivex.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RxReadStreamImpl implements ReadStream<Buffer> {
  private final Logger logger = LoggerFactory.getLogger(RxReadStreamImpl.class);
  private final ReadStream<Buffer> decorated;
  private final io.vertx.core.streams.ReadStream delegate;

  public RxReadStreamImpl(ReadStream<Buffer> decorated, String boundary) {
    this.decorated = decorated;
    this.delegate = new ReadStreamImpl(decorated.getDelegate(), boundary);
  }

  @Override
  public io.vertx.core.streams.ReadStream getDelegate() {
    logger.info("getDelegate()");
    return delegate;
  }

  @Override
  public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    logger.info("exceptionHandler(...)");
    decorated.exceptionHandler(handler);
    return this;
  }

  @Override
  public ReadStream<Buffer> handler(Handler<Buffer> handler) {
    logger.info("handler(...)");
    decorated.handler(handler);
    return this;
  }

  @Override
  public ReadStream<Buffer> pause() {
    logger.info("pause()");
    decorated.pause();
    return this;
  }

  @Override
  public ReadStream<Buffer> resume() {
    logger.info("resume()");
    decorated.resume();
    return this;
  }

  @Override
  public ReadStream<Buffer> fetch(long amount) {
    logger.info("fetch(...)");
    decorated.fetch(amount);
    return this;
  }

  @Override
  public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
    logger.info("endHandler(...)");
    decorated.endHandler(endHandler);
    return this;
  }

  @Override
  public Pipe<Buffer> pipe() {
    logger.info("pipe()");
    return decorated.pipe();
  }

  @Override
  public void pipeTo(WriteStream<Buffer> dst, Handler<AsyncResult<Void>> handler) {
    logger.info("pipeTo(...)");
    decorated.pipeTo(dst,handler);
  }

  @Override
  public void pipeTo(WriteStream<Buffer> dst) {
    logger.info("pipeTo(...)");
    decorated.pipeTo(dst);
  }

  @Override
  public Completable rxPipeTo(WriteStream<Buffer> dst) {
    logger.info("rxPipeTo(...)");
    return decorated.rxPipeTo(dst);
  }

  @Override
  public Observable<Buffer> toObservable() {
    logger.info("toObservable(...)");
    return decorated.toObservable();
  }

  @Override
  public Flowable<Buffer> toFlowable() {
    logger.info("toFlowable(...)");
    return decorated.toFlowable();
  }
}
