# IKEA Warehouse Data Consumer

A background Spring Boot Kafka consumer that ingests inventory & product update events and persists them to MongoDB. It exposes only operational endpoints (health & OpenAPI shell) — no business REST APIs.

## Architecture Overview
Processing pipeline:
1. Kafka listeners (inventory & product topics) receive JSON events.
2. Jackson deserializes into domain event records (`InventoryUpdateEvent`, `ProductUpdateEvent`).
3. Services (`InventoryService`, product service equivalent) perform bulk upserts using MongoDB `bulkWrite` operations for efficiency.
4. Retryable / non-retryable errors routed to configured retry/error topics.
5. Successful operations update `articles` and `products` collections with versioning & timestamps.

Design highlights:
- Batch & single-message listener container factories (manual ack, `MANUAL_IMMEDIATE`).
- Optimistic locking via `@Version` fields on documents.
- Explicit retry / error topic separation (allows later DLQ analytics).
- Structured JSON logging & OpenTelemetry auto instrumentation (exporters disabled by default unless configured).
- Indexing for name lookups & nested `containArticles.artId` filtering.

## Consumed Event Schemas
InventoryUpdateEvent (as published by ingestion service):
```json
{
  "artId": "1",
  "name": "table leg",
  "stock": "50",          // may arrive as a string
  "fileCreatedAt": "2025-09-05T12:00:00Z"
}
```
ProductUpdateEvent:
```json
{
  "name": "Dining Table",
  "containArticles": [
    { "art_id": "1", "amount_of": "4" },   // inner numeric values may be strings
    { "art_id": "2", "amount_of": "1" }
  ],
  "fileCreatedAt": "2025-09-05T12:00:00Z"
}
```
Coercion note:
- This consumer maps `stock` and `amount_of` to numeric (`Long`) fields. Jackson automatically coerces numeric strings; invalid values will trigger deserialization errors -> retry/error handling.

## MongoDB Persistence Models
Articles (`articles` collection):
```
{ _id: <artId>, name, stock, lastMessageId, version, createdDate, lastModifiedDate, fileCreatedAt }
```
Products (`products` collection):
```
{ _id: ObjectId, name, containArticles[ { artId, amountOf } ], version, createdDate, lastModifiedDate, fileCreatedAt }
```
Indexes:
- `articles.name` (simple index)
- `products.containArticles.artId` (compound index for nested array queries)

## Retry & Error Handling Strategy
Topic trio per domain (inventory & product):
- Primary topic (e.g. `ikea.warehouse.inventory.update.topic`)
- Retry topic (e.g. `...inventory.update.topic.retry`)
- Error topic (e.g. `...inventory.update.topic.error`)

Mechanism:
- Consumers annotated with `@Retryable` handle transient failures (e.g., optimistic locking or write conflicts if configured) with exponential backoff.
- Non-retryable (validation / write criteria mismatch) -> events forwarded to error topic.
- After max retries, remaining failures forwarded to retry or error topic depending on exception classification.

Manual acknowledgment ensures offsets are only committed after successful processing or final routing.

## Features
- Multi-topic Kafka consumption (inventory & product streams)
- Bulk MongoDB upsert for higher throughput
- Retry & error segregation topics
- Optimistic locking support
- Structured JSON logging & trace context
- Container & Docker Compose ready

## Configuration & Profiles
Active profiles (default): `default,logging,management,kafka,mongo`
Key resource files:
- `application.yaml` – base app + profile activation
- `application-kafka.yaml` – consumer, topics, retry/error topic names
- `application-mongo.yaml` – MongoDB URI & database
- `application-logging.yaml` – log levels

Environment variables (override defaults):
```
MONGODB_URI=mongodb://localhost:27017/ikea
MONGODB_DATABASE=ikea
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP_ID=warehouse-data-ingestion-group
KAFKA_TOPIC_PRODUCT=ikea.warehouse.product.update.topic
KAFKA_TOPIC_PRODUCT_RETRY=ikea.warehouse.product.update.topic.retry
KAFKA_TOPIC_PRODUCT_ERROR=ikea.warehouse.product.update.topic.error
KAFKA_TOPIC_INVENTORY=ikea.warehouse.inventory.update.topic
KAFKA_TOPIC_INVENTORY_RETRY=ikea.warehouse.inventory.update.topic.retry
KAFKA_TOPIC_INVENTORY_ERROR=ikea.warehouse.inventory.update.topic.error
PORT=8080
```

