package com.example.tb;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.core.streams.impl.PipeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadStreamImpl implements io.vertx.core.streams.ReadStream<Buffer> {
  private final Logger logger = LoggerFactory.getLogger(ReadStreamImpl.class);

  private final ReadStream decorated;
  private final Buffer prologo;
  private final Buffer epilogo;
  private Handler<Buffer> currentHandler;

  public ReadStreamImpl(ReadStream decorated, String boundary) {
    this.decorated = decorated;
    this.prologo = Buffer.buffer("--" + boundary + "\r\n" +
      "Content-Disposition: form-data; name=\"csv\"; filename=\"tmp.csv\"\r\n" +
      "Content-Type: application/octet-stream\r\n" +
      "\r\n");
    this.epilogo = Buffer.buffer(
      "\r\n" +
      "--" + boundary + "--\r\n");
;  }

  @Override
  public ReadStream exceptionHandler(Handler handler) {
    logger.info("exceptionHandler(...)");
    decorated.exceptionHandler(handler);
    return this;
  }

  @Override
  public ReadStream handler(@Nullable Handler<Buffer> handler) {
    logger.info("handler(...)");
    if (handler == null) {
      decorated.handler(null);
      currentHandler = null;
    } else {
      decorated.handler(new Handler<Buffer>() {
        boolean sendPrologue = false;

        @Override
        public void handle(Buffer event) {
          if (!sendPrologue) {
            handler.handle(prologo);
            sendPrologue = true;
            handler.handle(event);
          } else {
            handler.handle(event);
          }
        }
      });
      currentHandler = handler;
    }
    return this;
  }

  @Override
  public ReadStream pause() {
    logger.info("pause()");
    decorated.pause();
    return this;
  }

  @Override
  public ReadStream resume() {
    logger.info("resume()");
    decorated.resume();
    return this;
  }

  @Override
  public ReadStream fetch(long amount) {
    logger.info("fetch(...)");
    decorated.fetch(amount);
    return this;
  }

  @Override
  public Pipe pipe() {
    logger.info("pipe()");
    pause();
    return new PipeImpl<>(this);
  }

  @Override
  public Future<Void> pipeTo(WriteStream dst) {
    logger.info("pipeTo(...)");
    return decorated.pipeTo(dst);
  }

  @Override
  public void pipeTo(WriteStream dst, Handler handler) {
    logger.info("pipeTo(...)");
    decorated.pipeTo(dst,handler);
  }

  @Override
  public ReadStream endHandler(@Nullable Handler<Void> endHandler) {
    logger.info("endHandler(...)");
    if (endHandler == null) {
      decorated.endHandler(null);
    } else {
      decorated.endHandler(new Handler<Void>() {
        boolean sendEpilogue = false;

        @Override
        public void handle(Void v) {
          if (!sendEpilogue) {
            currentHandler.handle(epilogo);
            sendEpilogue=true;
            endHandler.handle(v);
          }
        }
      });
    }
    return this;
  }
}
