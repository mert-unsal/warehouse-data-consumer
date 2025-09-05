package com.ikea.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.model.WarehouseMessage;
import com.ikea.repository.WarehouseMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.data.mongodb.host=localhost"
})
class WarehouseMessageConsumerTest {

    @Autowired
    private WarehouseMessageConsumer consumer;

    @MockBean
    private WarehouseMessageRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testConsumeValidMessage() throws Exception {
        // Arrange
        String jsonMessage = """
            {
                "messageId": "MSG001",
                "warehouseId": "WH001",
                "productId": "PROD123",
                "action": "STOCK_IN",
                "quantity": 100,
                "location": "A1-B2"
            }
            """;

        WarehouseMessage savedMessage = new WarehouseMessage();
        savedMessage.setId("generated-id");
        when(repository.save(any(WarehouseMessage.class))).thenReturn(savedMessage);

        // Act
        consumer.consume(jsonMessage);

        // Assert
        verify(repository).save(any(WarehouseMessage.class));
    }

    @Test
    void testConsumeInvalidMessage() {
        // Arrange
        String invalidMessage = "invalid json message";

        WarehouseMessage errorMessage = new WarehouseMessage();
        errorMessage.setId("error-id");
        when(repository.save(any(WarehouseMessage.class))).thenReturn(errorMessage);

        // Act
        consumer.consume(invalidMessage);

        // Assert - Should save error message
        verify(repository).save(any(WarehouseMessage.class));
    }
}
