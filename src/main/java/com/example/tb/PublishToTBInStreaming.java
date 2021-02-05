package com.example.tb;

import io.reactivex.Completable;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This verticle explores loading data in STREAMING to Tinybird.
public class PublishToTBInStreaming extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(PublishToTBInStreaming.class);
  private static final String BOUNDARY = "------------------------5b486d5cbfe22191";

  WebClient webClient;
  ConfigRetriever retriever;

  @Override
  public Completable rxStart() {
    webClient = WebClient.create(vertx);
    retriever = ConfigRetriever.create(vertx);
    retriever.rxGetConfig().map(conf -> conf.getString("authToken")).subscribe(this::streamToTB, this::finish);

    return Completable.complete();
  }

  private void finish(Throwable throwable) {
    logger.error("Failed to retrieve auth token", throwable);
    vertx.close();
  }

  private void streamToTB(String authToken) {
    // Underlying webclient either sends chunked or not depending on the size.
    // This code is going to FAIL for small files (not chunked)
    OpenOptions options = new OpenOptions();
    AsyncFile file = vertx.fileSystem().openBlocking("/tmp/FE21137002276387.csv", options).setReadBufferSize(8192*6);

    RxReadStreamImpl readStream = new RxReadStreamImpl(file, BOUNDARY);

    webClient
      .postAbs("https://api.tinybird.co/v0/datasources?name=luz")
      .putHeader("transfer-encoding","chunked")
      .putHeader("Authorization", "Bearer " + authToken)
      .putHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
      .expect(ResponsePredicate.status(200))
      .as(BodyCodec.string())
      .rxSendStream(readStream)
      .subscribe(r -> {
          logger.info("ok");
          logger.info(r.body());
          vertx.close();
        },
        err -> {
          logger.info(err.getMessage());
          vertx.close();
        });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx
      .rxDeployVerticle(new Server())
      .ignoreElement()
      .andThen(
        vertx
      .rxDeployVerticle(new PublishToTBInStreaming())
      )
      .ignoreElement()
      .subscribe();
  }
}
