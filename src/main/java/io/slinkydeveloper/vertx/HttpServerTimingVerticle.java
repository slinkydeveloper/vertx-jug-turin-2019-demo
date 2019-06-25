package io.slinkydeveloper.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
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
      //TODO
      //       getAllTimings(ar -> {
      //        if (ar.succeeded()) {
      //          req
      //            .response()
      //            .setStatusCode(200)
      //            .putHeader("content-type", "application/json")
      //            .end(ar.result().toBuffer());
      //        } else {
      //          System.out.println("Error while retrieving stuff: " + ar.cause());
      //          req
      //            .response()
      //            .setStatusCode(500)
      //            .end();
      //        }
      //      });
    } else if (req.method() == HttpMethod.POST) {
      // Handle POST /timing
      // TODO
      //      req.bodyHandler(bodyBuf -> {
      //        JsonObject obj = bodyBuf.toJsonObject();
      //        postTiming(
      //          obj.getInteger("driver"),
      //          obj.getInteger("lap"),
      //          obj.getString("time")
      //        );
      //        req
      //          .response()
      //          .setStatusCode(202)
      //          .end();
      //      });
    } else {
      req.response().setStatusCode(405).end();
    }
  }

}
