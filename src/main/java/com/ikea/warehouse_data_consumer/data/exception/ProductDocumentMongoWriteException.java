package com.ikea.warehouse_data_consumer.data.exception;

import com.ikea.warehouse_data_consumer.data.event.ProductUpdateEvent;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com.ikea.warehouse_data_consumer.util.ExceptionMessageUtil.MONGO_WRITE_EXCEPTION;


/**
 * Custom exception for file processing errors in the warehouse data ingestion service.
 * This exception will be caught by the GlobalExceptionHandler and converted to a proper ErrorResponse.
 */
@Getter
public class ProductDocumentMongoWriteException extends BaseException {
    private final List<ProductUpdateEvent> failedEvents;
    private final List<ProductUpdateEvent> criteriaNotMatchedEvents;

    public <T> ProductDocumentMongoWriteException(List<ProductUpdateEvent> failedEvents, List<ProductUpdateEvent> criteriaNotMatchedEvents) {
        super(HttpStatus.BAD_REQUEST, MONGO_WRITE_EXCEPTION);
        this.failedEvents = failedEvents;
        this.criteriaNotMatchedEvents = criteriaNotMatchedEvents;
    }

}


