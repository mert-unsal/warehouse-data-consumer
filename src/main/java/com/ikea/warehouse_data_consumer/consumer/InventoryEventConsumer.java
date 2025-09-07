package com.ikea.warehouse_data_consumer.consumer;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.CustomMongoWriteException;
import com.ikea.warehouse_data_consumer.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.inventory-retry}")
    private String retryTopic;

    @Value("${app.kafka.topics.inventory-error}")
    private String errorTopic;

    @Retryable(
            noRetryFor = {CustomMongoWriteException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    @KafkaListener(
            topics = "${KAFKA_TOPIC_INVENTORY:ikea.warehouse.inventory.update.topic}",
            containerFactory = "kafkaListenerContainerFactoryInventory"
    )
    public void consume(List<InventoryUpdateEvent> inventoryUpdateEventList, Acknowledgment ack) {
        if (ObjectUtils.isEmpty(inventoryUpdateEventList)){
            log.warn("Received empty inventory update inventoryUpdateEventList; acking.");
            ack.acknowledge();
            return;
        }
        inventoryService.process(inventoryUpdateEventList);
        ack.acknowledge();
    }

    @Recover
    public void recover(CustomMongoWriteException customMongoWriteException, List<InventoryUpdateEvent> eventList, Acknowledgment ack) {
        log.error("Recovering from CustomMongoWriteException; events size={}",
                ObjectUtils.isEmpty(eventList) ? 0 : eventList.size(), customMongoWriteException);
        kafkaTemplate.send(retryTopic, customMongoWriteException.getFailedEvents());
        //TODO: copy bath producer from warehouse-data-ingestion-service, use it here
        ack.acknowledge();
    }

    @Recover
    public void recover(Exception exception, List<InventoryUpdateEvent> eventList, Acknowledgment ack) {
        log.error("Recovering from Exception; events size={}",
                ObjectUtils.isEmpty(eventList) ? 0 : eventList.size(), exception);
        kafkaTemplate.send(errorTopic, eventList);
        //TODO: copy bath producer from warehouse-data-ingestion-service, use it here
        ack.acknowledge();
    }


}
