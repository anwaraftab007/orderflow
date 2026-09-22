# OrderFlow

OrderFlow is a backend-only order and inventory management system built with Java 21 and Spring Boot.

It is a monolithic REST API focused on authentication, order processing, inventory consistency, Redis caching, and concurrent stock handling.

## Features

* JWT-based signup and login
* BCrypt password hashing
* CUSTOMER and ADMIN role-based authorization
* Product and inventory management
* Transactional order creation and cancellation
* Historical product prices in order items
* Atomic stock deduction to prevent overselling
* Redis product caching
* Redis-based order idempotency
* Order status lifecycle validation
* In-process order events

## Tech Stack

* Java 21
* Spring Boot
* Spring Security
* JWT
* JPA / Hibernate
* PostgreSQL
* Neon PostgreSQL
* Redis / Upstash Redis
* Maven
* H2 for local development

## Architecture

```text
Client
  |
  v
Spring Boot REST API
  |
  +-- Spring Security / JWT
  |
  +-- Redis
  |     +-- Product Cache
  |     +-- Order Idempotency
  |
  +-- PostgreSQL
  |     +-- Users
  |     +-- Products
  |     +-- Inventory
  |     +-- Orders
  |     +-- Order Items
  |
  +-- Spring Events
        +-- Local Event Listener
```

## Database

| Table           | Purpose                            |
| --------------- | ---------------------------------- |
| `app_users`     | User accounts and roles            |
| `products`      | Product details and current price  |
| `inventory`     | Available product quantity         |
| `orders`        | Orders, status, totals, timestamps |
| `order_items`   | Quantity and purchase-time price   |
| `system_checks` | Persistence check                  |

Relationships:

```text
User     1:N Orders
Order    1:N OrderItems
Product  1:N OrderItems
Product  1:1 Inventory
```

## Order Lifecycle

```text
CREATED -> CONFIRMED -> SHIPPED -> DELIVERED
    |
    +-----------------> CANCELLED
```

Orders can only be cancelled from `CREATED` or `CONFIRMED`.

## Inventory Concurrency

Stock deduction is handled by a conditional PostgreSQL update:

```sql
UPDATE inventory
SET quantity = quantity - :amount
WHERE product_id = :productId
  AND quantity >= :amount;
```

This prevents concurrent requests from overselling available stock.

Order creation and stock deduction run in the same transaction. If any item cannot be fulfilled, the transaction is rolled back.

## Redis

Redis is used for:

* Product caching with a 5-minute TTL
* Cache invalidation after inventory changes
* Order idempotency using `SET NX EX`
* 24-hour idempotency key expiration

Redis failures fall back to PostgreSQL for product reads.

## API Endpoints

| Method | Endpoint                        | Access        |
| ------ | ------------------------------- | ------------- |
| POST   | `/api/auth/signup`              | Public        |
| POST   | `/api/auth/login`               | Public        |
| GET    | `/api/health`                   | Public        |
| GET    | `/api/me`                       | Authenticated |
| GET    | `/api/admin/test`               | ADMIN         |
| POST   | `/api/products`                 | ADMIN         |
| GET    | `/api/products`                 | Authenticated |
| GET    | `/api/products/{id}`            | Authenticated |
| POST   | `/api/orders`                   | Authenticated |
| GET    | `/api/orders/my`                | Authenticated |
| GET    | `/api/orders/{id}`              | Owner / ADMIN |
| PATCH  | `/api/orders/{id}/cancel`       | Owner / ADMIN |
| PATCH  | `/api/admin/orders/{id}/status` | ADMIN         |

Order creation requires:

```http
Authorization: Bearer <JWT>
Idempotency-Key: <unique-value>
```

## Running Locally

The default local profile uses H2 and an in-process Redis implementation.

```powershell
.\mvnw.cmd spring-boot:run
```

Build:

```powershell
.\mvnw.cmd clean package
```

Production uses PostgreSQL and Upstash Redis through environment variables.

## Environment Variables

```text
DB_URL
DB_USER
DB_PASS
JWT_SECRET
REDIS_URL
REDIS_TOKEN
SPRING_PROFILES_ACTIVE
SERVER_PORT
```

Do not commit real credentials or secrets.

## Future Improvements

* Flyway or Liquibase migrations
* Transactional outbox and durable message broker
* JWT refresh tokens and revocation
* Metrics and distributed tracing
* Structured logging
* Production integration tests
* OpenAPI / Swagger documentation
