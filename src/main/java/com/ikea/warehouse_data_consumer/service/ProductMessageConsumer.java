package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ProductDocument;
import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMessageConsumer {

    private final MongoTemplate mongoTemplate;

    @Transactional
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

            ack.acknowledge();
            log.info("Persisted {} products with timestamp {}", event.productDocuments().size(), event.timestamp());
        } catch (Exception ex) {
            log.error("Failed to process product update event; will not ack to allow retry", ex);
            // no ack for retry
        }
    }
}
