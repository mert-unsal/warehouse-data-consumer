package com.ikea.warehouse_data_consumer.data.dto;

import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Wrapper for inventory data containing list of inventory items")
public record InventoryData(
    @Schema(description = "List of inventory items")
    List<ArticleDocument> inventory
) {}
