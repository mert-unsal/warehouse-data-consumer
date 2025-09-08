package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;
import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.ArticleDocumentMongoWriteException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final MongoTemplate mongoTemplate;

    public void proceedInventoryUpdateEvent(InventoryUpdateEvent event) {

        Bson filter = Filters.and(
                Filters.eq("_id", event.artId()),
                Filters.lt("fileCreatedAt", event.fileCreatedAt())
        );

        Bson update = Updates.combine(
                Updates.set("name", event.name()),
                Updates.set("stock", event.stock()),
                Updates.set("fileCreatedAt", event.fileCreatedAt())
        );

        UpdateOptions options = new UpdateOptions().upsert(true);

        mongoTemplate.getCollection(mongoTemplate.getCollectionName(ArticleDocument.class)).updateOne(filter, update, options);

    }

    public void proceedInventoryUpdateBatchEvent(List<InventoryUpdateEvent> eventList) {

        List<WriteModel<Document>> bulkOperations = new ArrayList<>();
        List<InventoryUpdateEvent> failedEvents = new ArrayList<>();

        try {
            if (ObjectUtils.isEmpty(eventList)) {
                log.warn("Received empty inventory update event list; skipping processing.");
                return;
            }

            // Delegate building of bulk operations to a dedicated builder for testability
            bulkOperations = new com.ikea.warehouse_data_consumer.service.builder.InventoryBulkOperationBuilder().build(eventList);

            BulkWriteResult bulkWriteResult = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ArticleDocument.class)).bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));

            List<InventoryUpdateEvent> notMatchedEvents = com.ikea.warehouse_data_consumer.util.MongoBulkUtil.getNotMatchedCriteria(eventList, bulkWriteResult);
            if (ObjectUtils.isNotEmpty(notMatchedEvents)) {
                throw new ArticleDocumentMongoWriteException(List.of(), notMatchedEvents);
            }
        } catch (MongoBulkWriteException mongoBulkWriteException) {
            List<InventoryUpdateEvent> notMatchedCriteria = com.ikea.warehouse_data_consumer.util.MongoBulkUtil.getNotMatchedCriteria(eventList, mongoBulkWriteException.getWriteResult());
            for (BulkWriteError bulkWriteError : mongoBulkWriteException.getWriteErrors()) {
                int index = bulkWriteError.getIndex();
                InventoryUpdateEvent event = eventList.get(index);
                failedEvents.add(event);
            }
            throw new ArticleDocumentMongoWriteException(failedEvents, notMatchedCriteria);
        }
    }

}
