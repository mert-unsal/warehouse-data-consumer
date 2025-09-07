package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final MongoTemplate mongoTemplate;

    @Value("${app.kafka.retry.attempts:3}")
    private int retryAttempts;

    @Value("${app.kafka.retry.backoff-delay:1000}")
    private long backoffDelay;

    public void persistEventList(List<ProductUpdateEvent> productUpdateEventList) {

    }
}
