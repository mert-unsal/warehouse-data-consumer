package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;
import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
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
public class InventoryMessageConsumer {

    private final MongoTemplate mongoTemplate;

    @Transactional
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

        try {
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
                    // New insert or first write: upsert and initialize version to 0 on insert
                    update.setOnInsert("_id", item.id());
                    update.setOnInsert("version", 0L);
                    bulkOps.upsert(query, update);
                } else {
                    // OCC: match current version and increment
                    query.addCriteria(Criteria.where("version").is(item.version()));
                    update.inc("version", 1);
                    bulkOps.updateOne(query, update);
                }
            }
            bulkOps.execute();

            ack.acknowledge();
            log.info("Persisted {} inventory items with timestamp {}", event.inventory().size(), event.timestamp());
        } catch (Exception ex) {
            log.error("Failed to process inventory update event; will not ack to allow retry", ex);
            // Do not ack; message will be retried depending on container config
        }
    }
}
