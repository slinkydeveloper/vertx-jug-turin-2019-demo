package io.slinkydeveloper.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.util.HashMap;
import java.util.Map;

public class KafkaTimingVerticle extends AbstractVerticle {

  private KafkaProducer<String, String> producer;
  private KafkaConsumer<String, String> consumer;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

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
      JsonObject timing = new JsonObject()
        .put("driver", Integer.parseInt(rec[0]))
        .put("lap", Integer.parseInt(rec[1]))
        .put("time", rec[2]);
      vertx.eventBus().send("add_timing.timingsapp", timing);
    }).subscribe("timing_input");

    vertx
      .eventBus()
      .<JsonObject>consumer("new_timing_event.timingsapp")
      .handler(message ->
        producer.write(
          KafkaProducerRecord.create("timing_output", String.format(
            "%d %d %s",
            message.body().getInteger("driver"),
            message.body().getInteger("lap"),
            message.body().getString("time")
          ))
        )
      );

  }
}
