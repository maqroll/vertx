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

// Have a look at
// https://gist.github.com/Stwissel/a7f8ce79785afd49eb2ced69b56335de
// Zip layer on top of readstream.
// Tinybird doesn't support compression so keep it aside for the time being.
public class ZipRxReadStream implements ReadStream<Buffer> {
  private final Logger logger = LoggerFactory.getLogger(ZipRxReadStream.class);
  private final ReadStream decorated;
  private Handler<Throwable> errorHandler;
  private Handler<Buffer> handler;
  private Handler<Void> endHandler;

  public ZipRxReadStream(ReadStream<Buffer> decorated) {
    this.decorated = decorated;
  }

  @Override
  public io.vertx.core.streams.ReadStream getDelegate() {
    return decorated.getDelegate();
  }

  @Override
  public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    errorHandler = new Handler<Throwable>() {
      @Override
      public void handle(Throwable event) {
        handler.handle(event);
      }
    };

    decorated.exceptionHandler(errorHandler);
    return this;
  }

  @Override
  public ReadStream<Buffer> handler(Handler<Buffer> handler) {
    this.handler = new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        handler.handle(event);
      }
    };

    decorated.handler(this.handler);
    return this;
  }

  @Override
  public ReadStream<Buffer> pause() {
    decorated.pause();
    return this;
  }

  @Override
  public ReadStream<Buffer> resume() {
    decorated.resume();
    return this;
  }

  @Override
  public ReadStream<Buffer> fetch(long amount) {
    decorated.fetch(amount);
    return this;
  }

  @Override
  public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
    this.endHandler = new Handler<Void>() {
      @Override
      public void handle(Void event) {
        endHandler.handle(event);
      }
    };

    decorated.endHandler(this.endHandler);
    return this;
  }

  @Override
  public Pipe<Buffer> pipe() {
    // TODO
    return decorated.pipe();
  }

  @Override
  public void pipeTo(WriteStream<Buffer> dst, Handler<AsyncResult<Void>> handler) {
    // TODO
    decorated.pipeTo(dst, handler);
  }

  @Override
  public void pipeTo(WriteStream<Buffer> dst) {
    // TODO
    decorated.pipeTo(dst);
  }

  @Override
  public Completable rxPipeTo(WriteStream<Buffer> dst) {
    // TODO
    return decorated.rxPipeTo(dst);
  }

  @Override
  public Observable<Buffer> toObservable() {
    // TODO
    return decorated.toObservable();
  }

  @Override
  public Flowable<Buffer> toFlowable() {
    // TODO
    return decorated.toFlowable();
  }
}
