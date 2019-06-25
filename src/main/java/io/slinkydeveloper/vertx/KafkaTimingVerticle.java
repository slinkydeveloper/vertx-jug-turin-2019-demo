package io.slinkydeveloper.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;

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
      int driver = Integer.parseInt(rec[0]);
      int lap = Integer.parseInt(rec[1]);
      String time = rec[2];
      //postTiming(driver, lap, time);
    }).subscribe("timing_input");

    // TODO when new timing is added
    //            producer.write(
    //              KafkaProducerRecord.create("timing_output", String.format("%d %d %s", driver, lap, time))
    //            );

  }
}
