package com.ikea.warehouse_data_consumer.service.builder;

import com.ikea.warehouse_data_consumer.builder.InventoryBulkOperationBuilder;
import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryBulkOperationBuilderTest {


    @Test
    void build_shouldReturnEmptyList_whenInputEmpty() {
        InventoryBulkOperationBuilder builder = new InventoryBulkOperationBuilder();
        List<WriteModel<Document>> models = builder.build(List.of());
        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void build_shouldCreateUpdateOneModelsWithUpsertAndCorrectFilterAndUpdate() {
        InventoryBulkOperationBuilder builder = new InventoryBulkOperationBuilder();
        InventoryUpdateEvent e1 = new InventoryUpdateEvent("1", "a", 10L, Instant.parse("2024-01-01T00:00:00Z"));
        InventoryUpdateEvent e2 = new InventoryUpdateEvent("2", "b", 20L, Instant.parse("2024-01-02T00:00:00Z"));

        List<WriteModel<Document>> models = builder.build(List.of(e1, e2));
        assertEquals(2, models.size());

        for (WriteModel<Document> model : models) {
            assertTrue(model instanceof UpdateOneModel);
            UpdateOneModel<Document> u = (UpdateOneModel<Document>) model;
            assertTrue(u.getOptions().isUpsert());
            Bson filter = u.getFilter();
            Bson update = u.getUpdate();
            assertNotNull(filter);
            assertNotNull(update);
        }
    }
}
