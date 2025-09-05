package com.ikea.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inventory_items")
@Schema(description = "Inventory item representing a warehouse article")
public record InventoryItem(
    @Id
    @JsonProperty("art_id")
    @Schema(description = "Unique article identifier", example = "1")
    String artId,

    @Schema(description = "Name of the inventory item", example = "leg")
    String name,

    @Schema(description = "Available stock quantity", example = "12")
    String stock
) {}
