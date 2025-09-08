package com.ikea.warehouse_data_consumer.consumer.inventory;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.ArticleDocumentMongoWriteException;
import com.ikea.warehouse_data_consumer.service.InventoryService;
import com.ikea.warehouse_data_consumer.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final KafkaProducerService kafkaProducerService;

    @Value("${app.kafka.consumer.inventory.retryTopic}")
    private String retryTopic;

    @Value("${app.kafka.consumer.inventory.errorTopic}")
    private String errorTopic;

    @Retryable(
            noRetryFor = {ArticleDocumentMongoWriteException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    @KafkaListener(
            topics = "${app.kafka.consumer.inventory.topic}",
            containerFactory = "batchKafkaListenerContainerFactoryInventory"
    )
    public void consume(List<InventoryUpdateEvent> inventoryUpdateEventList, Acknowledgment ack) {
        if (ObjectUtils.isEmpty(inventoryUpdateEventList)) {
            log.warn("Received empty inventory update inventoryUpdateEventList; acking.");
            ack.acknowledge();
            return;
        }
        try {
            inventoryService.proceedInventoryUpdateBatchEvent(inventoryUpdateEventList);
            ack.acknowledge();
        } catch (ArticleDocumentMongoWriteException ex) {
            // Handle known business exception locally to prevent container-level retries
            Map<String, InventoryUpdateEvent> retryableEventMap = ex.getFailedEvents()
                    .stream()
                    .collect(Collectors.toMap(InventoryUpdateEvent::artId, event -> event));

            Map<String, InventoryUpdateEvent> nonRetryableEventMap = ex.getCriteriaNotMatchedEvents()
                    .stream()
                    .collect(Collectors.toMap(InventoryUpdateEvent::artId, event -> event));

            log.error("Recovering from CustomMongoWriteException; with retryable event size={}, non-retryable event size={}",
                    ObjectUtils.isEmpty(retryableEventMap) ? 0 : retryableEventMap.size(),
                    ObjectUtils.isEmpty(nonRetryableEventMap) ? 0 : nonRetryableEventMap.size(),
                    ex);

            kafkaProducerService.sendBatch(retryTopic, retryableEventMap);
            kafkaProducerService.sendBatch(errorTopic, nonRetryableEventMap);
        } finally {
            ack.acknowledge();
        }
    }

    @Recover
    public void recover(Exception exception, List<InventoryUpdateEvent> eventList, Acknowledgment ack) {
        log.error("Recovering from Exception; events size={}",
                ObjectUtils.isEmpty(eventList) ? 0 : eventList.size(), exception);
        kafkaProducerService.sendBatch(errorTopic, eventList.stream()
                .collect(Collectors.toMap(InventoryUpdateEvent::artId, event -> event)));
        ack.acknowledge();
    }
}
