package io.slinkydeveloper.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class HttpServerTimingVerticle extends AbstractVerticle {

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
      vertx
        .eventBus()
        .<JsonArray>request("get_timings.timingsapp", null, ar -> {
          if (ar.succeeded()) {
            req
              .response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(ar.result().body().toBuffer());
          } else {
            System.out.println("Error while retrieving stuff: " + ar.cause());
            req
              .response()
              .setStatusCode(500)
              .end();
          }
        });
    } else if (req.method() == HttpMethod.POST) {
      // Handle POST /timing
      req.bodyHandler(bodyBuf -> {
        JsonObject obj = bodyBuf.toJsonObject();
        vertx
          .eventBus()
          .send("add_timing.timingsapp", obj);
        req
          .response()
          .setStatusCode(202)
          .end();
      });
    } else {
      req.response().setStatusCode(405).end();
    }
  }

}
