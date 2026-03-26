package com.bitetogether.chat_service.configuration.kafka;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {
  private final KafkaProperties kafkaProperties;
  private final ResourceLoader resourceLoader;

  @Bean
  public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> props = new HashMap<>();

    // Basic consumer configuration
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put("spring.json.trusted.packages", "*");

    // Security protocol
    KafkaProperties.Properties kafkaProps = kafkaProperties.getProperties();
    if (kafkaProps != null && kafkaProps.getSecurityProtocol() != null) {
      props.put("security.protocol", kafkaProps.getSecurityProtocol());

      // SSL configuration
      KafkaProperties.Properties.Ssl ssl = kafkaProps.getSsl();
      if (ssl != null) {
        configureSSL(props, ssl);
      }
    }

    // Consumer tuning for reactive processing
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 600_000);
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60_000);
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10_000);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5_000);

    return new DefaultKafkaConsumerFactory<>(props);
  }

  private void configureSSL(Map<String, Object> props, KafkaProperties.Properties.Ssl ssl) {
    try {
      configureKeystore(props, ssl.getKeystore());
      configureTruststore(props, ssl.getTruststore());
      configureEndpointIdentification(props, ssl.getEndpointIdentificationAlgorithm());
    } catch (IOException e) {
      log.error("Failed to load SSL keystore/truststore files", e);
      throw new KafkaConfigurationException("Failed to load SSL keystore/truststore files", e);
    }
  }

  private void configureKeystore(
      Map<String, Object> props, KafkaProperties.Properties.Ssl.Keystore keystore)
      throws IOException {
    if (keystore == null) {
      return;
    }

    if (keystore.getType() != null) {
      props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, keystore.getType());
    }

    if (keystore.getLocation() != null) {
      Resource keystoreResource = resourceLoader.getResource(keystore.getLocation());
      String absolutePath = keystoreResource.getFile().getAbsolutePath();
      props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, absolutePath);
      log.info("Loaded SSL keystore from: {}", absolutePath);
    }

    if (keystore.getPassword() != null) {
      props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystore.getPassword());
    }
  }

  private void configureTruststore(
      Map<String, Object> props, KafkaProperties.Properties.Ssl.Truststore truststore)
      throws IOException {
    if (truststore == null) {
      return;
    }

    if (truststore.getType() != null) {
      props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, truststore.getType());
    }

    if (truststore.getLocation() != null) {
      Resource truststoreResource = resourceLoader.getResource(truststore.getLocation());
      String absolutePath = truststoreResource.getFile().getAbsolutePath();
      props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, absolutePath);
      log.info("Loaded SSL truststore from: {}", absolutePath);
    }

    if (truststore.getPassword() != null) {
      props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststore.getPassword());
    }
  }

  private void configureEndpointIdentification(
      Map<String, Object> props, String endpointIdentificationAlgorithm) {
    if (endpointIdentificationAlgorithm != null) {
      props.put(
          SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, endpointIdentificationAlgorithm);
    }
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setConcurrency(3);

    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
    backOff.setInitialInterval(1_000L);
    backOff.setMultiplier(2.0);
    backOff.setMaxInterval(10_000L);

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
    factory.setCommonErrorHandler(errorHandler);

    return factory;
  }

  /** Custom exception for Kafka configuration errors */
  public static class KafkaConfigurationException extends RuntimeException {
    public KafkaConfigurationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
