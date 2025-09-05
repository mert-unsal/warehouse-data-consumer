package com.ikea.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Document(collection = "warehouse_messages")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WarehouseMessage {

    @Id
    private String id;
    private String messageId;
    private String warehouseId;
    private String productId;
    private String action;
    private Integer quantity;
    private String location;
    private LocalDateTime timestamp;
    private String rawMessage;

    public WarehouseMessage() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    @Override
    public String toString() {
        return "WarehouseMessage{" +
                "id='" + id + '\'' +
                ", messageId='" + messageId + '\'' +
                ", warehouseId='" + warehouseId + '\'' +
                ", productId='" + productId + '\'' +
                ", action='" + action + '\'' +
                ", quantity=" + quantity +
                ", location='" + location + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
