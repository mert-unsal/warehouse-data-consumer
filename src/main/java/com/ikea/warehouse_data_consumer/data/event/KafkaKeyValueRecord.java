package com.ikea.warehouse_data_consumer.data.event;

import lombok.Builder;

@Builder
public record KafkaKeyValueRecord(String key, Object event) {
}
