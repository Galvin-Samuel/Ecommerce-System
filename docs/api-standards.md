# Scalable E-Commerce System — REST API Standards

This document establishes the architecture, conventions, status code usage, and response standards for all RESTful APIs developed across the 20 phases of the Scalable E-Commerce System.

---

## 1. URL Structure and Versioning Conventions

### 1.1 API Versioning
All business APIs are versioned via the URI path to allow evolutionary changes without breaking backward compatibility:

```text
/api/v1/{resource}
```

* **Current API version**: `v1`
* **Operational & Infrastructure Endpoints**: Infrastructure health and diagnostic endpoints are intentionally maintained outside versioned business paths:
  * `/api/health` — Process liveness probe
  * `/api/health/dependencies` — Database and cache readiness probe

### 1.2 Resource Naming Conventions
* **Plural Nouns**: Resource endpoints must use plural nouns representing collections (e.g., `/api/v1/products`, `/api/v1/users`, `/api/v1/orders`).
* **kebab-case URLs**: Multi-word URI segments must use lowercase kebab-case (e.g., `/api/v1/cart-items`).
* **Resource Hierarchy**: Sub-resources express natural ownership relationships:
  * `/api/v1/users/{userId}/orders` — Orders belonging to a specific user.
* **CRUD Verb Mapping**: Never put action verbs in URIs. Use standard HTTP methods:
  * `GET /api/v1/products` — List products
  * `POST /api/v1/products` — Create product
  * `GET /api/v1/products/{id}` — Get single product
  * `PUT /api/v1/products/{id}` — Full update of product
  * `PATCH /api/v1/products/{id}` — Partial update of product
  * `DELETE /api/v1/products/{id}` — Remove product

---

## 2. Standard Success Response Envelope

For uniform client integration, successful business API endpoints return the generic `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-10-21T06:30:00.000Z"
}
```

### Fields
* **`success`** (`boolean`): Always `true` for successful operations.
* **`message`** (`string`): Consumer-friendly summary of the outcome.
* **`data`** (`object` / `array` / `primitive`): Payload containing the requested entity, collection, or operation result.
* **`timestamp`** (`string`): UTC ISO-8601 timestamp (`yyyy-MM-dd'T'HH:mm:ss.SSSXXX`).

---

## 3. Standard Error Response Format

All API errors caught by the centralized `GlobalExceptionHandler` return a structured `ApiErrorResponse`:

```json
{
  "timestamp": "2026-10-21T06:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed. Please check field errors.",
  "path": "/api/v1/products",
  "validationErrors": [
    {
      "field": "price",
      "rejectedValue": -10.0,
      "message": "Price cannot be negative"
    },
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Product name is required"
    }
  ]
}
```

### Fields
* **`timestamp`** (`string`): UTC ISO-8601 timestamp of when the error occurred.
* **`status`** (`integer`): HTTP status code matching the response header.
* **`error`** (`string`): Official HTTP status reason phrase (e.g., `"Bad Request"`, `"Not Found"`).
* **`message`** (`string`): Human-readable error description explaining what went wrong.
* **`path`** (`string`): Request URI that produced the error.
* **`validationErrors`** (`array`, optional): Present only when request validation fails. Contains field-level details:
  * `field`: The property that failed validation.
  * `rejectedValue`: The invalid value provided in the request (omitted if sensitive).
  * `message`: Specific validation error message.

---

## 4. HTTP Status Code Conventions

The system uses standard HTTP semantics to convey the outcome of requests. Every status code is chosen deliberately:

| Status Code | Meaning | When to Use |
|---|---|---|
| **`200 OK`** | Request succeeded | Standard successful `GET`, `PUT`, or `PATCH` operation returning data. |
| **`201 Created`** | Resource created | Successful `POST` creating an entity (returns the created resource or ID). |
| **`204 No Content`** | Action succeeded, no body | Successful `DELETE` or action where no response body is needed. |
| **`400 Bad Request`** | Malformed client request | Invalid input syntax, malformed JSON, or missing required query parameters. |
| **`401 Unauthorized`** | Authentication required | Missing, expired, or invalid credentials/JWT (Phases 5+). |
| **`403 Forbidden`** | Insufficient permissions | Authenticated user lacks authorization for the resource (e.g., customer accessing seller actions). |
| **`404 Not Found`** | Resource does not exist | Specified resource identifier or endpoint URL not found. |
| **`409 Conflict`** | Resource state conflict | Uniqueness constraint violations (e.g., registering duplicate email) or concurrency conflicts. |
| **`422 Unprocessable Content`** | Business validation failure | Syntactically valid request that violates domain rules (e.g., ordering more items than available stock). |
| **`429 Too Many Requests`** | Rate limit exceeded | Client exceeded token bucket or sliding window rate limit (Phase 10). |
| **`500 Internal Server Error`** | Unexpected server failure | Uncaught exception. Generic safe message returned; stack traces are never exposed. |
| **`503 Service Unavailable`** | Dependency / subsystem down | External dependency unavailable (e.g., MongoDB/Redis down in `/api/health/dependencies`). |

---

## 5. Pagination, Sorting, and Filtering Conventions

Future collection listing endpoints (Products, Orders, Users) adhere to the following standard query parameter conventions:

### 5.1 Pagination
* **`page`**: 0-indexed page number (default: `0`).
* **`size`**: Number of items per page (default: `20`, maximum: `100`).
* **Standard Paginated Data Structure**:
  ```json
  {
    "items": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
  ```

### 5.2 Sorting
* **`sort`**: Formatted as `fieldName,direction` where direction is `asc` or `desc`.
  * Example: `GET /api/v1/products?sort=price,asc`
  * Multiple sorts: `GET /api/v1/products?sort=category,asc&sort=createdAt,desc`

### 5.3 Filtering
* Use query parameters matching entity properties:
  * Example: `GET /api/v1/products?category=Electronics&minPrice=10&maxPrice=100`

---

## 6. Security & Safe Error Handling Principles

1. **No Technical Leaks**: Never expose database connection strings, passwords, API tokens, internal class paths, or Java stack traces to API consumers.
2. **Explicit Non-200 Responses**: Never return HTTP 200 with an internal `error` flag for failed operations. Errors must always map to proper 4xx/5xx status codes.
3. **Safe Rejection Echoing**: Passwords or cryptographic secrets must never be echoed back in `rejectedValue` fields of validation errors.

---

## 7. Postman Collection & Testing

An importable Postman collection and local environment are located in the `testing/` directory:

* Collection: [`testing/ecommerce-api.postman_collection.json`](file:///c:/ECommerce-System/testing/ecommerce-api.postman_collection.json)
* Environment: [`testing/ecommerce-local.postman_environment.json`](file:///c:/ECommerce-System/testing/ecommerce-local.postman_environment.json)

### Importing into Postman
1. Open Postman.
2. Click **Import** (top left).
3. Select both `testing/ecommerce-api.postman_collection.json` and `testing/ecommerce-local.postman_environment.json`.
4. In the top-right environment selector, select **ECommerce Local Environment**.
5. Execute requests against `{{baseUrl}}` (defaults to `http://localhost:8080`).

