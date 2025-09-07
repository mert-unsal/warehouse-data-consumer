package com.ikea.warehouse_data_consumer.data.exception;

import com.ikea.warehouse_data_consumer.data.event.InventoryUpdateEvent;
import com.mongodb.client.model.WriteModel;
import lombok.Getter;
import org.bson.Document;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com.ikea.warehouse_data_consumer.util.ExceptionMessageUtil.MONGO_WRITE_EXCEPTION;


/**
 * Custom exception for file processing errors in the warehouse data ingestion service.
 * This exception will be caught by the GlobalExceptionHandler and converted to a proper ErrorResponse.
 */
@Getter
public class CustomMongoWriteException extends BaseException {
    private final List<InventoryUpdateEvent> failedEvents;
    public <T> CustomMongoWriteException(List<InventoryUpdateEvent> failedEvents) {
        super(HttpStatus.BAD_REQUEST, MONGO_WRITE_EXCEPTION);
        this.failedEvents = failedEvents;
    }
}


