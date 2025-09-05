package com.ikea.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.model.Product;
import com.ikea.model.ProductsData;
import com.ikea.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ProductMessageConsumer.class);

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "${spring.kafka.consumer.products-topic:products-events}", groupId = "${spring.kafka.consumer.group-id:warehouse-consumer-group}")
    public void consumeProductsData(String message) {
        try {
            logger.info("Received products message from Kafka: {}", message);

            // Parse the JSON message as ProductsData
            ProductsData productsData = objectMapper.readValue(message, ProductsData.class);

            // Process each product
            List<Product> products = productsData.products();
            for (Product product : products) {
                // Save product to MongoDB
                Product savedProduct = repository.save(product);
                logger.info("Successfully persisted product: {} with {} articles",
                    savedProduct.name(), savedProduct.containArticles().size());
            }

            logger.info("Processed {} products from message", products.size());

        } catch (Exception e) {
            logger.error("Error processing products message: {}", message, e);
        }
    }

    @KafkaListener(topics = "${spring.kafka.consumer.product-update-topic:product-updates}", groupId = "${spring.kafka.consumer.group-id:warehouse-consumer-group}")
    public void consumeProductUpdate(String message) {
        try {
            logger.info("Received product update from Kafka: {}", message);

            // Parse single product update
            Product product = objectMapper.readValue(message, Product.class);

            // Save the product
            Product savedProduct = repository.save(product);

            logger.info("Successfully updated product: {} with {} required articles",
                savedProduct.name(), savedProduct.containArticles().size());

        } catch (Exception e) {
            logger.error("Error processing product update message: {}", message, e);
        }
    }
}
