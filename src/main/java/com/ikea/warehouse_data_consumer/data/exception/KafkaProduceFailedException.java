package com.ikea.warehouse_data_consumer.data.exception;

import lombok.Getter;

/**
 * Custom exception for file processing errors in the warehouse data ingestion service.
 * This exception will be caught by the GlobalExceptionHandler and converted to a proper ErrorResponse.
 */
@Getter
public class KafkaProduceFailedException extends RuntimeException {

    private final String error;

    public KafkaProduceFailedException(String message) {
        super(message);
        this.error = null;
    }

    public KafkaProduceFailedException(String message, Throwable cause) {
        super(message, cause);
        this.error = null;
    }

    public KafkaProduceFailedException(String message, String error) {
        super(message);
        this.error = error;
    }

    public KafkaProduceFailedException(String message, String error, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

}
