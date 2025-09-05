# IKEA Warehouse Data Consumer

A Spring Boot Kafka consumer application that listens to multiple Kafka topics to persist warehouse data (inventory, products, and warehouse events) into MongoDB. The application provides REST APIs for data management and warehouse analysis.

## Architecture Overview

This application processes three types of data:
- **Warehouse Events**: General warehouse operations and transactions
- **Inventory Data**: Article/item stock levels and information
- **Product Data**: Product definitions with required article compositions

## Features

- 🔄 **Multi-Topic Kafka Consumer**: Processes different message types from separate topics
- 📊 **MongoDB Persistence**: Stores all data in MongoDB with proper indexing
- 🔍 **REST API**: Comprehensive APIs for data access and warehouse analysis
- 📈 **Production Analysis**: Calculate manufacturing capacity based on inventory
- 🏗️ **Product Feasibility**: Check if products can be manufactured with current stock
- 📚 **API Documentation**: Auto-generated Swagger/OpenAPI documentation
- 🔧 **Error Handling**: Robust error handling with raw message preservation

## Data Models

### WarehouseMessage
```json
{
  "messageId": "MSG001",
  "warehouseId": "WH001", 
  "productId": "PROD123",
  "action": "STOCK_IN",
  "quantity": 100,
  "location": "A1-B2",
  "timestamp": "2025-09-05T12:00:00"
}
```

### InventoryItem
```json
{
  "art_id": "1",
  "name": "leg",
  "stock": "12"
}
```

### Product
```json
{
  "id": "TABLE001",
  "name": "Dining Table",
  "contain_articles": [
    {
      "art_id": "1",
      "amount_of": "4"
    },
    {
      "art_id": "2", 
      "amount_of": "1"
    }
  ]
}
```

## Prerequisites

- Java 21+
- Maven 3.6+
- Apache Kafka 2.8+
- MongoDB 4.4+

## Getting Started

### 1. Start Required Services

#### MongoDB
```bash
# Using Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest

# Or install locally and run
mongod
```

#### Apache Kafka
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties

# Create topics
bin/kafka-topics.sh --create --topic warehouse-events --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic inventory-events --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic inventory-updates --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic products-events --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic product-updates --bootstrap-server localhost:9092
```

### 2. Configure Application

Update `src/main/resources/application.properties`:

```properties
# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=warehouse_db

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=warehouse-consumer-group

# Kafka Topics
spring.kafka.consumer.topic=warehouse-events
spring.kafka.consumer.inventory-topic=inventory-events
spring.kafka.consumer.inventory-update-topic=inventory-updates
spring.kafka.consumer.products-topic=products-events
spring.kafka.consumer.product-update-topic=product-updates
```

### 3. Run the Application

```bash
# Compile and run
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/warehouse-data-consumer-1.0-SNAPSHOT.jar
```

The application will start on port 8080.

## API Endpoints

### Warehouse Messages
- `GET /api/warehouse-messages` - Get all warehouse messages (paginated)
- `GET /api/warehouse-messages/{id}` - Get message by ID
- `GET /api/warehouse-messages/warehouse/{warehouseId}` - Filter by warehouse
- `GET /api/warehouse-messages/product/{productId}` - Filter by product
- `GET /api/warehouse-messages/count` - Get total message count

### Inventory Management
- `GET /api/inventory` - Get all inventory items (paginated)
- `GET /api/inventory/{artId}` - Get item by article ID
- `GET /api/inventory/search?name={name}` - Search by name
- `GET /api/inventory/in-stock?minStock={amount}` - Items above stock threshold
- `POST /api/inventory` - Create/update inventory item
- `DELETE /api/inventory/{artId}` - Delete inventory item

### Product Management
- `GET /api/products` - Get all products (paginated)
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/search?name={name}` - Search by name
- `GET /api/products/by-article/{artId}` - Find products using specific article
- `POST /api/products` - Create/update product
- `DELETE /api/products/{id}` - Delete product

### Warehouse Analysis
- `GET /api/analysis/can-manufacture/{productId}` - Check if product can be manufactured
- `GET /api/analysis/production-capacity/{productId}` - Calculate max production quantity
- `GET /api/analysis/manufacturable-products` - Get all manufacturable products
- `GET /api/analysis/inventory-status/{productId}` - Detailed inventory status for product

### Health & Documentation
- `GET /api/warehouse-messages/health` - Application health check
- `GET /swagger-ui.html` - Swagger UI documentation
- `GET /api-docs` - OpenAPI specification

## Usage Examples

### Send Test Messages via Kafka

#### Warehouse Event
```bash
echo '{"messageId":"MSG001","warehouseId":"WH001","productId":"TABLE001","action":"STOCK_IN","quantity":10,"location":"A1-B2"}' | \
kafka-console-producer.sh --topic warehouse-events --bootstrap-server localhost:9092
```

#### Inventory Data
```bash
echo '{"inventory":[{"art_id":"1","name":"table leg","stock":"50"},{"art_id":"2","name":"table top","stock":"25"}]}' | \
kafka-console-producer.sh --topic inventory-events --bootstrap-server localhost:9092
```

#### Product Definition
```bash
echo '{"products":[{"id":"TABLE001","name":"Dining Table","contain_articles":[{"art_id":"1","amount_of":"4"},{"art_id":"2","amount_of":"1"}]}]}' | \
kafka-console-producer.sh --topic products-events --bootstrap-server localhost:9092
```

### REST API Usage

#### Check Production Capacity
```bash
curl http://localhost:8080/api/analysis/production-capacity/TABLE001
```
Response: `12` (can manufacture 12 tables with current inventory)

#### Get Manufacturable Products
```bash
curl http://localhost:8080/api/analysis/manufacturable-products
```

#### Add Inventory Item
```bash
curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/json" \
  -d '{"art_id":"3","name":"table screw","stock":"100"}'
```

## Monitoring & Logging

The application provides comprehensive logging for:
- Kafka message consumption
- Database operations
- API requests
- Error handling

Logs are configured to show:
- Message processing status
- MongoDB persistence confirmations
- Production analysis calculations
- Error details with stack traces

## Development

### Running Tests
```bash
mvn test
```

### Building Docker Image
```bash
docker build -t ikea/warehouse-data-consumer .
```

### IDE Setup
- Import as Maven project
- Set Java version to 21
- Enable annotation processing for Lombok (if added later)

## Configuration

The application supports environment-based configuration:

```bash
# MongoDB
SPRING_DATA_MONGODB_HOST=localhost
SPRING_DATA_MONGODB_PORT=27017
SPRING_DATA_MONGODB_DATABASE=warehouse_db

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPRING_KAFKA_CONSUMER_GROUP_ID=warehouse-consumer-group
```

## Troubleshooting

### Common Issues

1. **Kafka Connection Failed**
   - Ensure Kafka is running on localhost:9092
   - Check if topics exist

2. **MongoDB Connection Failed**
   - Verify MongoDB is running on localhost:27017
   - Check database permissions

3. **Port Already in Use**
   - Change server port in application.properties: `server.port=8081`

4. **Out of Memory**
   - Increase JVM heap size: `java -Xmx2g -jar app.jar`

### Logs to Check
```bash
# Application logs
tail -f logs/application.log

# Kafka consumer logs
tail -f logs/kafka.log
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Create Pull Request

## License

This project is licensed under the MIT License.
