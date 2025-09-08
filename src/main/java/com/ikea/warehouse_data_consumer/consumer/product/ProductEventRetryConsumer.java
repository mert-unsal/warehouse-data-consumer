package com.ikea.warehouse_data_consumer.consumer.product;

import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import com.ikea.warehouse_data_consumer.service.KafkaProducerService;
import com.ikea.warehouse_data_consumer.service.ProductService;
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
public class ProductEventRetryConsumer {

    private final ProductService productService;
    private final KafkaProducerService kafkaProducerService;

    @Value("${app.kafka.consumer.product.errorTopic}")
    private String errorTopic;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    @KafkaListener(
            topics = "${app.kafka.consumer.product.retryTopic}",
            containerFactory = "kafkaListenerContainerFactoryProduct"
    )
    public void consume(ProductUpdateEvent productUpdateEvent, Acknowledgment ack) {
        productService.proceedProductUpdateEvent(productUpdateEvent);
        ack.acknowledge();
    }

    @Recover
    public void recover(Exception exception, ProductUpdateEvent productUpdateEvent, Acknowledgment ack) {
        log.error("ProductEventRetryConsumer recover exception", exception);
        kafkaProducerService.send(errorTopic, productUpdateEvent.name(), productUpdateEvent);
        ack.acknowledge();
    }
}

