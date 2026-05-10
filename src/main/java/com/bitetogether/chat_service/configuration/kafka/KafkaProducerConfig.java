package com.bitetogether.chat_service.configuration.kafka;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

  private final KafkaProperties kafkaProperties;
  private final ResourceLoader resourceLoader;

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> props = new HashMap<>();

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    KafkaProperties.Properties kafkaProps = kafkaProperties.getProperties();
    if (kafkaProps != null && kafkaProps.getSecurityProtocol() != null) {
      props.put("security.protocol", kafkaProps.getSecurityProtocol());

      KafkaProperties.Properties.Ssl ssl = kafkaProps.getSsl();
      if (ssl != null) {
        configureSSL(props, ssl);
      }
    }

    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  private void configureSSL(Map<String, Object> props, KafkaProperties.Properties.Ssl ssl) {
    try {
      if (ssl.getKeystore() != null) {
        KafkaProperties.Properties.Ssl.Keystore ks = ssl.getKeystore();
        if (ks.getType() != null) {
          props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, ks.getType());
        }
        if (ks.getLocation() != null) {
          Resource resource = resourceLoader.getResource(ks.getLocation());
          props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, resource.getFile().getAbsolutePath());
        }
        if (ks.getPassword() != null) {
          props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, ks.getPassword());
        }
      }
      if (ssl.getTruststore() != null) {
        KafkaProperties.Properties.Ssl.Truststore ts = ssl.getTruststore();
        if (ts.getType() != null) {
          props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, ts.getType());
        }
        if (ts.getLocation() != null) {
          Resource resource = resourceLoader.getResource(ts.getLocation());
          props.put(
              SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, resource.getFile().getAbsolutePath());
        }
        if (ts.getPassword() != null) {
          props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, ts.getPassword());
        }
      }
      if (ssl.getEndpointIdentificationAlgorithm() != null) {
        props.put(
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
            ssl.getEndpointIdentificationAlgorithm());
      }
    } catch (IOException e) {
      log.error("Failed to load SSL keystore/truststore files for producer", e);
      throw new KafkaConsumerConfig.KafkaConfigurationException(
          "Failed to load SSL keystore/truststore files for producer", e);
    }
  }
}
