package com.ikea.controller;

import com.ikea.model.InventoryItem;
import com.ikea.repository.InventoryItemRepository;
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
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "Endpoints for managing inventory items")
public class InventoryController {

    @Autowired
    private InventoryItemRepository repository;

    @GetMapping
    @Operation(summary = "Get all inventory items", description = "Retrieve all inventory items with pagination")
    public ResponseEntity<List<InventoryItem>> getAllItems(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("artId"));
        Page<InventoryItem> items = repository.findAll(pageRequest);
        return ResponseEntity.ok(items.getContent());
    }

    @GetMapping("/{artId}")
    @Operation(summary = "Get inventory item by article ID", description = "Retrieve specific inventory item by its article ID")
    public ResponseEntity<InventoryItem> getItemByArtId(@PathVariable String artId) {
        Optional<InventoryItem> item = repository.findByArtId(artId);
        return item.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search inventory items by name", description = "Search for inventory items containing the specified name")
    public ResponseEntity<List<InventoryItem>> searchByName(
            @Parameter(description = "Name to search for") @RequestParam String name) {
        List<InventoryItem> items = repository.findByNameContainingIgnoreCase(name);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/in-stock")
    @Operation(summary = "Get items with stock above threshold", description = "Retrieve items with stock greater than specified amount")
    public ResponseEntity<List<InventoryItem>> getItemsInStock(
            @Parameter(description = "Minimum stock threshold") @RequestParam(defaultValue = "0") String minStock) {
        List<InventoryItem> items = repository.findByStockGreaterThan(minStock);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/count")
    @Operation(summary = "Get total inventory item count", description = "Get the total number of inventory items in the system")
    public ResponseEntity<Long> getTotalItemCount() {
        long count = repository.count();
        return ResponseEntity.ok(count);
    }

    @PostMapping
    @Operation(summary = "Create or update inventory item", description = "Add new inventory item or update existing one")
    public ResponseEntity<InventoryItem> createOrUpdateItem(@RequestBody InventoryItem item) {
        InventoryItem savedItem = repository.save(item);
        return ResponseEntity.ok(savedItem);
    }

    @DeleteMapping("/{artId}")
    @Operation(summary = "Delete inventory item", description = "Remove inventory item by article ID")
    public ResponseEntity<Void> deleteItem(@PathVariable String artId) {
        if (repository.existsByArtId(artId)) {
            repository.deleteById(artId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
