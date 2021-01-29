package com.example.services;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen // Annotation used to generate an event-bus proxy
//@VertxGen
// Vert.x does not rely on magic through bytecode engineering or reflection at runtime,
// so service proxies and handlers need to be written and compiled.
// Vert.x comes with code generators instead, so you will generate both the service proxies
// and handlers at compilation time rather than write them yourself.
public interface ExampleDataService {

  // BOILERPLATE
  // Factory method for creating a service instance
  static ExampleDataService create(Vertx vertx) {
    return new ExampleDataServiceImpl(vertx);
  }

  // Factory method for creating a proxy
  static ExampleDataService createProxy(Vertx vertx, String address) {
    return new ExampleDataServiceVertxEBProxy(vertx, address); // auto-generated proxy
  }

  // BUSINESS
  void get(String param, Handler<AsyncResult<JsonObject>> handler);

  void average(Handler<AsyncResult<JsonObject>> handler);
}
