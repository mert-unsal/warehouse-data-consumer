package com.ikea.warehouse_data_consumer.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "spring.kafka")
public record KafkaConfigurationProperties(String bootstrapServers,
                                           CustomProducerProperties producer,
                                           CustomConsumerProperties consumer) {

    public record CustomProducerProperties(String keySerializer,
                                           String valueSerializer,
                                           String acks,
                                           int retries,
                                           int batchSize,
                                           int lingerMs,
                                           int bufferMemory,
                                           boolean enableIdempotence,
                                           int maxInFlightRequestsPerConnection,
                                           int concurrency,
                                           Map<String, Object> properties
    ) {}

    public record CustomConsumerProperties(String keyDeserializer,
                                           String valueDeserializer,
                                           String groupId,
                                           String autoOffsetReset,
                                           boolean enableAutoCommit,
                                           int maxPollRecords,
                                           int fetchMaxWait,
                                           int maxPollIntervalMs,
                                           int fetchMinBytes,
                                           int autoCommitInterval,
                                           String ackMode,
                                           int concurrency,
                                           boolean batchListener,
                                           Map<String, Object> properties
    ) {}
};