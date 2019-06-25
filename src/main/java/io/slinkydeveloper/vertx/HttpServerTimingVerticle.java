package io.slinkydeveloper.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class HttpServerTimingVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);

    router
      .get("/timing")
      .handler(routingContext -> {
        vertx
          .eventBus()
          .<JsonArray>request("get_timings.timingsapp", null, ar -> {
            if (ar.succeeded()) {
              routingContext
                .response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(ar.result().body().toBuffer());
            } else {
              System.out.println("Error while retrieving stuff: " + ar.cause());
              routingContext
                .response()
                .setStatusCode(500)
                .end();
            }
          });
      });

    router
      .post("/timing")
      .handler(BodyHandler.create())
      .handler(routingContext -> {
        JsonObject obj = routingContext.getBodyAsJson();
        vertx
          .eventBus()
          .send("add_timing.timingsapp", obj);
        routingContext
          .response()
          .setStatusCode(202)
          .end();
      });

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    sockJSHandler.bridge(
      new BridgeOptions()
        .addInboundPermitted(new PermittedOptions().setAddress("get_timings.timingsapp"))
        .addOutboundPermitted(new PermittedOptions().setAddress("new_timing_event.timingsapp"))
    );

    router.route("/eventbus/*").handler(sockJSHandler);

    router.route().handler(StaticHandler.create());

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(8080, ar -> {
        if (ar.succeeded()) startPromise.complete();
        else startPromise.fail(ar.cause());
      });
  }

}
