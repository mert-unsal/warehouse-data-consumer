package com.ikea.controller;

import com.ikea.model.Product;
import com.ikea.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "Endpoints for managing products and their article requirements")
public class ProductController {

    @Autowired
    private ProductRepository repository;

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve all products with pagination")
    public ResponseEntity<List<Product>> getAllProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name"));
        Page<Product> products = repository.findAll(pageRequest);
        return ResponseEntity.ok(products.getContent());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve specific product by its ID")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Optional<Product> product = repository.findById(id);
        return product.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name", description = "Search for products containing the specified name")
    public ResponseEntity<List<Product>> searchByName(
            @Parameter(description = "Name to search for") @RequestParam String name) {
        List<Product> products = repository.findByNameContainingIgnoreCase(name);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/by-article/{artId}")
    @Operation(summary = "Find products using specific article", description = "Find all products that require the specified article")
    public ResponseEntity<List<Product>> getProductsByArticle(@PathVariable String artId) {
        List<Product> products = repository.findByContainArticles_ArtId(artId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/count")
    @Operation(summary = "Get total product count", description = "Get the total number of products in the system")
    public ResponseEntity<Long> getTotalProductCount() {
        long count = repository.count();
        return ResponseEntity.ok(count);
    }

    @PostMapping
    @Operation(summary = "Create or update product", description = "Add new product or update existing one")
    public ResponseEntity<Product> createOrUpdateProduct(@RequestBody Product product) {
        Product savedProduct = repository.save(product);
        return ResponseEntity.ok(savedProduct);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Remove product by ID")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
