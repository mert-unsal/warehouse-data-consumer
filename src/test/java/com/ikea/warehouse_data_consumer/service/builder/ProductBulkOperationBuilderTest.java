package com.ikea.warehouse_data_consumer.service.builder;

import com.ikea.warehouse_data_consumer.data.dto.ArticleAmount;
import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductBulkOperationBuilderTest {

    @Test
    void build_shouldReturnEmptyList_whenInputEmpty() {
        ProductBulkOperationBuilder builder = new ProductBulkOperationBuilder();
        List<WriteModel<Document>> models = builder.build(List.of());
        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void build_shouldCreateUpdateOneModelsWithUpsertAndCorrectFilterAndUpdate() {
        ProductBulkOperationBuilder builder = new ProductBulkOperationBuilder();
        ProductUpdateEvent e1 = new ProductUpdateEvent("p1", List.of(new ArticleAmount("1", 2L)), Instant.parse("2024-01-01T00:00:00Z"));
        ProductUpdateEvent e2 = new ProductUpdateEvent("p2", List.of(new ArticleAmount("2", 3L)), Instant.parse("2024-01-02T00:00:00Z"));

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
