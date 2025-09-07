package com.ikea.warehouse_data_consumer.config.kafka;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@Profile("!test")
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigurationProperties.bootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaConfigurationProperties.producer().keySerializer());
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaConfigurationProperties.producer().valueSerializer());
        configProps.put(ProducerConfig.ACKS_CONFIG, kafkaConfigurationProperties.producer().acks());
        configProps.put(ProducerConfig.RETRIES_CONFIG, kafkaConfigurationProperties.producer().retries());
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaConfigurationProperties.producer().batchSize());
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, kafkaConfigurationProperties.producer().lingerMs());
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, kafkaConfigurationProperties.producer().bufferMemory());
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, kafkaConfigurationProperties.producer().enableIdempotence());
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, kafkaConfigurationProperties.producer().maxInFlightRequestsPerConnection());

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    public  Map<String, Object> consumerConfigProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigurationProperties.bootstrapServers());
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfigurationProperties.consumer().groupId());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConfigurationProperties.consumer().keyDeserializer());
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConfigurationProperties.consumer().valueDeserializer());
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConfigurationProperties.consumer().autoOffsetReset());
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConfigurationProperties.consumer().enableAutoCommit());
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaConfigurationProperties.consumer().maxPollRecords());
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, kafkaConfigurationProperties.consumer().maxPollIntervalMs());
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, kafkaConfigurationProperties.consumer().fetchMinBytes());
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, kafkaConfigurationProperties.consumer().autoCommitInterval());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.putAll(kafkaConfigurationProperties.consumer().properties());

        return configProps;
    }

    @Bean
    public ConsumerFactory<String, ProductUpdateEvent> productConsumerFactory() {
        Map<String, Object> configProps = consumerConfigProps();
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ProductUpdateEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConsumerFactory<String, InventoryUpdateEvent> inventoryConsumerFactory() {
        Map<String, Object> configProps = consumerConfigProps();
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, InventoryUpdateEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductUpdateEvent> batchKafkaListenerContainerFactoryProduct() {
        ConcurrentKafkaListenerContainerFactory<String, ProductUpdateEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(kafkaConfigurationProperties.consumer().concurrency());
        factory.setBatchListener(Boolean.TRUE);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductUpdateEvent> kafkaListenerContainerFactoryProduct() {
        ConcurrentKafkaListenerContainerFactory<String, ProductUpdateEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(kafkaConfigurationProperties.consumer().concurrency());
        factory.setBatchListener(Boolean.FALSE);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryUpdateEvent> batchKafkaListenerContainerFactoryInventory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryUpdateEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(kafkaConfigurationProperties.consumer().concurrency());
        factory.setBatchListener(Boolean.TRUE);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryUpdateEvent> kafkaListenerContainerFactoryInventory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryUpdateEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(kafkaConfigurationProperties.consumer().concurrency());
        factory.setBatchListener(Boolean.FALSE);
        return factory;
    }
}
