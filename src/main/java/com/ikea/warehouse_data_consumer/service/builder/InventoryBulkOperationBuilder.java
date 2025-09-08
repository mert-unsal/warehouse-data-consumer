package com.ikea.warehouse_data_consumer.service.builder;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds MongoDB bulk write operations for Inventory updates.
 * Extracting this logic makes it easier to unit test the translation from events to write models.
 */
public class InventoryBulkOperationBuilder {

    /**
     * Convert a list of InventoryUpdateEvent into UpdateOneModel operations with upsert enabled.
     */
    public List<com.mongodb.client.model.WriteModel<Document>> build(List<InventoryUpdateEvent> eventList) {
        List<com.mongodb.client.model.WriteModel<Document>> bulkOperations = new ArrayList<>();
        if (ObjectUtils.isEmpty(eventList)) {
            return bulkOperations;
        }
        for (InventoryUpdateEvent event : eventList) {
            Bson updates = Updates.combine(
                    Updates.set("name", event.name()),
                    Updates.set("stock", event.stock()),
                    Updates.set("fileCreatedAt", event.fileCreatedAt())
            );

            bulkOperations.add(new UpdateOneModel<>(
                    Filters.and(Filters.eq("_id", event.artId()),
                                Filters.lt("fileCreatedAt", event.fileCreatedAt())),
                    updates,
                    new UpdateOptions().upsert(true)
            ));
        }
        return bulkOperations;
    }
}
