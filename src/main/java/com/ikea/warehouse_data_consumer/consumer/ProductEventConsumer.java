package com.ikea.warehouse_data_consumer.consumer;

import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import com.ikea.warehouse_data_consumer.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final ProductService productService;

    @KafkaListener(
        topics = "${KAFKA_TOPIC_PRODUCT:ikea.warehouse.product.update.topic}",
        containerFactory = "kafkaListenerContainerFactoryProduct"
    )
    public void consume(ProductUpdateEvent event, Acknowledgment ack) {
        if (event == null || event.productDocuments() == null || event.productDocuments().isEmpty()) {
            log.warn("Received empty product update event; acking.");
            ack.acknowledge();
            return;
        }

        try {
            productService.process(event);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process product update event; will not ack to allow retry", ex);
            // no ack for retry
        }
    }
}
