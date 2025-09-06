package com.ikea.warehouse_data_consumer.data.event;

//TODO: If it is not required, remove this object
public record KafkaCommonErrorEvent(
    String key,
    Object originalMessage,
    String originalTopic,
    String errorMessage,
    Long timestamp
) {}
