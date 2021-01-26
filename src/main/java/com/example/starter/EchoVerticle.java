package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.net.NetSocket;

public class EchoVerticle extends AbstractVerticle {
  private static int numberOfConnections = 0;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    final CompositeFuture netFutures = CompositeFuture.all(
      vertx.createNetServer().connectHandler(EchoVerticle::handleNewClient).listen(3000),
      vertx.createHttpServer().requestHandler(request -> request.response().end(howMany())).listen(8080)
    );

    vertx.setPeriodic(5000, id -> System.out.println(howMany()));

    if (netFutures.succeeded()) {
      startPromise.complete();
    } else {
      startPromise.fail(netFutures.cause());
    }
  }

  private static void handleNewClient(NetSocket socket) {
    numberOfConnections++;
    socket.handler(buffer -> {
      socket.write(buffer);
      if (buffer.toString().endsWith("/quit\n")) {
        socket.close();
      }
    });
    socket.closeHandler(v -> numberOfConnections--);
  }

  private static String howMany() {
    return "We now have " + numberOfConnections + " connections";
  }
}
