package com.ikea.repository;

import com.ikea.model.InventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends MongoRepository<InventoryItem, String> {

    Optional<InventoryItem> findByArtId(String artId);

    List<InventoryItem> findByNameContainingIgnoreCase(String name);

    List<InventoryItem> findByStockGreaterThan(String stock);

    boolean existsByArtId(String artId);
}
