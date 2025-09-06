package com.ikea.warehouse_data_consumer.data.event;


import com.ikea.warehouse_data_consumer.data.document.ArticleDocument;

import java.util.List;

public record InventoryUpdateEvent(
    List<ArticleDocument> inventory,
    Long timestamp
) {}
