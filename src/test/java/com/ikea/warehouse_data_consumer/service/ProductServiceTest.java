package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ProductDocument;
import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.ProductDocumentMongoWriteException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.UpdateOneModel;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Mock
    MongoCollection<Document> collection;

    @InjectMocks
    ProductService productService;

    String collectionName;

    @BeforeEach
    void setUp() {
        collectionName = "productDocument";
        lenient().when(mongoTemplate.getCollectionName(ProductDocument.class)).thenReturn(collectionName);
        lenient().when(mongoTemplate.getCollection(collectionName)).thenReturn(collection);
        // Single updateOne should also use ProductDocument collection
    }

    @Test
    void proceedProductUpdateEvent_shouldCallUpdateOneWithUpsert() {
        ProductUpdateEvent event = new ProductUpdateEvent("chair", List.of(), Instant.parse("2024-01-01T00:00:00Z"));

        // no exception expected
        productService.proceedProductUpdateEvent(event);

        verify(collection, times(1)).updateOne(any(Bson.class), any(Bson.class), argThat(opt -> opt.isUpsert()));
    }

    @Test
    void proceedProductUpdateBatchEvent_shouldReturnWhenListEmpty() {
        assertDoesNotThrow(() -> productService.proceedProductUpdateBatchEvent(List.of()));
        verify(collection, never()).bulkWrite(anyList(), any(BulkWriteOptions.class));
    }

    @Test
    void proceedProductUpdateBatchEvent_shouldThrowWhenNotMatchedCriteriaExists() {
        ProductUpdateEvent e1 = new ProductUpdateEvent("a", List.of(), Instant.parse("2024-01-01T00:00:00Z"));
        ProductUpdateEvent e2 = new ProductUpdateEvent("b", List.of(), Instant.parse("2024-01-02T00:00:00Z"));

        // Simulate bulk result: matchedCount 1 (only one matched), and upsert for index 0 -> means index 1 not matched
        BulkWriteResult result = mock(BulkWriteResult.class);
        when(result.getUpserts()).thenReturn(List.of(new BulkWriteUpsert(0, null)));
        when(result.getMatchedCount()).thenReturn(1);

        when(collection.bulkWrite(anyList(), any(BulkWriteOptions.class))).thenReturn(result);

        ProductDocumentMongoWriteException ex = assertThrows(ProductDocumentMongoWriteException.class,
                () -> productService.proceedProductUpdateBatchEvent(List.of(e1, e2)));
        assertTrue(ex.getCriteriaNotMatchedEvents().contains(e2));
    }

    @Test
    void proceedProductUpdateBatchEvent_shouldMapBulkWriteException() {
        ProductUpdateEvent e1 = new ProductUpdateEvent("a", List.of(), Instant.parse("2024-01-01T00:00:00Z"));
        ProductUpdateEvent e2 = new ProductUpdateEvent("b", List.of(), Instant.parse("2024-01-02T00:00:00Z"));

        // mock BulkWriteException with write result and one error at index 1
        BulkWriteResult result = mock(BulkWriteResult.class);
        when(result.getUpserts()).thenReturn(List.of());
        when(result.getMatchedCount()).thenReturn(0);

        org.bson.BsonDocument details = new org.bson.BsonDocument();
        BulkWriteError writeError = new BulkWriteError(11000, "dup", details, 1);
        com.mongodb.ServerAddress serverAddress = new com.mongodb.ServerAddress();
        com.mongodb.bulk.WriteConcernError wcError = null;
        MongoBulkWriteException bulkEx = new MongoBulkWriteException(result, List.of(writeError), wcError, serverAddress);

        when(collection.bulkWrite(anyList(), any(BulkWriteOptions.class))).thenThrow(bulkEx);

        ProductDocumentMongoWriteException ex = assertThrows(ProductDocumentMongoWriteException.class,
                () -> productService.proceedProductUpdateBatchEvent(List.of(e1, e2)));
        assertEquals(List.of(e2), ex.getFailedEvents());
        assertTrue(ex.getCriteriaNotMatchedEvents().containsAll(List.of(e1, e2)));
    }
}
