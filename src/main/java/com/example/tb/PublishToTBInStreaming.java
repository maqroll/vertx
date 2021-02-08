package com.example.tb;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.web.client.WebClientOptions;
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
public class PublishToTBInStreaming extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(PublishToTBInStreaming.class);
  private static final String BOUNDARY = "------------------------5b486d5cbfe22191";

  WebClient webClient;
  ConfigRetriever retriever;

  @Override
  public Completable rxStart() {
    HttpClientOptions httpClientOptions = new HttpClientOptions();
    httpClientOptions.setTryUseCompression(true);

    webClient = WebClient.create(vertx, new WebClientOptions(httpClientOptions));
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
    AsyncFile file = vertx.fileSystem().openBlocking("/tmp/FE21137002276387.csv", options).setReadBufferSize(8192 * 2);

    Single<Buffer> epilogo = Single.just(Buffer.buffer(
      "\r\n" +
        "--" + BOUNDARY + "--\r\n"));

    Single<Buffer> prologo = Single.just(Buffer.buffer("--" + BOUNDARY + "\r\n" +
      "Content-Disposition: form-data; name=\"csv\"; filename=\"tmp.csv\"\r\n" +
      "Content-Type: application/octet-stream\r\n" +
      "\r\n"));

    Flowable<Buffer> bufferFlowable1 =
      Flowable.concat(prologo.toFlowable(),
        file.toFlowable(),
        epilogo.toFlowable());

     /*prologo.toFlowable()
      .concatWith(file.toFlowable().observeOn(RxHelper.blockingScheduler(vertx)))
      .concatWith(epilogo);*/

    webClient
      .postAbs("http://localhost:8080/v0/datasources?name=luz")
      //.postAbs("https://api.tinybird.co/v0/datasources?name=luz3")
      .putHeader("transfer-encoding", "chunked")
      .putHeader("Authorization", "Bearer " + authToken)
      .putHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
      .expect(ResponsePredicate.status(200))
      .as(BodyCodec.string())
      .rxSendStream(bufferFlowable1)
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
