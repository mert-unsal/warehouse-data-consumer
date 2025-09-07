package com.ikea.warehouse_data_consumer.consumer;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.service.InventoryService;
import com.ikea.warehouse_data_consumer.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventRetryConsumer {

    private final InventoryService inventoryService;
    private final KafkaProducerService kafkaProducerService;

    @Value("${app.kafka.topics.inventory-retry}")
    private String retryTopic;

    @Value("${app.kafka.topics.inventory-error}")
    private String errorTopic;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    @KafkaListener(
            topics = "${KAFKA_TOPIC_INVENTORY_RETRY:ikea.warehouse.inventory.update.topic.retry}",
            containerFactory = "kafkaListenerContainerFactoryInventory"
    )
    public void consume(InventoryUpdateEvent inventoryUpdateEvent, Acknowledgment ack) {
        inventoryService.persistEvent(inventoryUpdateEvent);
        ack.acknowledge();
    }

    @Recover
    public void recover(Exception e, InventoryUpdateEvent inventoryUpdateEvent, Acknowledgment ack) {
        log.error("InventoryEventRetryConsumer recover exception", e);
        kafkaProducerService.send(errorTopic, inventoryUpdateEvent.artId(), inventoryUpdateEvent);
        ack.acknowledge();
    }
}

