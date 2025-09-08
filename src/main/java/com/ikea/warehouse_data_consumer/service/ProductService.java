package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ProductDocument;
import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.ProductDocumentMongoWriteException;
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
public class ProductService {

    private final MongoTemplate mongoTemplate;

    public void proceedProductUpdateEvent(ProductUpdateEvent event) {
        MongoCollection<Document> collection = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ProductDocument.class));

        // Read current version (server-side)
        Document existing = collection
                .find(Filters.eq("name", event.name()))
                .projection(Projections.include("version", "fileCreatedAt"))
                .first();

        Bson baseFilter = Filters.and(
                Filters.eq("name", event.name()),
                Filters.lt("fileCreatedAt", event.fileCreatedAt())
        );

        Bson versionFilter = (existing != null && existing.get("version") != null)
                ? Filters.eq("version", existing.getLong("version"))
                : null;

        Bson filter = (versionFilter != null) ? Filters.and(baseFilter, versionFilter) : baseFilter;

        Bson update = Updates.combine(
                Updates.set("name", event.name()),
                Updates.set("containArticles", event.containArticles()),
                Updates.set("fileCreatedAt", event.fileCreatedAt()),
                Updates.inc("version", 1L)
        );

        UpdateOptions options = new UpdateOptions().upsert(Objects.isNull(existing));
        UpdateResult result = collection.updateOne(filter, update, options);

        if (Objects.nonNull(versionFilter) && result.getMatchedCount() == 0 && Objects.isNull(result.getUpsertedId())) {
            throw new OptimisticLockingFailureException(STR."Optimistic lock conflict for product name=\{event.name()}");
        }
    }

    public void proceedProductUpdateBatchEvent(List<ProductUpdateEvent> eventList) {
        List<WriteModel<Document>> bulkOperations;
        List<ProductUpdateEvent> failedEvents = new ArrayList<>();

        try {
            if (ObjectUtils.isEmpty(eventList)) {
                log.warn("Received empty inventory update event list; skipping processing.");
                return;
            }

            MongoCollection<Document> collection = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ProductDocument.class));

            Set<String> names = eventList.stream().map(ProductUpdateEvent::name).collect(Collectors.toSet());
            Map<String, Long> currentVersions = new HashMap<>();
            collection.find(Filters.in("name", names))
                    .projection(Projections.include("name", "version"))
                    .forEach(doc -> currentVersions.put(doc.getString("name"), doc.getLong("version")));

            bulkOperations = new ArrayList<>(eventList.size());
            for (ProductUpdateEvent e : eventList) {
                Bson baseFilter = Filters.and(
                        Filters.eq("name", e.name()),
                        Filters.lt("fileCreatedAt", e.fileCreatedAt())
                );
                Long cv = currentVersions.get(e.name());
                Bson filter = (cv != null) ? Filters.and(baseFilter, Filters.eq("version", cv)) : baseFilter;

                Bson update = Updates.combine(
                        Updates.set("name", e.name()),
                        Updates.set("containArticles", e.containArticles()),
                        Updates.set("fileCreatedAt", e.fileCreatedAt()),
                        Updates.inc("version", 1L)
                );

                bulkOperations.add(new UpdateOneModel<>(filter, update, new UpdateOptions().upsert(true)));
            }

            BulkWriteResult bulkWriteResult = collection.bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));

            List<ProductUpdateEvent> notMatchedEvents = getNotMatchedCriteria(eventList, bulkWriteResult);
            if (ObjectUtils.isNotEmpty(notMatchedEvents)) {
                // These include optimistic conflicts (version mismatch) and fileCreatedAt guard rejections.
                throw new ProductDocumentMongoWriteException(List.of(), notMatchedEvents);
            }
        } catch (MongoBulkWriteException mongoBulkWriteException) {
            List<ProductUpdateEvent> notMatchedCriteria = getNotMatchedCriteria(eventList,
                    mongoBulkWriteException.getWriteResult());
            for (BulkWriteError bulkWriteError : mongoBulkWriteException.getWriteErrors()) {
                int index = bulkWriteError.getIndex();
                ProductUpdateEvent event = eventList.get(index);
                failedEvents.add(event);
            }
            throw new ProductDocumentMongoWriteException(failedEvents, notMatchedCriteria);
        }
    }
}
