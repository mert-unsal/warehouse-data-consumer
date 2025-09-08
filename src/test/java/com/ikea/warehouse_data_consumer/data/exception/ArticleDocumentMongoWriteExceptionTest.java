package com.ikea.warehouse_data_consumer.data.exception;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static com.ikea.warehouse_data_consumer.util.ExceptionMessageUtil.MONGO_WRITE_EXCEPTION;
import static org.junit.jupiter.api.Assertions.*;

class ArticleDocumentMongoWriteExceptionTest {

    @Test
    void gettersAndStatusShouldBeSet() {
        InventoryUpdateEvent e1 = new InventoryUpdateEvent("1", "a", "1", Instant.now());
        InventoryUpdateEvent e2 = new InventoryUpdateEvent("2", "b", "2", Instant.now());

        ArticleDocumentMongoWriteException ex = new ArticleDocumentMongoWriteException(List.of(e1), List.of(e2));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(MONGO_WRITE_EXCEPTION, ex.getCode());
        assertEquals(List.of(e1), ex.getFailedEvents());
        assertEquals(List.of(e2), ex.getCriteriaNotMatchedEvents());
    }
}
