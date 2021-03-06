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
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PublishToTBInStreamingFromKafkaWithExplicitPolling extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(PublishToTBInStreamingFromKafkaWithExplicitPolling.class);
  private static final String BOUNDARY = "------------------------5b486d5cbfe22191"; // anything will do
  private static final String CONSUMER_GROUP = "tb-ingestion";
  private static final String TOPIC = "data";
  private static final String CSV_HEADER = "dia;from;to;tipo;kwh;unit_price;cost\r\n";
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

  private <K,V> List<KafkaConsumerRecord<K,V>> toList(KafkaConsumerRecords<K,V> records) {
    List<KafkaConsumerRecord<K,V>> res = new ArrayList<>();

    for(int i=0; i<records.size(); i++) {
      res.add(records.recordAt(i));
    }

    return res;
  }

  private Flowable<HttpResponse<String>> init(AtomicReference<KafkaConsumer<String, String>> consumer) {
    consumer.set(KafkaConsumer.<String, String>create(vertx, KafkaConfig.consumerConfig(CONSUMER_GROUP)).subscribe(TOPIC));

    return Flowable
      .interval(10, TimeUnit.SECONDS, RxHelper.blockingScheduler(vertx))
      .map(l -> {
        return consumer.get().rxPoll(Duration.of(2, ChronoUnit.SECONDS));
      })
      .map(b -> b.toFuture().get())
      .filter(b -> !b.isEmpty())
      .map(this::toList)
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
      // if something goes wrong keeps re-trying FOREVER from LAST COMMITTED offset
      .onErrorResumeNext(t -> {
          consumer.get().close();
          consumer.set(KafkaConsumer.<String, String>create(vertx, KafkaConfig.consumerConfig(CONSUMER_GROUP)));
          return init(consumer);
        }
      );
  }

  private void streamToTB() {
    AtomicReference<KafkaConsumer<String, String>> consumer = new AtomicReference<>();

    init(consumer)
      .subscribe(resp -> {
          // At least once semantic
          consumer.get().rxCommit().blockingGet();
        },
        t -> {
          logger.error("Fail!!", t);
        });
  }

  private Single<HttpResponse<String>> streamBatchToTB(List<KafkaConsumerRecord<String, String>> kafkaConsumerRecords) {
    logger.error("Starting web request");
    // Underlying webclient either sends chunked or not depending on the size.
    // This code is going to FAIL for small files (not chunked)
    Single<Buffer> header = Single.just(Buffer.buffer(CSV_HEADER));
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

    // Aparently TB ignores Content-Encoding header in multipart data.
    return webClient
      //.postAbs("http://localhost:8080/v0/datasources?name=luz")
      .postAbs("https://api.tinybird.co/v0/datasources?name=luz&mode=append")
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
      .rxDeployVerticle(new PublishToTBInStreamingFromKafkaWithExplicitPolling())
      .ignoreElement()
      .subscribe();
  }
}
