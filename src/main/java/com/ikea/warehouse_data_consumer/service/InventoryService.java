package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;
import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.ArticleDocumentMongoWriteException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.ikea.warehouse_data_consumer.util.MongoBulkUtil.getNotMatchedCriteria;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final MongoTemplate mongoTemplate;

    public void proceedInventoryUpdateEvent(InventoryUpdateEvent event) {

        MongoCollection<Document> collection = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ArticleDocument.class));

        // Read the current version (server-side expected version)
        Document existing = collection
                .find(Filters.eq("_id", event.artId()))
                .projection(Projections.include("version"))
                .first();

        Bson baseFilter = Filters.and(
                Filters.eq("_id", event.artId()),
                Filters.lt("fileCreatedAt", event.fileCreatedAt())
        );

        Bson versionFilter = (existing != null && existing.get("version") != null)
                ? Filters.eq("version", existing.getLong("version"))
                : null;

        Bson filter = (versionFilter != null) ? Filters.and(baseFilter, versionFilter) : baseFilter;

        Bson update = Updates.combine(
                Updates.set("name", event.name()),
                Updates.set("stock", event.stock()),
                Updates.set("fileCreatedAt", event.fileCreatedAt()),
                Updates.inc("version", 1L)
        );

        // Upsert only if the document does not exist
        UpdateOptions options = new UpdateOptions().upsert(Objects.isNull(existing));

        UpdateResult result = collection.updateOne(filter, update, options);

        // If a document existed (we had a version expectation), but no match happened and no upsert occurred => optimistic conflict
        if (versionFilter != null && result.getMatchedCount() == 0 && result.getUpsertedId() == null) {
            throw new OptimisticLockingFailureException("Optimistic lock conflict for articleId=" + event.artId());
        }


    }

    public void proceedInventoryUpdateBatchEvent(List<InventoryUpdateEvent> eventList) {

        List<WriteModel<Document>> bulkOperations;
        List<InventoryUpdateEvent> failedEvents = new ArrayList<>();

        try {
            if (ObjectUtils.isEmpty(eventList)) {
                log.warn("Received empty inventory update event list; skipping processing.");
                return;
            }

            MongoCollection<Document> collection = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ArticleDocument.class));

            Set<String> ids = eventList.stream().map(InventoryUpdateEvent::artId).collect(Collectors.toSet());

            Map<String, Long> currentVersions = new HashMap<>();
            collection.find(Filters.in("_id", ids))
                    .projection(Projections.include("_id", "version"))
                    .forEach(doc -> currentVersions.put(doc.getString("_id"), doc.getLong("version")));

            bulkOperations = new ArrayList<>(eventList.size());
            for (InventoryUpdateEvent inventoryUpdateEvent : eventList) {
                Bson baseFilter = Filters.and(
                        Filters.eq("_id", inventoryUpdateEvent.artId()),
                        Filters.lt("fileCreatedAt", inventoryUpdateEvent.fileCreatedAt())
                );

                Long curVer = currentVersions.get(inventoryUpdateEvent.artId());
                Bson filter = (curVer != null)
                        ? Filters.and(baseFilter, Filters.eq("version", curVer))
                        : baseFilter;

                Bson update = Updates.combine(
                        Updates.set("name", inventoryUpdateEvent.name()),
                        Updates.set("stock", inventoryUpdateEvent.stock()),
                        Updates.set("fileCreatedAt", inventoryUpdateEvent.fileCreatedAt()),
                        Updates.inc("version", 1L)
                );

                bulkOperations.add(new UpdateOneModel<>(filter, update, new UpdateOptions().upsert(true)));
            }

            BulkWriteResult bulkWriteResult = collection.bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));

            List<InventoryUpdateEvent> notMatchedEvents = getNotMatchedCriteria(eventList, bulkWriteResult);
            if (ObjectUtils.isNotEmpty(notMatchedEvents)) {
                // These include optimistic conflicts (version mismatch) and fileCreatedAt guard rejections.
                throw new ArticleDocumentMongoWriteException(List.of(), notMatchedEvents);
            }
        } catch (MongoBulkWriteException mongoBulkWriteException) {
            List<InventoryUpdateEvent> notMatchedCriteria = getNotMatchedCriteria(eventList,
                    mongoBulkWriteException.getWriteResult());
            for (BulkWriteError bulkWriteError : mongoBulkWriteException.getWriteErrors()) {
                int index = bulkWriteError.getIndex();
                InventoryUpdateEvent event = eventList.get(index);
                failedEvents.add(event);
            }
            throw new ArticleDocumentMongoWriteException(failedEvents, notMatchedCriteria);
        }

    }

}
