package com.example.tb;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This verticle explores loading data in STREAMING to Tinybird.
// TODO: build a reactive pipeline between Kafka and Tinybird
public class Test extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(Test.class);
  private static final String BOUNDARY = "------------------------5b486d5cbfe22191";

  WebClient webClient;
  ConfigRetriever retriever;

  @Override
  public Completable rxStart() {
    OpenOptions options = new OpenOptions();
    AsyncFile file = vertx.fileSystem().openBlocking("/tmp/FE21137002276387.csv", options).setReadBufferSize(8192 * 6);

    file
      .toFlowable()
      .doOnNext(e -> logger.error("NEXT"))
      //.observeOn(RxHelper.blockingScheduler(vertx))
      .subscribe(b -> {
        Thread.sleep(5_000);
        logger.error("next");
        }, t -> logger.error("Fail", t), () -> logger.error("Complete") );

    return Completable.complete();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx
          .rxDeployVerticle(new Test())
      .ignoreElement()
      .subscribe();
  }
}
