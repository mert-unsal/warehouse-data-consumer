package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;
import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.CustomMongoWriteException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final MongoTemplate mongoTemplate;
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    public void persistEvent(InventoryUpdateEvent event) {

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

    public void persistEventList(List<InventoryUpdateEvent> eventList) {

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
                        new UpdateOptions().upsert(true)
                ));
            }

            BulkWriteResult bulkWriteResult = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ArticleDocument.class)).bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));

            List<InventoryUpdateEvent> notMatchedEvents = getNotMatchedCriteria(eventList, bulkWriteResult);
            if (ObjectUtils.isNotEmpty(notMatchedEvents)) {
                throw new CustomMongoWriteException(List.of(), notMatchedEvents);
            }
        } catch (MongoBulkWriteException mongoBulkWriteException) {
            List<InventoryUpdateEvent> notMatchedCriteria = getNotMatchedCriteria(eventList, mongoBulkWriteException.getWriteResult());
            for (BulkWriteError bulkWriteError : mongoBulkWriteException.getWriteErrors()) {
                int index = bulkWriteError.getIndex();
                InventoryUpdateEvent event = eventList.get(index);
                failedEvents.add(event);
            }
            throw new CustomMongoWriteException(failedEvents, notMatchedCriteria);
        }
    }

    public List<InventoryUpdateEvent> getNotMatchedCriteria(List<InventoryUpdateEvent> eventList, BulkWriteResult bulkWriteResult) {
        List<InventoryUpdateEvent> notMatchedEvents = new ArrayList<>();
        Set<Integer> upsertedIndexList = bulkWriteResult.getUpserts().stream().map(BulkWriteUpsert::getIndex).collect(Collectors.toSet());

        if (ObjectUtils.notEqual(bulkWriteResult.getMatchedCount(), eventList.size())) {
            for (int i = 0; i < eventList.size(); i++) {
                if (!upsertedIndexList.contains(i)) {
                    notMatchedEvents.add(eventList.get(i));
                }
            }
        }
        return notMatchedEvents;
    }
}
