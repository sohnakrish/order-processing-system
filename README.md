# Order Processing System
### PeerIslands Take-Home Assignment

The Order Processing System is a domain-driven backend application built
with Java 17 and Spring Boot 3.5 that orchestrates order creation, tracking,
fulfillment and cancellation through well-defined business workflows. By
combining RESTful services, scheduled processing, validation, centralized
error handling and interactive API documentation, the system delivers a
reliable foundation that can evolve into a larger event-driven commerce platform.

---

## Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Core language |
| Spring Boot | 3.5.14 | Application framework |
| Spring Data JPA | 3.5.14 | Database access |
| Spring Validation | 3.5.14 | Request validation |
| Spring Actuator | 3.5.14 | Health monitoring |
| H2 Database | 2.3.x | File-based embedded database |
| Lombok | Latest | Reduces boilerplate |
| JUnit 5 | 5.x | Unit test framework |
| Mockito | 5.x | Mocking for unit tests |
| AssertJ | 3.x | Fluent test assertions |

---

## Prerequisites

You will need Java 17 or higher and Maven 3.6 or higher installed on your machine before running the application.

---

## How to Run

Start by cloning the repository and navigating into the project folder.

```bash
git clone <repository-url>
cd order-processing
```

Before the very first run, make sure the following settings are in your `application.properties`. This tells the application to create the database schema fresh and load the seed data on startup.

```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=always
```

Then start the application.

```bash
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`.

Once the first run completes successfully, switch to persistent mode so your data is not wiped on every restart.

```properties
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=never
```

---

## Seed Data

The application comes preloaded with 7 sample orders spread across all statuses so you can test every endpoint right away without creating data manually.

| Customer | Status |
|----------|--------|
| James Anderson | PENDING |
| Priya Patel | PENDING |
| Michael Thompson | PROCESSING |
| Emily Rodriguez | SHIPPED |
| David Chen | DELIVERED |
| Sophie Williams | DELIVERED |
| Ryan Martinez | CANCELLED |

---

## How to Run Tests

Running all tests is a single command.

```bash
./mvnw test
```

You should see the following output when everything passes.

```
Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All tests follow the AAA pattern — Arrange, Act, Assert — using JUnit 5 and Mockito. No real database or server is started during unit tests, keeping them fast and focused.

| What is tested | Tests |
|---------------|-------|
| All business logic | 28 |
| HTTP layer and validations | 12 |
| Entity to DTO mapping | 12 |
| Background job behavior | 4 |
| Spring context loads | 1 |
| **Total** | **57** |

---

## API Reference

All requests go through the base URL below.

```
http://localhost:8080/api/orders
```

Every response from the API follows the same structure regardless of whether the request succeeded or failed.

```json
{
  "success": true,
  "message": "Order placed successfully.",
  "data": { },
  "timestamp": "2026-06-06T20:30:45"
}
```

---

### Create Order

```
POST http://localhost:8080/api/orders
```

```json
{
  "customerName": "John Smith",
  "customerEmail": "john.smith@gmail.com",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "items": [
    {
      "productId": "APPL-IPH15-001",
      "productName": "Apple iPhone 15",
      "quantity": 1,
      "productPrice": 999.99
    }
  ]
}
```

A successful request returns `201 Created` with the full order details including the generated order ID, all item line totals and timestamps.

```json
{
  "success": true,
  "message": "Order placed successfully.",
  "data": {
    "orderId": "d64120c6-e2cb-4bed-a3fb-cd5295e9f082",
    "customerName": "John Smith",
    "customerEmail": "john.smith@gmail.com",
    "status": "PENDING",
    "totalAmount": 999.99,
    "items": [
      {
        "itemId": "3704e91a-dc9f-4f2d-aaee-5e683077416d",
        "productName": "Apple iPhone 15",
        "productId": "APPL-IPH15-001",
        "quantity": 1,
        "productPrice": 999.99,
        "lineTotal": 999.99
      }
    ],
    "createdAt": "2026-06-06T20:30:45",
    "updatedAt": "2026-06-06T20:30:45"
  },
  "timestamp": "2026-06-06T20:30:45"
}
```

---

### Get Order by ID

```
GET http://localhost:8080/api/orders/{id}
```

Returns `200 OK` with the full order details. Returns `404 Not Found` if the order does not exist or has been cancelled.

---

### List Orders

```
GET http://localhost:8080/api/orders
GET http://localhost:8080/api/orders?status=PENDING
GET http://localhost:8080/api/orders?status=PENDING&page=0&size=10
```

The status filter is optional. When provided it restricts results to only orders in that status. Pagination defaults to page 0 with 10 results per page and accepts a maximum of 100 per page.

```json
{
  "success": true,
  "message": "Orders retrieved successfully.",
  "data": {
    "orders": [],
    "pagination": {
      "currentPage": 0,
      "pageSize": 10,
      "totalOrders": 25,
      "totalPages": 3,
      "firstPage": true,
      "lastPage": false
    }
  },
  "timestamp": "2026-06-06T20:30:45"
}
```

---

### Update Order Status

```
PATCH http://localhost:8080/api/orders/{id}/status?newStatus=SHIPPED
```

Returns `200 OK` with the updated order. Returns `409 Conflict` if the transition is not allowed.

---

### Cancel Order

```
PATCH http://localhost:8080/api/orders/{id}/cancel
```

Only orders in PENDING status can be cancelled. Returns `409 Conflict` for any other status.

---

## Order Status Flow

Every order starts as PENDING and moves forward through the lifecycle. No step can be skipped and no transition can go backwards.

```
PENDING → PROCESSING → SHIPPED → DELIVERED
    │
    └── CANCELLED (only from PENDING)
