package com.ikea.warehouse_data_consumer.service;

import com.ikea.warehouse_data_consumer.data.event.KafkaKeyValueRecord;
import com.ikea.warehouse_data_consumer.data.exception.KafkaProduceFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendBatch(String topic, List<KafkaKeyValueRecord> producerRecordList) {
            List<CompletableFuture<SendResult<String, Object>>> futures = new ArrayList<>();
            producerRecordList.forEach((producerRecord) -> {
                CompletableFuture<SendResult<String, Object>> completableFuture = kafkaTemplate.send(new ProducerRecord<>(topic, producerRecord.key(), producerRecord.event()));
                futures.add(completableFuture);
                completableFuture.whenComplete((stringObjectSendResult, throwable) -> {
                    if (Objects.nonNull(throwable)) {
                        log.error("Sending kafka message failed with the following exception : {}, topic : {}, event: {}", throwable.getMessage(),
                                stringObjectSendResult.getProducerRecord().topic(),
                                stringObjectSendResult.getProducerRecord().value());
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
