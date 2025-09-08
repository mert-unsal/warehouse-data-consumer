package com.ikea.warehouse_data_consumer.consumer.inventory;

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

    @Value("${app.kafka.consumer.inventory.errorTopic}")
    private String errorTopic;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    @KafkaListener(
            topics = "${app.kafka.consumer.inventory.retryTopic}",
            containerFactory = "kafkaListenerContainerFactoryInventory"
    )
    public void consume(InventoryUpdateEvent inventoryUpdateEvent, Acknowledgment ack) {
        inventoryService.proceedInventoryUpdateEvent(inventoryUpdateEvent);
        ack.acknowledge();
    }

    @Recover
    public void recover(Exception exception, InventoryUpdateEvent inventoryUpdateEvent, Acknowledgment ack) {
        log.error("InventoryEventRetryConsumer recover exception", exception);
        kafkaProducerService.send(errorTopic, inventoryUpdateEvent.artId(), inventoryUpdateEvent);
        ack.acknowledge();
    }
}