```

Once an order reaches DELIVERED or CANCELLED it is in a terminal state and cannot be modified further.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/sohna/order_processing/
│   │   ├── controller/        REST endpoints
│   │   ├── service/           Business logic
│   │   ├── repository/        Database queries
│   │   ├── mapper/            Entity and DTO conversion
│   │   ├── model/             JPA entities
│   │   ├── dto/
│   │   │   ├── request/       Incoming request payloads
│   │   │   └── response/      Outgoing response payloads
│   │   ├── exception/         Custom exceptions and global handler
│   │   ├── helper/            Validation and message helpers
│   │   ├── scheduler/         Background job
│   │   └── event/ listener/   Event bus structured and ready to activate
│   └── resources/
│       ├── application.properties
│       └── data.sql           7 seed orders
└── test/
    └── java/com/sohna/order_processing/
        ├── controller/
        ├── mapper/
        ├── scheduler/
        └── service/
```

---

## Architecture

The project follows a strict layered architecture where each layer has one responsibility and never reaches into another layer's concern.

```
Client
  │
  ▼
Controller     Handles HTTP, validates input, shapes the response
  │
  ▼
Service        Owns all business logic, status transitions and calculations
  │
  ├── Repository    Talks to the database only
  ├── Mapper        Converts entities to DTOs and back
  └── Helper        Holds reusable validation and message logic
          │
          ▼
      H2 Database
```

---

## Validation Rules

Every field in the request is validated before it reaches any business logic. Invalid requests are rejected immediately with a clear message telling the client exactly what needs to be fixed.

**Customer Name** must be provided, between 3 and 100 characters, and contain letters only. Numbers and special characters are not allowed.

**Customer Email** must be a valid email address with at least 3 characters before the @ symbol and no more than 100 characters total.

**Idempotency Key** is optional but if provided it must be a valid UUID (e.g. 550e8400-e29b-41d4-a716-446655440000). An empty string is treated the same as not providing a key.

**Product ID** must follow the uppercase hyphen separated catalog format (e.g. APPL-IPH15-001). Lowercase letters and missing hyphens are rejected.

**Product Name** must be between 3 and 100 characters.

**Quantity** must be at least 1 and no more than 100. Orders requiring more than 100 of the same item must be placed as separate orders.

**Product Price** must be greater than zero and must not exceed 7 digits with 2 decimal places.

---

## Database

The application uses an H2 file-based database that persists data between restarts. The database file is stored at `./data/orderdb.mv.db`.

The H2 console is available during development and can be used to inspect tables and run live queries.

```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/orderdb
Username: sa
Password: leave blank
```

---

## API Documentation

Swagger UI is available once the application is running. All endpoints can be explored and tested directly from the browser without needing Postman.

```
http://localhost:8080/swagger-ui/index.html
```

The API docs in JSON format are available at:
```
http://localhost:8080/v3/api-docs
```

---

## Health Check

```
GET http://localhost:8080/actuator/health
```

```json
{ "status": "UP" }
```

---

## Error Reference

| Status | When it happens |
|--------|----------------|
| 400 Bad Request | Validation failure or malformed input |
| 404 Not Found | Order does not exist or has been soft deleted |
| 409 Conflict | Duplicate order, invalid status transition or terminal state update |
| 503 Service Unavailable | Database connection error |
| 500 Internal Server Error | Unexpected server error |

---

## Key Features

**Idempotency Protection**
Each order optionally accepts a UUID key generated by the client. If a network failure causes the client to retry the request with the same key, the system detects it and rejects the duplicate so the customer is never charged twice.

**Soft Delete**
Cancelled orders are never physically removed from the database. They are flagged as deleted so they disappear from normal queries but remain available for audit and customer support purposes.

**Optimistic Locking**
A version field on each order prevents silent data loss when two requests try to update the same order at the same time. The second request gets a 409 Conflict instead of overwriting the first change.

**Strict Status Transitions**
Orders follow a strict one-way lifecycle. Skipping steps, reversing direction and updating terminal states are all rejected with a descriptive error. Only PENDING orders can be cancelled.

**Bulk Scheduler**
A background job runs every 5 minutes and moves all PENDING orders to PROCESSING in a single database query rather than one query per order. If the job encounters an error it logs it and waits for the next cycle without crashing.

**Input Validation**
Every request field is validated before reaching the business logic layer. Customer name format, email structure, product ID catalog format, quantity limits and price precision are all enforced with user friendly error messages.

**Clean Pagination**
List responses return only the pagination fields the client actually needs — current page, page size, total orders and total pages — without the verbose default structure.

**Consistent API Responses**
Every endpoint returns the same response shape with a success flag, message, data and timestamp so the client always knows what to expect regardless of whether the request succeeded or failed.

**Centralized Error Handling**
All exceptions are caught in one place and converted into clean structured responses with the correct HTTP status code. No raw stack traces or inconsistent error formats ever reach the client.

---

## Future Enhancements

**Event Driven Architecture**

The system is already structured to support an event bus and the groundwork is in place. The natural next step is publishing an event after each order operation so downstream systems can react without the order service needing to know about them.

When an order is placed, a notification service can send a confirmation email to the customer. When the status changes, it can send a shipping update or delivery confirmation. An audit service can record every transition with a full timestamp trail. An inventory service can reserve stock on creation and release it on cancellation. All of these happen independently and adding a new one never requires touching the core order logic.

**Production Readiness**

Replacing the embedded database with a production grade relational database is the most important step before going live. Securing endpoints with token based authentication and separating admin and customer facing routes would follow. Rate limiting per customer would protect the system from abuse.

**Scale**

As order volume grows, switching to asynchronous event processing means notifications and inventory updates happen in the background without adding to the order response time. Containerizing the application and deploying to cloud infrastructure requires only configuration changes with no code modifications. Caching frequently accessed orders reduces database load during peak traffic periods.
