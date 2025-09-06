package com.ikea.warehouse_data_consumer.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Article requirement specification for a product")
public record ArticleAmount(
    @JsonProperty("art_id")
    @Schema(description = "Article identifier", example = "1")
    String artId,

    @JsonProperty("amount_of")
    @Schema(description = "Required quantity of this article", example = "4")
    Long amountOf
) {}
