package com.ikea.warehouse_data_consumer.data.event;

import java.time.Instant;

public record InventoryUpdateEvent(String artId, String name, Long stock, Instant fileCreatedAt) {}
