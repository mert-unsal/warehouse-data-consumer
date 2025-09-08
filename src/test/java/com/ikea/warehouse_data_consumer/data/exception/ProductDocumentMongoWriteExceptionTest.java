package com.ikea.warehouse_data_consumer.data.exception;

import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static com.ikea.warehouse_data_consumer.util.ExceptionMessageUtil.MONGO_WRITE_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductDocumentMongoWriteExceptionTest {

    @Test
    void gettersAndStatusShouldBeSet() {
        ProductUpdateEvent e1 = new ProductUpdateEvent("a", List.of(), Instant.now());
        ProductUpdateEvent e2 = new ProductUpdateEvent("b", List.of(), Instant.now());

        ProductDocumentMongoWriteException ex = new ProductDocumentMongoWriteException(List.of(e1), List.of(e2));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(MONGO_WRITE_EXCEPTION, ex.getCode());
        assertEquals(List.of(e1), ex.getFailedEvents());
        assertEquals(List.of(e2), ex.getCriteriaNotMatchedEvents());
    }
}
