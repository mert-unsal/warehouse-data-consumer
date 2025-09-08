package com.ikea.warehouse_data_consumer.consumer.product;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.ArticleDocumentMongoWriteException;
import com.ikea.warehouse_data_consumer.data.exception.ProductDocumentMongoWriteException;
import com.ikea.warehouse_data_consumer.service.KafkaProducerService;
import com.ikea.warehouse_data_consumer.service.ProductService;
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
public class ProductEventConsumer {

    private final ProductService productService;
    private final KafkaProducerService kafkaProducerService;

    @Value("${app.kafka.consumer.product.retryTopic}")
    private String retryTopic;

    @Value("${app.kafka.consumer.product.errorTopic}")
    private String errorTopic;

    @Retryable(
            noRetryFor = {ProductDocumentMongoWriteException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    @KafkaListener(
        topics = "${app.kafka.consumer.product.topic}",
        containerFactory = "batchKafkaListenerContainerFactoryProduct"
    )
    public void consume(List<ProductUpdateEvent> productUpdateEventList, Acknowledgment ack) {
        if (ObjectUtils.isEmpty(productUpdateEventList)) {
            log.warn("Received empty product update productUpdateEventList; acking.");
            ack.acknowledge();
            return;
        }
        try {
            productService.proceedProductUpdateBatchEvent(productUpdateEventList);
        } catch (ProductDocumentMongoWriteException productDocumentMongoWriteException) {
            // Handle known business exception locally to prevent container-level retries
            Map<String, ProductUpdateEvent> retryableEventMap = productDocumentMongoWriteException.getFailedEvents()
                    .stream()
                    .collect(Collectors.toMap(ProductUpdateEvent::name, event -> event));

            Map<String, ProductUpdateEvent> nonRetryableEventMap = productDocumentMongoWriteException.getCriteriaNotMatchedEvents()
                    .stream()
                    .collect(Collectors.toMap(ProductUpdateEvent::name, event -> event));

            log.error("Recovering from CustomMongoWriteException; with retryable event size={}, non-retryable event size={}",
                    ObjectUtils.isEmpty(retryableEventMap) ? 0 : retryableEventMap.size(),
                    ObjectUtils.isEmpty(nonRetryableEventMap) ? 0 : nonRetryableEventMap.size(),
                    productDocumentMongoWriteException);

            kafkaProducerService.sendBatch(retryTopic, retryableEventMap);
            kafkaProducerService.sendBatch(errorTopic, nonRetryableEventMap);
        } finally {
            ack.acknowledge();
        }
    }

    @Recover
    public void recover(Exception exception, List<ProductUpdateEvent> eventList, Acknowledgment ack) {
        log.error("Recovering from Exception; events size={}",
                ObjectUtils.isEmpty(eventList) ? 0 : eventList.size(), exception);
        kafkaProducerService.sendBatch(errorTopic, eventList.stream()
                .collect(Collectors.toMap(ProductUpdateEvent::name, event -> event)));
        ack.acknowledge();
    }

}
