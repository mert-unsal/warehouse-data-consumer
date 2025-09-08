package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ProductDocument;
import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.ProductDocumentMongoWriteException;
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

import static com.ikea.warehouse_data_consumer.util.MongoBulkUtil.getNotMatchedCriteria;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final MongoTemplate mongoTemplate;

    public void proceedProductUpdateEvent(ProductUpdateEvent productUpdateEvent) {

        Bson filter = Filters.and(
                Filters.eq("name", productUpdateEvent.name()),
                Filters.lt("fileCreatedAt", productUpdateEvent.fileCreatedAt())
        );

        Bson update = Updates.combine(
                Updates.set("name", productUpdateEvent.name()),
                Updates.set("containArticles", productUpdateEvent.containArticles()),
                Updates.set("fileCreatedAt", productUpdateEvent.fileCreatedAt())
        );

        UpdateOptions options = new UpdateOptions().upsert(true);

        mongoTemplate.getCollection(mongoTemplate.getCollectionName(ProductUpdateEvent.class))
                .updateOne(filter, update, options);

    }

    public void proceedProductUpdateBatchEvent(List<ProductUpdateEvent> productUpdateEventList) {
        List<WriteModel<Document>> bulkOperations = new ArrayList<>();
        List<ProductUpdateEvent> failedEvents = new ArrayList<>();

        try {
            if (ObjectUtils.isEmpty(productUpdateEventList)) {
                log.warn("Received empty inventory update event list; skipping processing.");
                return;
            }

            for (ProductUpdateEvent productUpdateEvent : productUpdateEventList) {
                Bson updates = Updates.combine(
                        Updates.set("name", productUpdateEvent.name()),
                        Updates.set("containArticles", productUpdateEvent.containArticles()),
                        Updates.set("fileCreatedAt", productUpdateEvent.fileCreatedAt())
                );

                bulkOperations.add(new UpdateOneModel<>(
                        Filters.and(Filters.eq("name", productUpdateEvent.name()),
                                    Filters.lt("fileCreatedAt", productUpdateEvent.fileCreatedAt())),
                        updates,
                        new UpdateOptions().upsert(true)
                ));
            }

            BulkWriteResult bulkWriteResult = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ProductDocument.class))
                                                    .bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));

            List<ProductUpdateEvent> notMatchedEvents = getNotMatchedCriteria(productUpdateEventList, bulkWriteResult);
            if (ObjectUtils.isNotEmpty(notMatchedEvents)) {
                throw new ProductDocumentMongoWriteException(List.of(), notMatchedEvents);
            }
        } catch (MongoBulkWriteException mongoBulkWriteException) {
            List<ProductUpdateEvent> notMatchedCriteria = getNotMatchedCriteria(productUpdateEventList, mongoBulkWriteException.getWriteResult());
            for (BulkWriteError bulkWriteError : mongoBulkWriteException.getWriteErrors()) {
                int index = bulkWriteError.getIndex();
                ProductUpdateEvent event = productUpdateEventList.get(index);
                failedEvents.add(event);
            }
            throw new ProductDocumentMongoWriteException(failedEvents, notMatchedCriteria);
        }
    }


}
