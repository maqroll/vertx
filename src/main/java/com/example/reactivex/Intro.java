package com.example.reactivex;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

public class Intro {

  // Observable<T>  A stream of events of type T. Does not support back-pressure.Timer events, observable source where we cannot apply back-pressure like GUI events
  // Flowable<T>    A stream of events of type T where back-pressure can be applied.  Network data, filesystem inputs
  // Single<T>      A source that emits exactly one event of type T. Fetching an entry from a data store by key
  // Maybe<T>       A source that may emit one event of type T, or none. Fetching an entry from a data store by key, but the key may not exist
  // Completable    A source that notifies of some action having completed, but no value is being given. Deleting files
  public static void main(String[] args) throws InterruptedException {
    Observable.just(1, 2, 3)
      .map(Object::toString)
      .map(s -> "@" + s)
      .subscribe(System.out::println);

    Observable.<String>error(() -> new RuntimeException("Woops"))
      .map(String::toUpperCase)
      .subscribe(System.out::println, Throwable::printStackTrace);

    Single<String> s1 = Single.just("foo");
    Single<String> s2 = Single.just("bar");
    Flowable<String> m = Single.merge(s1, s2);
    m.subscribe(System.out::println);

    Observable
      .just("--", "this", "is", "--", "a", "sequence", "of", "items", "!")
      .doOnSubscribe(d -> System.out.println("Subscribed!"))
      .delay(5, TimeUnit.SECONDS)
      .filter(s -> !s.startsWith("--"))
      .doOnNext(x -> System.out.println("doOnNext: " + x))
      .map(String::toUpperCase)
      .buffer(2)
      .subscribe(
        pair -> System.out.println("next: " + pair + " at " + System.currentTimeMillis()),
        Throwable::printStackTrace,
        () -> System.out.println("~Done~"));

    Thread.sleep(10_000);
  }
}
