package io.slinkydeveloper.vertx;

import io.vertx.core.Vertx;

public class TimingApp {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new HttpServerTimingVerticle());
    vertx.deployVerticle(new KafkaTimingVerticle());
    vertx.deployVerticle(new PgTimingVerticle());
  }

}
