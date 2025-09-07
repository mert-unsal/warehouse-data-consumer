package com.ikea.warehouse_data_consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@EnableMongoAuditing
@SpringBootApplication
@ConfigurationPropertiesScan
public class WarehouseDataConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseDataConsumerApplication.class, args);
    }
}