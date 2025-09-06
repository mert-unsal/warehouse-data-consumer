package com.ikea.warehouse_data_consumer.data.event;


import com.ikea.warehouse_data_consumer.data.document.ProductDocument;

import java.util.List;

public record ProductUpdateEvent(
    List<ProductDocument> productDocuments,
    Long timestamp
) {}
