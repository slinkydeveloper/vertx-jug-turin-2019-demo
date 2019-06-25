package io.slinkydeveloper.vertx;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;

import java.util.stream.Collector;

public class PgTimingVerticle extends AbstractVerticle {

  private PgPool pgClient;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    // Connect options
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(5432)
      .setHost("localhost")
      .setDatabase("timing")
      .setUser("postgres")
      .setPassword("postgres");

    // Create the client pool
    this.pgClient = PgPool.pool(vertx, connectOptions, new PoolOptions());

    vertx
      .eventBus()
      .consumer("get_timings.timingsapp")
      .handler(message -> {
        getAllTimings(ar -> {
          if (ar.succeeded()) {
            message.reply(ar.result());
          } else {
            message.fail(1, ar.cause().getMessage());
            System.err.format("Something went wrong while retrieving all timings: %s", ar.cause().getMessage());
          }
        });
      });

    vertx
      .eventBus()
      .<JsonObject>consumer("add_timing.timingsapp")
      .handler(message -> {
        JsonObject timingEntry = message.body();
        postTiming(
          timingEntry.getInteger("driver"),
          timingEntry.getInteger("lap"),
          timingEntry.getString("time")
        );
      });
  }

  private void postTiming(int driver, int lap, String time) {
    pgClient.preparedQuery(
      "INSERT INTO timing.time (driver_id, lap, time) VALUES ($1, $2, $3)",
      Tuple.of(driver, lap, time),
      ar -> {
        if (ar.succeeded()) {
          vertx
            .eventBus()
            .publish(
              "new_timing_event.timingsapp",
              new JsonObject()
                .put("driver", driver)
                .put("lap", lap)
                .put("time", time)
            );
          System.out.format("Added timing for driver %d at lap %d with time %s\n", driver, lap, time);
        } else if (ar.failed())
          System.out.println("Something went wrong! " + ar.cause());
      }
    );
  }

  private void getAllTimings(Handler<AsyncResult<JsonArray>> handler) {
    pgClient.query(
      "SELECT time.driver_id, time.lap, time.time FROM timing.time JOIN timing.driver ON driver.id = time.driver_id",
      Collector.of(
        JsonArray::new,
        (ja, row) -> ja.add(
          new JsonObject()
            .put("driver_id", row.getInteger("driver_id"))
            .put("lap", row.getInteger("lap"))
            .put("time", row.getString("time"))
        ),
        JsonArray::addAll
      ),
      ar -> {
        if (ar.failed()) {
          handler.handle(Future.failedFuture(ar.cause()));
        } else {
          handler.handle(Future.succeededFuture(ar.result().value()));
        }
      });
  }
}
