package com.bitetogether.chat_service.configuration.kafka;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

  private final KafkaProperties kafkaProperties;

  @Bean
  public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> props = new HashMap<>();

    // Basic connection and group configuration from bound properties
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());

    // Let Spring Kafka's ErrorHandlingDeserializer2 delegate to StringDeserializer
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    // Start from earliest if there is no committed offset (matches application-kafka.yml)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    // Trust all packages is not strictly needed for String deserialization, but keep it aligned
    // with producer/consumer YAML configuration in case of future Json deserialization usage.
    props.put("spring.json.trusted.packages", "*");

    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory) {

    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);

    // Configure a simple error handler with backoff; this now works because
    // ErrorHandlingDeserializer2 converts SerializationExceptions into DeserializationException
    // that the DefaultErrorHandler can process.
    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
    backOff.setInitialInterval(1_000L);
    backOff.setMultiplier(2.0);
    backOff.setMaxInterval(10_000L);

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
    factory.setCommonErrorHandler(errorHandler);

    return factory;
  }
}

