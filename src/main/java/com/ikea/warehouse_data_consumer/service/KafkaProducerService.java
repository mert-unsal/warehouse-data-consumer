package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.exception.KafkaProduceFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public <T> void sendBatch(String topic, Map<String,T> eventMap) {
            List<CompletableFuture<SendResult<String, Object>>> futures = new ArrayList<>();
            eventMap.forEach((key, event) -> {
                CompletableFuture<SendResult<String, Object>> completableFuture = kafkaTemplate.send(topic, key, event);
                futures.add(completableFuture);
                completableFuture.whenComplete((stringObjectSendResult, throwable) -> {
                    if (Objects.nonNull(throwable)) {
                        log.error("Sending kafka message failed with the following exception : {}, topic : {}, event: {}", throwable.getMessage(), topic, event);
                        throw new KafkaProduceFailedException(throwable.getMessage(), throwable);
                    }
                });
            });
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public <T> void send(String topic, String key, T event) {
        kafkaTemplate.send(topic, key, event);
    }

}
