# IKEA Warehouse Data Consumer

A Spring Boot Kafka consumer application that listens to Kafka topics and persists warehouse data (inventory and product definitions) into MongoDB. This service runs in the background; it does not expose business REST endpoints.

## Architecture Overview

This application processes two kinds of inputs:
- **Inventory Updates**: Article/item stock levels and names
- **Product Updates**: Product definitions with required article compositions

## Features

- 🔄 **Multi-topic Kafka consumer**: Consumes product and inventory messages from dedicated topics
- 📊 **MongoDB persistence**: Stores articles and products with sensible indexes (name index; nested containArticles.artId compound index)
- 🧵 **Retry/Error topics support**: Separate configurable topics for retry and error handling (see application-kafka.yaml)
- 🩺 **Operational endpoints**: Actuator health endpoint; OpenAPI UI available but no REST controllers in this service
- 📜 **Structured logging & telemetry**: Logback JSON encoder and OpenTelemetry auto-instrumentation
- 🐳 **Container-ready**: Dockerfiles and docker-compose for local orchestration

## Data Models

### Kafka payloads (consumed)

- InventoryUpdateEvent
```json
{
  "artId": "1",
  "name": "table leg",
  "stock": 50,
  "fileCreatedAt": "2025-09-05T12:00:00Z"
}
```

- ProductUpdateEvent
```json
{
  "name": "Dining Table",
  "contain_articles": [
    { "art_id": "1", "amount_of": 4 },
    { "art_id": "2", "amount_of": 1 }
  ],
  "fileCreatedAt": "2025-09-05T12:00:00Z"
}
```

Notes:
- Article requirements use fields art_id and amount_of on the wire; they map to ArticleAmount(artId, amountOf) in code.

### MongoDB documents (stored)

- articles collection (ArticleDocument)
```json
{
  "_id": "1",
  "name": "table leg",
  "stock": 50,
  "lastMessageId": null,
  "version": 0,
  "createdDate": "2025-09-05T12:00:01Z",
  "lastModifiedDate": "2025-09-05T12:00:01Z",
  "fileCreatedAt": "2025-09-05T12:00:00Z"
}
```

- products collection (ProductDocument)
```json
{
  "_id": "64f0c1...",
  "name": "Dining Table",
  "containArticles": [
    { "artId": "1", "amountOf": 4 },
    { "artId": "2", "amountOf": 1 }
  ],
  "version": 0,
  "createdDate": "2025-09-05T12:00:02Z",
  "lastModifiedDate": "2025-09-05T12:00:02Z",
  "fileCreatedAt": "2025-09-05T12:00:00Z"
}
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker 24+ and Docker Compose

## Getting Started

### 1. Start Required Services (via Docker Compose)
From the repository root, this service is orchestrated together with MongoDB, Kafka and other apps.

```bash
# Build and start everything
docker compose up -d --build

# Or start only this service (and its deps)
docker compose up -d --build warehouse-data-consumer
```

> Services and ports (by default):
> - MongoDB: localhost:27017
> - Kafka broker (KRaft): localhost:9092 (internal: kafka:29092)
> - Kafka UI: http://localhost:8090
> - Warehouse Data Consumer: http://localhost:8080

Topics are created/managed manually as needed; defaults are read from application-kafka.yaml. You can also use Kafka UI to create them quickly.

### 2. Configure Application

Configuration is YAML-based and profile-driven. By default, the following profiles are active: `default,logging,management,kafka,mongo` (see application.yaml).

Key files:
- src/main/resources/application.yaml (common settings, server.port=8080)
- src/main/resources/application-kafka.yaml (Kafka topics and consumer settings)
- src/main/resources/application-mongo.yaml (MongoDB connection)

You can override via environment variables (used in Docker Compose):

```bash
# MongoDB
MONGODB_URI=mongodb://localhost:27017/ikea
MONGODB_DATABASE=ikea

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP_ID=warehouse-data-ingestion-group

# App port
PORT=8080
```

Kafka topics (from application-kafka.yaml; override via env):
- Product topics:
  - KAFKA_TOPIC_PRODUCT (default: ikea.warehouse.product.update.topic)
  - KAFKA_TOPIC_PRODUCT_RETRY (default: ikea.warehouse.product.update.topic.retry)
  - KAFKA_TOPIC_PRODUCT_ERROR (default: ikea.warehouse.product.update.topic.error)
- Inventory topics:
  - KAFKA_TOPIC_INVENTORY (default: ikea.warehouse.inventory.update.topic)
  - KAFKA_TOPIC_INVENTORY_RETRY (default: ikea.warehouse.inventory.update.topic.retry)
  - KAFKA_TOPIC_INVENTORY_ERROR (default: ikea.warehouse.inventory.update.topic.error)

### 3. Run the Application

```bash
# Compile and run with active profiles (for local without Docker)
mvn spring-boot:run -Dspring-boot.run.profiles=default,logging,management,kafka,mongo \
  -Dspring-boot.run.jvmArguments="--enable-preview"

# Or build and run JAR
mvn clean package
java --enable-preview -jar target/warehouse-data-consumer-1.0-SNAPSHOT.jar
```

The application starts on port 8080 (override via PORT env).

## HTTP Endpoints

This service does not expose business REST APIs. It is a background consumer that:
- Listens to Kafka topics for product and inventory updates
- Persists normalized data into MongoDB

Available HTTP endpoints are limited to operational endpoints only:
- GET /actuator/health — liveness/readiness
- GET /swagger-ui.html and GET /api-docs — provided by springdoc if enabled, but there are no controllers documented (useful mainly for future extensions)

## Usage Examples

### Send Test Messages via Kafka

#### Product update event
```bash
echo '{"id":"TABLE001","name":"Dining Table","contain_articles":[{"art_id":"1","amount_of":"4"},{"art_id":"2","amount_of":"1"}]}' | \
  kafka-console-producer.sh --topic ${KAFKA_TOPIC_PRODUCT:-ikea.warehouse.product.update.topic} --bootstrap-server localhost:9092
```

#### Inventory update event
```bash
echo '{"art_id":"1","name":"table leg","stock":"50"}' | \
  kafka-console-producer.sh --topic ${KAFKA_TOPIC_INVENTORY:-ikea.warehouse.inventory.update.topic} --bootstrap-server localhost:9092
```

## Monitoring & Logging

The application provides logging for:
- Kafka message consumption
- Database operations
- Error handling

Logs are configured to show:
- Message processing status
- MongoDB persistence confirmations
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
