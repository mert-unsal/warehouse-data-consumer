package com.ikea.warehouse_data_consumer.consumer;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "${KAFKA_TOPIC_INVENTORY:ikea.warehouse.inventory.update.topic}",
            containerFactory = "kafkaListenerContainerFactoryInventory"
    )
    public void consume(InventoryUpdateEvent event, Acknowledgment ack) {
        if (event == null || event.inventory() == null || event.inventory().isEmpty()) {
            log.warn("Received empty inventory update event; acking.");
            ack.acknowledge();
            return;
        }
        inventoryService.process(event);
        ack.acknowledge();
    }
}
