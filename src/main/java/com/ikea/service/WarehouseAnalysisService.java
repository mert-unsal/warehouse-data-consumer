package com.ikea.service;

import com.ikea.model.ArticleAmount;
import com.ikea.model.InventoryItem;
import com.ikea.model.Product;
import com.ikea.repository.InventoryItemRepository;
import com.ikea.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WarehouseAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseAnalysisService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryItemRepository inventoryRepository;

    /**
     * Check if a product can be manufactured based on current inventory
     */
    public boolean canManufactureProduct(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            logger.warn("Product not found: {}", productId);
            return false;
        }

        Product product = productOpt.get();
        return checkArticleAvailability(product.containArticles());
    }

    /**
     * Calculate how many units of a product can be manufactured
     */
    public int calculateMaxProductionQuantity(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return 0;
        }

        Product product = productOpt.get();
        int maxQuantity = Integer.MAX_VALUE;

        for (ArticleAmount articleAmount : product.containArticles()) {
            Optional<InventoryItem> inventoryOpt = inventoryRepository.findByArtId(articleAmount.artId());
            if (inventoryOpt.isEmpty()) {
                return 0; // Cannot manufacture if any required article is missing
            }

            InventoryItem inventory = inventoryOpt.get();
            int availableStock = Integer.parseInt(inventory.stock());
            int requiredAmount = Integer.parseInt(articleAmount.amountOf());

            int possibleUnits = availableStock / requiredAmount;
            maxQuantity = Math.min(maxQuantity, possibleUnits);
        }

        return maxQuantity == Integer.MAX_VALUE ? 0 : maxQuantity;
    }

    /**
     * Get all products that can be manufactured with current inventory
     */
    public List<Product> getManufacturableProducts() {
        List<Product> allProducts = productRepository.findAll();
        return allProducts.stream()
                .filter(product -> checkArticleAvailability(product.containArticles()))
                .toList();
    }

    /**
     * Get inventory status for all articles required by a product
     */
    public Map<String, InventoryStatus> getProductInventoryStatus(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return Collections.emptyMap();
        }

        Product product = productOpt.get();
        Map<String, InventoryStatus> status = new HashMap<>();

        for (ArticleAmount articleAmount : product.containArticles()) {
            Optional<InventoryItem> inventoryOpt = inventoryRepository.findByArtId(articleAmount.artId());

            if (inventoryOpt.isPresent()) {
                InventoryItem inventory = inventoryOpt.get();
                int available = Integer.parseInt(inventory.stock());
                int required = Integer.parseInt(articleAmount.amountOf());

                status.put(articleAmount.artId(), new InventoryStatus(
                    inventory.name(),
                    available,
                    required,
                    available >= required
                ));
            } else {
                status.put(articleAmount.artId(), new InventoryStatus(
                    "Unknown Article",
                    0,
                    Integer.parseInt(articleAmount.amountOf()),
                    false
                ));
            }
        }

        return status;
    }

    private boolean checkArticleAvailability(List<ArticleAmount> requiredArticles) {
        for (ArticleAmount articleAmount : requiredArticles) {
            Optional<InventoryItem> inventoryOpt = inventoryRepository.findByArtId(articleAmount.artId());

            if (inventoryOpt.isEmpty()) {
                return false;
            }

            InventoryItem inventory = inventoryOpt.get();
            int availableStock = Integer.parseInt(inventory.stock());
            int requiredAmount = Integer.parseInt(articleAmount.amountOf());

            if (availableStock < requiredAmount) {
                return false;
            }
        }
        return true;
    }

    public record InventoryStatus(
        String articleName,
        int availableStock,
        int requiredAmount,
        boolean sufficient
    ) {}
}
