package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ProductDocument;
import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final MongoTemplate mongoTemplate;

    @Value("${app.kafka.retry.attempts:3}")
    private int retryAttempts;

    @Value("${app.kafka.retry.backoff-delay:1000}")
    private long backoffDelay;

    @Transactional
    @Retryable(maxAttemptsExpression = "#{${app.kafka.retry.attempts:3}}",
            backoff = @Backoff(delayExpression = "#{${app.kafka.retry.backoff-delay:1000}}", multiplier = 2.0))
    public void process(ProductUpdateEvent event) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ProductDocument.class);
        List<ProductDocument> productDocuments = event.productDocuments();
        for (ProductDocument productDocument : productDocuments) {
            Query query = Query.query(Criteria.where("_id").is(productDocument.id()));
            Update update = new Update()
                .set("name", productDocument.name())
                .set("containArticles", productDocument.containArticles());

            if (productDocument.version() == null) {
                update.setOnInsert("_id", productDocument.id());
                update.setOnInsert("version", 0L);
                bulkOps.upsert(query, update);
            } else {
                query.addCriteria(Criteria.where("version").is(productDocument.version()));
                update.inc("version", 1);
                bulkOps.updateOne(query, update);
            }
        }
        bulkOps.execute();
        log.info("Persisted {} products with timestamp {}", event.productDocuments().size(), event.timestamp());
    }
}
