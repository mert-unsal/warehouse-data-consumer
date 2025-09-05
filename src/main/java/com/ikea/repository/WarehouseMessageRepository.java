package com.ikea.repository;

import com.ikea.model.WarehouseMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WarehouseMessageRepository extends MongoRepository<WarehouseMessage, String> {

    List<WarehouseMessage> findByWarehouseId(String warehouseId);

    List<WarehouseMessage> findByProductId(String productId);

    List<WarehouseMessage> findByAction(String action);

    List<WarehouseMessage> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<WarehouseMessage> findByWarehouseIdAndProductId(String warehouseId, String productId);
}
