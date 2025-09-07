package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;
import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.CustomMongoWriteException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final MongoTemplate mongoTemplate;
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    public void process(List<InventoryUpdateEvent> eventList) {

        List<WriteModel<Document>> bulkOperations = new ArrayList<>();
        List<InventoryUpdateEvent> failedEvents = new ArrayList<>();

        try {
            if (ObjectUtils.isEmpty(eventList)) {
                log.warn("Received empty inventory update event list; skipping processing.");
                return;
            }

            for (InventoryUpdateEvent event : eventList) {
                Bson updates = Updates.combine(
                        Updates.set("name", event.name()),
                        Updates.set("stock", event.stock()),
                        Updates.set("fileCreatedAt", event.fileCreatedAt())
                );

                bulkOperations.add(new UpdateOneModel<>(
                        Filters.and(Filters.eq("_id", event.artId()), Filters.lt("fileCreatedAt", event.fileCreatedAt())),
                        updates,
                        new UpdateOptions()
                ));
            }
            mongoTemplate.getCollection(mongoTemplate.getCollectionName(ArticleDocument.class)).bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));
        } catch (MongoBulkWriteException mongoBulkWriteException) {
            for (BulkWriteError bulkWriteError : mongoBulkWriteException.getWriteErrors()) {
                int index = bulkWriteError.getIndex();
                InventoryUpdateEvent event = eventList.get(index);
                failedEvents.add(event);
            }
            throw new CustomMongoWriteException(failedEvents);
        }
    }
}
