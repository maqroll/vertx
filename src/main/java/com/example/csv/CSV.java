package com.example.csv;

import io.reactivex.Completable;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.AsyncFile;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class CSV extends AbstractVerticle {
  private AtomicLong counter = new AtomicLong();

  @Override
  public Completable rxStart() {
    Instant start = Instant.now();

    OpenOptions options = new OpenOptions();
    vertx.fileSystem()
      .openBlocking("/tmp/ten_million.csv", options)
      .setReadBufferSize(8192*4)
      .toFlowable()
      .doOnComplete(() -> {
          System.out.println(counter.get());
          System.out.println(Duration.between(start, Instant.now()).toString());
          vertx.close();
        }
      ).map(Buffer::length)
      .subscribe(
        b -> {
          counter.incrementAndGet();
        },
        e -> {
          System.out.println("end");
        }
      );

    return Completable.complete();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx
      .rxDeployVerticle(new CSV())
      .ignoreElement()
      .subscribe();
  }
}
