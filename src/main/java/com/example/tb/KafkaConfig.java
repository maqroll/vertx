package com.example.tb;

import java.util.HashMap;
import java.util.Map;

class KafkaConfig {

  static Map<String, String> consumerConfig(String group) {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");
    config.put("max.poll.records", "5000");
    config.put("group.id", group);
    return config;
  }
}
