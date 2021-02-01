package com.example.services;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ExampleDataServiceImpl implements ExampleDataService {
  public ExampleDataServiceImpl(Vertx vertx) {
    // empty by design
  }

  @Override
  public void get(String param, Handler<AsyncResult<JsonObject>> handler) {
    if (!"abc".equals(param)) {
      JsonObject data = new JsonObject()
        .put("sensorId", param)
        .put("value", new Double(0.0));

      handler.handle(Future.succeededFuture(data));
    } else {
      handler.handle(Future.failedFuture("No value has been observed for " + param));
    }
  }

  @Override
  public void average(Handler<AsyncResult<JsonObject>> handler) {
    JsonObject data = new JsonObject().put("average", new Double(0.0));
    handler.handle(Future.succeededFuture(data));
  }
}
