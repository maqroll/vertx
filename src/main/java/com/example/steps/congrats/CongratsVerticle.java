package com.example.steps.congrats;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CongratsVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(CongratsVerticle.class);

  private WebClient webClient;

  @Override
  public Completable rxStart() {
    webClient = WebClient.create(vertx);

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumerConfig("congrats-service"))
      .subscribe("daily.step.updates")
      .toFlowable()
      .filter(this::above10k)
      .distinct(KafkaConsumerRecord::key) // In memory (not persistent) -> is possible to send more than one email to the same user and same date.
      .flatMapSingle(this::sendmail)
      .doOnError(err -> logger.error("Woops", err))
      .retryWhen(this::retryLater)
      .subscribe(mailResult -> logger.info("Congratulated {}", mailResult.getRecipients()));

    return Completable.complete();
  }

  private Single<MailResult> sendmail(KafkaConsumerRecord<String, JsonObject> record) {
    logger.info("Send congrats to user on walking more than 10k steps on one day");
    return Single.just(new MailResult()); // empty recipients
  }

  private Single<String> getEmail(String username) {
    return webClient
      .get(3000, "localhost", "/" + username)
      .as(BodyCodec.jsonObject())
      .rxSend()
      .map(HttpResponse::body)
      .map(json -> json.getString("email"));
  }

  private MailMessage makeEmail(Integer stepsCount, String email) {
    return new MailMessage()
      .setFrom("noreply@tenksteps.tld")
      .setTo(email)
      .setSubject("You made it!")
      .setText("Congratulations on reaching " + stepsCount + " steps today!\n\n- The 10k Steps Team\n");
  }

  private boolean above10k(KafkaConsumerRecord<String, JsonObject> record) {
    return record.value().getInteger("stepsCount") >= 10_000;
  }

  private Flowable<Throwable> retryLater(Flowable<Throwable> errs) {
    return errs.delay(10, TimeUnit.SECONDS, RxHelper.scheduler(vertx));
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.rxDeployVerticle(new CongratsVerticle())
      .subscribe(ok -> logger.info("Ready to notify people reaching more than 10k steps"));
  }
}
