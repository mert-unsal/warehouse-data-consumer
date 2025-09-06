package com.ikea.warehouse_data_consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@EnableMongoAuditing
@SpringBootApplication
public class WarehouseDataConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseDataConsumerApplication.class, args);
    }
}