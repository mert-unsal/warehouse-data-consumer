package com.ikea.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "products")
@Schema(description = "Product definition with required articles")
public record Product(
    @Id
    String id,

    @Schema(description = "Product name", example = "Dining Chair")
    String name,

    @JsonProperty("contain_articles")
    @Schema(description = "List of articles required to build this product")
    List<ArticleAmount> containArticles
) {}
