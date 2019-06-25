package io.slinkydeveloper.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx
        .createHttpServer()
        .requestHandler(this::handleRequest)
        .listen(8080, ar -> {
      if (ar.succeeded()) startPromise.complete();
      else startPromise.fail(ar.cause());
    });
  }

  private void handleRequest(HttpServerRequest req) {
    if (!req.path().equals("/timing")) {
      req.response().setStatusCode(404).end();
      return;
    }

    if (req.method() == HttpMethod.GET) {
      // Handle GET /timing
    } else if (req.method() == HttpMethod.POST) {
      // Handle POST /timing
    } else {
      req.response().setStatusCode(405).end();
    }
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
