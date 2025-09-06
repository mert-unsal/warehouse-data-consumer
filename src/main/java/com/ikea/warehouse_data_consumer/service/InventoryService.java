package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;
import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
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
public class InventoryService {

    private final MongoTemplate mongoTemplate;

    @Value("${app.kafka.retry.attempts:3}")
    private int retryAttempts;

    @Value("${app.kafka.retry.backoff-delay:1000}")
    private long backoffDelay;

    @Transactional
    @Retryable(maxAttemptsExpression = "#{${app.kafka.retry.attempts:3}}",
            backoff = @Backoff(delayExpression = "#{${app.kafka.retry.backoff-delay:1000}}", multiplier = 2.0))
    public void process(InventoryUpdateEvent event) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ArticleDocument.class);
        List<ArticleDocument> items = event.inventory();
        for (ArticleDocument item : items) {
            Query query = Query.query(Criteria.where("_id").is(item.id()));
            if (item.lastMessageId() != null && !item.lastMessageId().isBlank()) {
                query.addCriteria(new Criteria().orOperator(
                    Criteria.where("lastMessageId").ne(item.lastMessageId()),
                    Criteria.where("lastMessageId").exists(false)
                ));
            }

            Update update = new Update()
                .set("name", item.name())
                .set("stock", item.stock());

            if (item.lastMessageId() != null && !item.lastMessageId().isBlank()) {
                update.set("lastMessageId", item.lastMessageId());
            }

            if (item.version() == null) {
                update.setOnInsert("_id", item.id());
                update.setOnInsert("version", 0L);
                bulkOps.upsert(query, update);
            } else {
                query.addCriteria(Criteria.where("version").is(item.version()));
                update.inc("version", 1);
                bulkOps.updateOne(query, update);
            }
        }
        bulkOps.execute();
        log.info("Persisted {} inventory items with timestamp {}", event.inventory().size(), event.timestamp());
    }
}
