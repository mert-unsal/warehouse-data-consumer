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
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Mock
    com.mongodb.client.FindIterable<Document> findIterable;

    @InjectMocks
    ProductService productService;

    String collectionName;

    @BeforeEach
    void setUp() {
        collectionName = "productDocument";
        lenient().when(mongoTemplate.getCollectionName(ProductDocument.class)).thenReturn(collectionName);
        lenient().when(mongoTemplate.getCollection(collectionName)).thenReturn(collection);
        // Default stubs for find() chain used by version prefetch and batch prefetch
        lenient().when(collection.find(any(Bson.class))).thenReturn(findIterable);
        lenient().when(findIterable.projection(any())).thenReturn(findIterable);
        // By default, no existing doc
        lenient().when(findIterable.first()).thenReturn(null);
        // For batch prefetch, do nothing on forEach
        lenient().doAnswer(inv -> null).when(findIterable).forEach(any());
    }

    @Test
    void proceedProductUpdateEvent_shouldCallUpdateOneWithUpsert_andIncludeVersionOps() {
        ProductUpdateEvent event = new ProductUpdateEvent("chair", List.of(), Instant.parse("2024-01-01T00:00:00Z"));

        // no exception expected
        productService.proceedProductUpdateEvent(event);

        // verify upsert
        org.mockito.ArgumentCaptor<Bson> updateCaptor = org.mockito.ArgumentCaptor.forClass(Bson.class);
        verify(collection, times(1)).updateOne(any(Bson.class), updateCaptor.capture(), argThat(opt -> opt.isUpsert()));

        // verify version operators
        Bson update = updateCaptor.getValue();
        org.bson.BsonDocument bson = update.toBsonDocument(Document.class, com.mongodb.MongoClientSettings.getDefaultCodecRegistry());
        org.bson.BsonDocument inc = bson.getDocument("$inc");
        assertNotNull(inc);
        assertEquals(new org.bson.BsonInt64(1L), inc.get("version"));
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

    @Test
    void proceedProductUpdateEvent_shouldThrowOptimisticLockWhenVersionMismatch() {
        // Existing doc with version 3
        org.bson.Document existing = new org.bson.Document("name", "chair").append("version", 3L);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.projection(any())).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(existing);

        // Simulate update result with no match and no upsert -> conflict
        com.mongodb.client.result.UpdateResult updateResult = com.mongodb.client.result.UpdateResult.acknowledged(0L, 0L, null);
        when(collection.updateOne(any(Bson.class), any(Bson.class), any(com.mongodb.client.model.UpdateOptions.class))).thenReturn(updateResult);

        ProductUpdateEvent event = new ProductUpdateEvent("chair", List.of(), Instant.parse("2024-01-03T00:00:00Z"));
        assertThrows(org.springframework.dao.OptimisticLockingFailureException.class,
                () -> productService.proceedProductUpdateEvent(event));
    }
}