## Running Locally
Build & run:
```bash
mvn clean package
java --enable-preview -jar target/warehouse-data-consumer-1.0-SNAPSHOT.jar
```
Or via Maven plugin with profiles:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=default,logging,management,kafka,mongo \
  -Dspring-boot.run.jvmArguments="--enable-preview"
```
Docker:
```bash
docker build -t ikea/warehouse-data-consumer .
```
Compose (from repo root):
```bash
docker compose up -d --build warehouse-data-consumer
```

## Sending Test Messages
Inventory:
```bash
echo '{"artId":"1","name":"table leg","stock":50}' | \
  kafka-console-producer.sh --topic ${KAFKA_TOPIC_INVENTORY:-ikea.warehouse.inventory.update.topic} --bootstrap-server localhost:9092
```
Product:
```bash
echo '{"name":"Dining Table","containArticles":[{"art_id":"1","amount_of":4},{"art_id":"2","amount_of":1}]}' | \
  kafka-console-producer.sh --topic ${KAFKA_TOPIC_PRODUCT:-ikea.warehouse.product.update.topic} --bootstrap-server localhost:9092
```
(If ingestion service publishes string numeric values, both forms are accepted due to coercion.)

## Operational Endpoints
- `GET /actuator/health`
- `GET /api-docs` (OpenAPI JSON shell – no domain controllers)
- `GET /swagger-ui.html` (present but minimal)

## Logging & Observability
- JSON structured logs (logstash encoder) include trace context when OpenTelemetry exporters enabled.
- Retry & error routing events logged with counts (failed vs non-retryable) for visibility.
- Extend with metrics (Micrometer) if message throughput dashboards are required.

## Error Scenarios
| Scenario | Handling |
|----------|----------|
| Deserialization error (bad JSON) | Retry -> error topic after max attempts |
| Bulk write partial failure | Custom exception; failed subset forwarded appropriately |
| Non-matching criteria (no docs updated) | Classified non-retryable -> error topic |
| Mongo transient issue | Retries with backoff, then retry topic / error |

## Development Tips
- Use Kafka UI (default http://localhost:8090) to inspect topics.
- Validate indexes in Mongo shell: `db.products.getIndexes()`.
- Enable debug logging temporarily via `logging.level.com.ikea=DEBUG` env override.

## Limitations & Future Enhancements
- No deduplication / idempotency on message keys (`lastMessageId` reserved but unused).
- No dead-letter quarantine aside from flat error topics (could enrich with headers / reason codes).
- Concurrency currently 1; horizontal scaling increases partition consumption but requires partition strategy awareness.
- Lacks metrics (consumer lag, processing latency) – add Micrometer & KafkaConsumer metrics binder.
- No schema registry integration; relies on loose JSON contract.

## Future Enhancements Ideas
- Implement idempotency using `lastMessageId` or message hash.
- Add circuit breaker or rate limiting for Mongo spikes.
- Introduce per-record validation & schema enforcement (e.g., Avro / JSON Schema).
- Expose lightweight read-only endpoints for diagnostic introspection (toggle via profile).

## Troubleshooting Quick Guide
| Issue | Check |
|-------|-------|
| No messages consumed | Topic name/env vars; group id reset; consumer lag in Kafka UI |
| Mongo writes missing | Index constraints? Logs for bulk write result counts |
| Deserialization failures | Inspect error topic payloads; verify numeric string coercion |
| High latency | Increase consumer concurrency or partition count; adjust `max-poll-records` |

## Revision History
- 2025-09-09: README expanded (architecture, retry/error strategy, coercion note, limitations, enhancements).

## License
MIT (see LICENSE file).
