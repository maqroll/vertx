package com.example.tb;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class PublishToTBInStreamingFromKafka extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(PublishToTBInStreamingFromKafka.class);
  private static final String BOUNDARY = "------------------------5b486d5cbfe22191"; // anything will do
  private String authToken;

  WebClient webClient;
  ConfigRetriever retriever;

  @Override
  public Completable rxStart() {
    HttpClientOptions httpClientOptions = new HttpClientOptions();
    httpClientOptions.setTryUseCompression(true);

    webClient = WebClient.create(vertx, new WebClientOptions(httpClientOptions));
    retriever = ConfigRetriever.create(vertx);
    Single<String> authTokenSingle = retriever.rxGetConfig().map(conf -> conf.getString("authToken"));
    authTokenSingle.subscribe(token -> {
        authToken = token;
        this.streamToTB();
      },
      this::finish);

    return Completable.complete();
  }

  private void finish(Throwable throwable) {
    logger.error("Failed to retrieve auth token", throwable);
    vertx.close();
  }

  private void streamToTB() {
    KafkaConsumer<String, String> consumer = KafkaConsumer.<String, String>create(vertx, KafkaConfig.consumerConfig("tb-ingestion"));

    consumer
      .subscribe("data")
      .toFlowable()
      // make sure that flow processing (including http) is SERIALIZED
      // blocking in subscription in event-loop prevents http request from making progress
      // so we observe on worker (with ORDERED to force serialization)
      .observeOn(RxHelper.blockingScheduler(vertx, true))
      .buffer(3, TimeUnit.SECONDS, 1000) // every 3 seconds or 1000 messages (what happens first)
      .filter(e -> !e.isEmpty())
      .doOnNext(e -> {
        Optional<Long> minOffset = e.stream().map(l -> l.offset()).min(Long::compare);
        Optional<Long> maxOffset = e.stream().map(l -> l.offset()).max(Long::compare);
        logger.error("next {}-{}", minOffset, maxOffset);
      })
      .map(this::streamBatchToTB)
      // retryWhen is ineffective without this map
      // because if the subscription to the single is done inside onNext
      // errors didn't get signalled in the main flowable
      .map(e -> e.toFuture().get())
      .doOnError(err -> {
        logger.error("Woops", err);
      })
      // ineffective because consumer buffers messages
      // better replace consumer
      .retryWhen(this::retryLater)
      .subscribe(resp -> {
          // mapping using streamBatchToTB doesn't really add anything
          // better move that to subscribe loop
          // resp.toFuture().get(); // wait until httpresponse completes
          Thread.sleep(10000); // artificial delay because of tinybird's quotas
          //consumer.commit();
        },
        t -> {
          logger.error("Fail!!", t);
        });
    // TODO: inmune to failure both in Kafka and backend
    // TODO: log partitions/offsets in failure
    // TODO: move resp.toFuture().get() to map
  }

  private Single<HttpResponse<String>> streamBatchToTB(List<KafkaConsumerRecord<String, String>> kafkaConsumerRecords) {
    logger.error("Starting web request");
    // Underlying webclient either sends chunked or not depending on the size.
    // This code is going to FAIL for small files (not chunked)
    Single<Buffer> header = Single.just(Buffer.buffer("dia;from;to;tipo;kwh;unit_price;cost\r\n"));
    Single<Buffer> epilogue = Single.just(Buffer.buffer(
      "\r\n" +
        "--" + BOUNDARY + "--\r\n"));

    Buffer prologue = Buffer.buffer("--" + BOUNDARY + "\r\n" +
      "Content-Disposition: form-data; name=\"csv\"; filename=\"tmp.csv\"\r\n" +
      "Content-Type: application/octet-stream\r\n" +
      "\r\n");

    Flowable<Buffer> preamble = Flowable.just(prologue);

    Flowable<Buffer> buffer =
      Flowable.concat(preamble,
        header.toFlowable(),
        Flowable.fromIterable(kafkaConsumerRecords).map(r -> Buffer.buffer(r.value() + "\r\n")),
        epilogue.toFlowable());

    return webClient
      //.postAbs("http://localhost:8080/v0/datasources?name=luz")
      .postAbs("https://api.tinybirdo.co/v0/datasources?name=luz&mode=append")
      .putHeader("transfer-encoding", "chunked")
      .putHeader("Authorization", "Bearer " + authToken)
      .putHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
      .expect(ResponsePredicate.status(200))
      .as(BodyCodec.string())
      .rxSendStream(buffer)
      ;
    //.toFlowable();
  }

  private Flowable<Throwable> retryLater(Flowable<Throwable> errs) {
    return errs.delay(10, TimeUnit.SECONDS, RxHelper.blockingScheduler(vertx));
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx
      .rxDeployVerticle(new PublishToTBInStreamingFromKafka())
      .ignoreElement()
      .subscribe();
  }
}
