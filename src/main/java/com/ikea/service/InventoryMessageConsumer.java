package com.ikea.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.model.InventoryData;
import com.ikea.model.InventoryItem;
import com.ikea.repository.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(InventoryMessageConsumer.class);

    @Autowired
    private InventoryItemRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "${spring.kafka.consumer.inventory-topic:inventory-events}", groupId = "${spring.kafka.consumer.group-id:warehouse-consumer-group}")
    public void consumeInventoryData(String message) {
        try {
            logger.info("Received inventory message from Kafka: {}", message);

            // Parse the JSON message as InventoryData
            InventoryData inventoryData = objectMapper.readValue(message, InventoryData.class);

            // Process each inventory item
            List<InventoryItem> items = inventoryData.inventory();
            for (InventoryItem item : items) {
                // Update or insert inventory item
                InventoryItem savedItem = repository.save(item);
                logger.info("Successfully persisted inventory item: {} with stock: {}",
                    savedItem.artId(), savedItem.stock());
            }

            logger.info("Processed {} inventory items from message", items.size());

        } catch (Exception e) {
            logger.error("Error processing inventory message: {}", message, e);
        }
    }

    @KafkaListener(topics = "${spring.kafka.consumer.inventory-update-topic:inventory-updates}", groupId = "${spring.kafka.consumer.group-id:warehouse-consumer-group}")
    public void consumeInventoryUpdate(String message) {
        try {
            logger.info("Received inventory update from Kafka: {}", message);

            // Parse single inventory item update
            InventoryItem inventoryItem = objectMapper.readValue(message, InventoryItem.class);

            // Save the inventory item
            InventoryItem savedItem = repository.save(inventoryItem);

            logger.info("Successfully updated inventory item: {} with new stock: {}",
                savedItem.artId(), savedItem.stock());

        } catch (Exception e) {
            logger.error("Error processing inventory update message: {}", message, e);
        }
    }
}
