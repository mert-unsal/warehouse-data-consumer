package com.ikea.controller;

import com.ikea.model.Product;
import com.ikea.service.WarehouseAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Warehouse Analysis", description = "Endpoints for warehouse inventory analysis and production planning")
public class WarehouseAnalysisController {

    @Autowired
    private WarehouseAnalysisService analysisService;

    @GetMapping("/can-manufacture/{productId}")
    @Operation(summary = "Check if product can be manufactured",
               description = "Check if a product can be manufactured based on current inventory levels")
    public ResponseEntity<Boolean> canManufactureProduct(@PathVariable String productId) {
        boolean canManufacture = analysisService.canManufactureProduct(productId);
        return ResponseEntity.ok(canManufacture);
    }

    @GetMapping("/production-capacity/{productId}")
    @Operation(summary = "Calculate maximum production quantity",
               description = "Calculate how many units of a product can be manufactured with current inventory")
    public ResponseEntity<Integer> getProductionCapacity(@PathVariable String productId) {
        int maxQuantity = analysisService.calculateMaxProductionQuantity(productId);
        return ResponseEntity.ok(maxQuantity);
    }

    @GetMapping("/manufacturable-products")
    @Operation(summary = "Get all manufacturable products",
               description = "Get list of all products that can be manufactured with current inventory")
    public ResponseEntity<List<Product>> getManufacturableProducts() {
        List<Product> products = analysisService.getManufacturableProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/inventory-status/{productId}")
    @Operation(summary = "Get product inventory status",
               description = "Get detailed inventory status for all articles required by a product")
    public ResponseEntity<Map<String, WarehouseAnalysisService.InventoryStatus>> getProductInventoryStatus(
            @Parameter(description = "Product ID to analyze") @PathVariable String productId) {
        Map<String, WarehouseAnalysisService.InventoryStatus> status =
            analysisService.getProductInventoryStatus(productId);
        return ResponseEntity.ok(status);
    }
}
