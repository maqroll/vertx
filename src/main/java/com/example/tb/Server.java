package com.example.tb;

import io.vertx.core.AbstractVerticle;

public class Server extends AbstractVerticle {

  @Override
  public void start() throws Exception {

    vertx.createHttpServer().requestHandler(req -> {
      System.out.println("Got form with content-type " + req.getHeader("content-type"));
      /*req.setExpectMultipart(true);
      req.uploadHandler(h -> {
        h.streamToFileSystem("/tmp/rcv.csv");
      });*/
      req.body(b -> {
        System.out.println(b.result());
      });
      req.endHandler(v -> {
        req.response().end();
      });

    }).listen(8080, listenResult -> {
      if (listenResult.failed()) {
        System.out.println("Could not start HTTP server");
        listenResult.cause().printStackTrace();
      } else {
        System.out.println("Server started");
      }
    });
  }
}
