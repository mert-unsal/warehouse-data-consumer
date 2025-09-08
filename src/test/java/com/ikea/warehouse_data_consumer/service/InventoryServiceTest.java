package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;
import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_consumer.data.exception.ArticleDocumentMongoWriteException;
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
class InventoryServiceTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Mock
    MongoCollection<Document> collection;

    @InjectMocks
    InventoryService inventoryService;

    String collectionName;

    @BeforeEach
    void setUp() {
        collectionName = "articleDocument";
        lenient().when(mongoTemplate.getCollectionName(ArticleDocument.class)).thenReturn(collectionName);
        lenient().when(mongoTemplate.getCollection(collectionName)).thenReturn(collection);
    }

    @Test
    void proceedInventoryUpdateEvent_shouldCallUpdateOneWithUpsert() {
        InventoryUpdateEvent event = new InventoryUpdateEvent("1", "art", "10", Instant.parse("2024-01-01T00:00:00Z"));

        inventoryService.proceedInventoryUpdateEvent(event);

        verify(collection, times(1)).updateOne(any(Bson.class), any(Bson.class), argThat(opt -> opt.isUpsert()));
    }

    @Test
    void proceedInventoryUpdateBatchEvent_shouldReturnWhenListEmpty() {
        assertDoesNotThrow(() -> inventoryService.proceedInventoryUpdateBatchEvent(List.of()));
        verify(collection, never()).bulkWrite(anyList(), any(BulkWriteOptions.class));
    }

    @Test
    void proceedInventoryUpdateBatchEvent_shouldThrowWhenNotMatchedCriteriaExists() {
        InventoryUpdateEvent e1 = new InventoryUpdateEvent("1", "a", "1", Instant.parse("2024-01-01T00:00:00Z"));
        InventoryUpdateEvent e2 = new InventoryUpdateEvent("2", "b", "2", Instant.parse("2024-01-02T00:00:00Z"));

        BulkWriteResult result = mock(BulkWriteResult.class);
        when(result.getUpserts()).thenReturn(List.of(new BulkWriteUpsert(0, null)));
        when(result.getMatchedCount()).thenReturn(1);

        when(collection.bulkWrite(anyList(), any(BulkWriteOptions.class))).thenReturn(result);

        ArticleDocumentMongoWriteException ex = assertThrows(ArticleDocumentMongoWriteException.class,
                () -> inventoryService.proceedInventoryUpdateBatchEvent(List.of(e1, e2)));
        assertTrue(ex.getCriteriaNotMatchedEvents().contains(e2));
    }

    @Test
    void proceedInventoryUpdateBatchEvent_shouldMapBulkWriteException() {
        InventoryUpdateEvent e1 = new InventoryUpdateEvent("1", "a", "1", Instant.parse("2024-01-01T00:00:00Z"));
        InventoryUpdateEvent e2 = new InventoryUpdateEvent("2", "b", "2", Instant.parse("2024-01-02T00:00:00Z"));

        BulkWriteResult result = mock(BulkWriteResult.class);
        when(result.getUpserts()).thenReturn(List.of());
        when(result.getMatchedCount()).thenReturn(0);

        org.bson.BsonDocument details = new org.bson.BsonDocument();
        BulkWriteError writeError = new BulkWriteError(11000, "dup", details, 1);
        com.mongodb.ServerAddress serverAddress = new com.mongodb.ServerAddress();
        com.mongodb.bulk.WriteConcernError wcError = null;
        MongoBulkWriteException bulkEx = new MongoBulkWriteException(result, List.of(writeError), wcError, serverAddress);

        when(collection.bulkWrite(anyList(), any(BulkWriteOptions.class))).thenThrow(bulkEx);

        ArticleDocumentMongoWriteException ex = assertThrows(ArticleDocumentMongoWriteException.class,
                () -> inventoryService.proceedInventoryUpdateBatchEvent(List.of(e1, e2)));
        assertEquals(List.of(e2), ex.getFailedEvents());
        assertTrue(ex.getCriteriaNotMatchedEvents().containsAll(List.of(e1, e2)));
    }
}
