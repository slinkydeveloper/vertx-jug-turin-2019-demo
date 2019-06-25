package io.slinkydeveloper.vertx;

import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

public class MainVerticle extends AbstractVerticle {

  private PgPool pgClient;
  private KafkaProducer<String, String> producer;
  private KafkaConsumer<String, String> consumer;

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

    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("group.id", "my_group");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "true");
    config.put("acks", "1");
    config.put("auto.commit.interval.ms", "100");

    this.producer = KafkaProducer.create(vertx, config);
    this.consumer = KafkaConsumer.create(vertx, config);

    this.consumer.handler(record -> {
      String[] rec = record.value().split("\\s");
      int driver = Integer.parseInt(rec[0]);
      int lap = Integer.parseInt(rec[1]);
      String time = rec[2];
      postTiming(driver, lap, time);
    }).subscribe("timing_input");

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
      getAllTimings(ar -> {
        if (ar.succeeded()) {
          req
            .response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json")
            .end(ar.result().toBuffer());
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
        postTiming(
          obj.getInteger("driver"),
          obj.getInteger("lap"),
          obj.getString("time")
        );
        req
          .response()
          .setStatusCode(202)
          .end();
      });
    } else {
      req.response().setStatusCode(405).end();
    }
  }
  private void postTiming(int driver, int lap, String time) {
    pgClient.preparedQuery(
        "INSERT INTO timing.time (driver_id, lap, time) VALUES ($1, $2, $3)",
        Tuple.of(driver, lap, time),
        ar -> {
          if (ar.succeeded()) {
            System.out.format("Added timing for driver %d at lap %d with time %s\n", driver, lap, time);
            producer.write(
              KafkaProducerRecord.create("timing_output", String.format("%d %d %s", driver, lap, time))
            );
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

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
