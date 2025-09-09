# Warehouse System (CQRS + Event-Driven)

This repository contains four cooperating Spring Boot services that manage warehouse products, articles (inventory), and product availability using Kafka and MongoDB.

Services
- warehouse-data-ingestion-service — Bulk file ingestion to normalized domain events.
- warehouse-data-consumer — Kafka consumer that materializes MongoDB read models.
- warehouse-command-api — Write-side commands for Articles and Products.
- warehouse-query-api — Read-side API that returns denormalized product availability.

Architecture overview
- Normalized writes: Ingestion and Command services emit small, canonical events per entity (InventoryUpdate, ProductUpdate).
- Event backbone: Kafka decouples producers and consumers, enabling backpressure and horizontal scale.
- Materialization: Data Consumer upserts articles/products to MongoDB as the read model source.
- Denormalized reads: Query API computes buildableQuantity per product from current article stock and product requirements.
- Observability: All services expose health endpoints and OpenAPI docs; structured logs are enabled.

Core concept: buildableQuantity
- For each product, buildableQuantity = min(floor(article.stock / requiredAmount) for each required article).
- If any required article is missing, the result is 0.

Tech stack
- Java 21, Spring Boot 3.x
- Kafka (producers/consumers), MongoDB
- Spring Web, Spring Data MongoDB
- Jackson, Actuator, springdoc-openapi
- Docker and Docker Compose
- OpenTelemetry (optional), Maven, Lombok (if present)

Ports (defaults)
- warehouse-data-ingestion-service: 8081
- warehouse-data-consumer: 8080
- warehouse-command-api: 8082
- warehouse-query-api: 8083
- MongoDB: 27017 (via container)
- Kafka broker: 9092 (host) / 29092 (in-network)
- Kafka UI (optional): 8090

Quick start (Docker Compose)
1) Build and start everything:
   - docker compose up -d --build
2) Check health:
   - Ingestion: http://localhost:8081/actuator/health
   - Consumer: http://localhost:8080/actuator/health
   - Command: http://localhost:8082/actuator/health
   - Query: http://localhost:8083/actuator/health
3) Open API docs (examples):
   - Ingestion: http://localhost:8081/swagger-ui.html
   - Command: http://localhost:8082/swagger-ui.html
   - Query: http://localhost:8083/swagger-ui.html

End-to-end test (happy path)
- Option A: Bulk ingestion
  1) Upload inventory.json to the ingestion service.
  2) Upload products.json to the ingestion service.
  3) Verify messages in Kafka (Kafka UI optional).
  4) The consumer persists articles/products to MongoDB.
  5) Call the query API to see buildableQuantity per product.

- Option B: Command-driven
  1) Create/update articles and products via the Command API.
  2) (If event publishing is enabled) verify messages in Kafka.
  3) The consumer persists/updates MongoDB.
  4) Query the product endpoint for computed availability.

Local development (without Docker)
- Prerequisites: Java 21, Maven, running Kafka and MongoDB (or Testcontainers in tests).
- Each service README contains per-service commands and environment variables.
- Common environment variables:
  - PORT=808X
  - SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/ikea
  - KAFKA_BOOTSTRAP_SERVERS=localhost:9092

Testing strategy
- Unit tests: services, mappers, controller validations.
- Integration tests: with Testcontainers for Kafka and MongoDB (recommended).
- Contract tests: event schema validation for producer/consumer alignment.
- Load tests: k6/JMeter for ingestion and query paths; monitor consumer lag.

Resilience and operations
- Retry/error topics for Kafka consumers (configurable).
- Structured logging with trace correlation (if OpenTelemetry enabled).
- Actuator health and metrics endpoints for all services.

Security and hardening (production)
- Add authentication/authorization for Command and Query APIs.
- Evolve event/doc schemas with versioning.
- Backups and retention for MongoDB and Kafka.
- Scale via Kafka partitions and service replicas; ensure consumer idempotency.

Service documentation
- See service-specific READMEs:
  - ./warehouse-data-ingestion-service/README.md
  - ./warehouse-data-consumer/README.md
  - ./warehouse-command-api/README.md
  - ./warehouse-query-api/README.md

