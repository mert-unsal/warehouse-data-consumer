package com.ikea.controller;

import com.ikea.model.WarehouseMessage;
import com.ikea.repository.WarehouseMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/warehouse-messages")
public class WarehouseMessageController {

    @Autowired
    private WarehouseMessageRepository repository;

    @GetMapping
    public ResponseEntity<List<WarehouseMessage>> getAllMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<WarehouseMessage> messages = repository.findAll(pageRequest);
        return ResponseEntity.ok(messages.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseMessage> getMessageById(@PathVariable String id) {
        Optional<WarehouseMessage> message = repository.findById(id);
        return message.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<WarehouseMessage>> getMessagesByWarehouse(@PathVariable String warehouseId) {
        List<WarehouseMessage> messages = repository.findByWarehouseId(warehouseId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<WarehouseMessage>> getMessagesByProduct(@PathVariable String productId) {
        List<WarehouseMessage> messages = repository.findByProductId(productId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<WarehouseMessage>> getMessagesByAction(@PathVariable String action) {
        List<WarehouseMessage> messages = repository.findByAction(action);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalMessageCount() {
        long count = repository.count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Warehouse Data Consumer is running");
    }
}
