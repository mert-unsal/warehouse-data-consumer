package com.ikea.warehouse_data_consumer.builder;

import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import com.mongodb.client.model.*;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds MongoDB bulk write operations for Product updates.
 * Extracting this logic makes it easier to unit test the translation from events to write models.
 */
public class ProductBulkOperationBuilder {

    /**
     * Convert a list of ProductUpdateEvent into UpdateOneModel operations with upsert enabled.
     */
    public List<WriteModel<Document>> build(List<ProductUpdateEvent> events) {
        List<WriteModel<Document>> bulkOperations = new ArrayList<>();
        if (ObjectUtils.isEmpty(events)) {
            return bulkOperations;
        }
        for (ProductUpdateEvent event : events) {
            Bson updates = Updates.combine(
                    Updates.set("name", event.name()),
                    Updates.set("containArticles", event.containArticles()),
                    Updates.set("fileCreatedAt", event.fileCreatedAt())
            );

            bulkOperations.add(new UpdateOneModel<>(
                    Filters.and(Filters.eq("name", event.name()),
                                Filters.lt("fileCreatedAt", event.fileCreatedAt())),
                    updates,
                    new UpdateOptions().upsert(true)
            ));
        }
        return bulkOperations;
    }
}
