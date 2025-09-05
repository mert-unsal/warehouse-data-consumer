package com.ikea.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.model.WarehouseMessage;
import com.ikea.repository.WarehouseMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class WarehouseMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseMessageConsumer.class);

    @Autowired
    private WarehouseMessageRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "${spring.kafka.consumer.topic:warehouse-events}", groupId = "${spring.kafka.consumer.group-id:warehouse-consumer-group}")
    public void consume(String message) {
        try {
            logger.info("Received message from Kafka: {}", message);

            // Parse the JSON message
            WarehouseMessage warehouseMessage = objectMapper.readValue(message, WarehouseMessage.class);

            // Store the raw message for debugging purposes
            warehouseMessage.setRawMessage(message);

            // Save to MongoDB
            WarehouseMessage savedMessage = repository.save(warehouseMessage);

            logger.info("Successfully persisted message to MongoDB with ID: {}", savedMessage.getId());

        } catch (Exception e) {
            logger.error("Error processing Kafka message: {}", message, e);

            // Even if parsing fails, we can still save the raw message for later analysis
            try {
                WarehouseMessage errorMessage = new WarehouseMessage();
                errorMessage.setRawMessage(message);
                errorMessage.setAction("ERROR");
                repository.save(errorMessage);
                logger.info("Saved raw message to MongoDB for error analysis");
            } catch (Exception saveException) {
                logger.error("Failed to save error message to MongoDB", saveException);
            }
        }
    }
}
