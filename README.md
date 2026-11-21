# NovaCart — E-Commerce Microservices Platform

A portfolio-grade e-commerce platform built as a set of Spring Boot microservices behind an
API Gateway, with a React storefront, Razorpay test-mode payments, Kafka eventing, Redis
caching, per-service MySQL schemas, Flyway migrations, and Docker Compose.

Every request from the browser goes through the gateway (`/api/**`). Services never read
another service's tables; they collaborate over HTTP (internal endpoints) and Kafka events.

---

## 1. Status at a Glance

| Area | State |
|---|---|
| Auth/User Service | **IMPLEMENTED AND TESTED** — register, login, BCrypt, JWT, USER/ADMIN, profile, password change |
| Product Service | **IMPLEMENTED AND TESTED** — CRUD, search, category filter, pagination/sorting, Redis cache + eviction, ADMIN-only writes, internal stock reserve/release |
| Order Service | **IMPLEMENTED AND TESTED** — multi-item orders, server-side totals, stock reservation, ownership checks, cancel, status transitions, transactional outbox |
| Payment Service | **IMPLEMENTED AND TESTED** (unit/integration); **PENDING** real Razorpay sandbox E2E (needs test credentials) |
| Notification Service | **IMPLEMENTED AND TESTED** — Kafka consumers for 4 events, SMTP sending, idempotency, failure logging |
| API Gateway | **IMPLEMENTED AND TESTED** — routing, JWT validation, public/protected rules, CORS, consistent errors |
| Kafka | **IMPLEMENTED AND TESTED** — unit tests + live broker; transactional outbox event published to `order.created` in the smoke run |
| Redis | **IMPLEMENTED AND TESTED** cache hit/evict with graceful degradation; live container healthy |
| MySQL + Flyway | **IMPLEMENTED AND TESTED** — migrations applied successfully to all five live MySQL schemas with `ddl-auto=validate` |
| React Frontend | **IMPLEMENTED AND TESTED** — `npm run build` succeeds; browser E2E against running stack **PENDING** |
| Docker Compose | **IMPLEMENTED AND TESTED** — full stack starts healthy; live register/login/product/order/outbox smoke verified |

Full detail and honest limitations: [Section 12](#12-implementation-classification).

---

## 2. Architecture

```text
                        ┌───────────────────────────────┐
   Browser (React 19)   │  nginx :3000  /  Vite :5173    │
                        └───────────────┬───────────────┘
                                        │ HTTP /api/**
                                        ▼
                        ┌───────────────────────────────┐
                        │   API Gateway   :8081         │
                        │   JWT validation + routing    │
                        │   CORS + X-Correlation-Id     │
                        └──┬────────┬─────────┬─────┬───┘
                           │        │         │     │
              ┌────────────▼─┐ ┌────▼──────┐ ┌▼───────────┐ ┌▼──────────────┐
              │ Auth/User    │ │ Product   │ │ Order      │ │ Payment       │
              │ :8080        │ │ :8082     │ │ :8083      │ │ :8084         │
              │ JWT issuer   │ │ Redis     │ │ outbox     │ │ Razorpay      │
              └──────┬───────┘ └────┬──────┘ └────┬───────┘ └────┬──────────┘
                     │              │             │              │
                     ▼              ▼             ▼              ▼
                ecommerce_db   product_db     order_db       payment_db
                                     ▲              │
                                     └── internal stock reserve/release (HTTP)
                                                    │
                        ┌───────────────────────────┴────────────────┐
                        │ Kafka (KRaft) :9092  /  host :29092        │
                        │ order.created, payment.initiated,          │
                        │ payment.successful, payment.failed,        │
                        │ order.confirmed, order.cancelled           │
                        └───────────────────────┬────────────────────┘
                                                ▼
                                   ┌──────────────────────────┐
                                   │ Notification Service     │
                                   │ :8085  -> SMTP/MailHog   │
                                   │ notification_db          │
                                   └──────────────────────────┘
```

### Key design decisions

- **One schema per service.** `ecommerce_db`, `product_db`, `order_db`, `payment_db`,
  `notification_db`. No cross-schema joins.
- **Gateway is the only public entry point.** It validates the JWT, strips spoofed identity
  headers, injects trusted `X-User-Id` / `X-User-Email` / `X-User-Roles`, and forwards.
- **Internal endpoints are not routed by the gateway.** `/internal/**` is reachable only on
  the Docker/service network and requires `X-Internal-Api-Key`.
- **Transactional outbox.** Order and Payment write events to an outbox table in the same
  transaction as the state change; a scheduled publisher sends them to Kafka. A rolled-back
  transaction therefore produces no event.
- **Idempotent consumers.** Processed event/webhook IDs are stored so redeliveries are safe.
- **Server-side money.** The client sends only `productId` + `quantity`. Prices, item totals,
  and order totals are computed from Product Service data.

---

## 3. Technology Stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot **4.0.9** |
| Cloud | Spring Cloud **2025.1.4** (Gateway `spring-cloud-starter-gateway-server-webflux` 5.0.4) |
| Security | Spring Security 7, JJWT 0.12.6 (HS256) |
| Persistence | Spring Data JPA / Hibernate 7, MySQL 8.4, Flyway 11 |
| Cache | Redis 7.4 (Spring Cache) |
| Messaging | Apache Kafka 3.9.1 (KRaft), Spring Kafka |
| Payments | Razorpay Java SDK 1.4.10 (test mode) + manual HMAC-SHA256 verification |
| Mail | Spring Mail + MailHog for local SMTP |
| Frontend | React 19, Vite 7, React Router 7, plain CSS |
| Build | Maven 3.9 multi-module reactor |
| Containers | Docker, Docker Compose |

> Spring Boot 4.0.9 is pinned because Spring Cloud 2025.1.4 targets the 4.0.x line.
> Kafka auto-configuration in Boot 4 comes from `spring-boot-starter-kafka`.

---

## 4. Repository Layout

```text
ecommerce-microservices/
├── pom.xml                          # Parent reactor (9 modules)
├── docker-compose.yml               # MySQL, Redis, Kafka, MailHog, 5 services, gateway, frontend
├── Dockerfile                       # Shared parameterised backend image (--build-arg MODULE=...)
├── .env.example                     # Every supported environment variable
├── docs/
│   └── NovaCart.postman_collection.json
├── docker/mysql-init/               # Creates the five schemas + app user on first boot
├── ecommerce-common-security/       # JwtService, JwtPrincipal, JwtConfiguration
├── ecommerce-common-events/         # KafkaTopics + event records + OrderItemPayload
├── ecommerce-platform-server/       # Auth/User service            :8080
├── ecommerce-product-service/       # Product service + Redis      :8082
├── ecommerce-order-service/         # Order service + outbox       :8083
├── ecommerce-payment-service/       # Payment service + outbox     :8084
├── ecommerce-notification-service/  # Kafka -> SMTP notifications  :8085
├── ecommerce-api-gateway/           # Spring Cloud Gateway         :8081
└── frontend/                        # React + Vite storefront      :5173 / :3000
```

Each service follows `controller -> service -> repository` with constructor injection, DTOs,
Bean Validation, and `@RestControllerAdvice` problem details.

---

## 5. Feature Summary

### Auth / User Service
- Registration with case-insensitive duplicate-email rejection; role forced to `USER`.
- BCrypt password hashing; password hashes never appear in responses.
- Login returns `{ token, userId, name, email, role }`.
- JWT claims: `sub` (user id), `email`, `roles`, `iss`, `aud`, `jti`, `iat`, `exp`.
- `GET/PUT /api/users/me`, `PUT /api/users/me/password`.
- Optional startup bootstrap admin via `ADMIN_EMAIL` + `ADMIN_PASSWORD`.
- Correct `401` (missing/invalid token) vs `403` (role denied).

### Product Service
- Public catalog reads: list (paged/sorted), get by id, category filter, name search.
- ADMIN-only create/update/delete.
- Stock updates with optimistic locking; over-reservation returns `409`.
- Redis cache (`products`, 10-minute TTL, `ecommerce:` prefix) with automatic eviction on
  every mutation; Redis errors degrade to DB reads instead of failing requests.
- Internal `POST /internal/products/{id}/reserve` and `/release` guarded by
  `X-Internal-Api-Key`.

### Order Service
- `POST /api/orders` with `{ "items": [{ "productId": 1, "quantity": 2 }] }`.
- Validates availability, reserves stock, computes totals from Product Service prices,
  saves `PENDING_PAYMENT` + an `OrderCreated` outbox event.
- Lists only the current user's orders; detail and cancel enforce ownership (ADMIN allowed).
- Cancel releases reserved stock and emits `OrderCancelled`; unpaid-only, idempotent.
- Payment events move orders to `CONFIRMED` (`PaymentSuccessful`) or `PAYMENT_FAILED`
  (`PaymentFailed`), releasing stock on failure.

### Payment Service
- Creates a Razorpay order server-side and stores a `CREATED` payment row linked to the order.
- Refuses to charge an already-paid order; reuses an unfinished attempt.
- Verifies `razorpay_signature = HMAC_SHA256(order_id|payment_id, key_secret)` before any
  status change.
- Webhook endpoint verifies `X-Razorpay-Signature` against `RAZORPAY_WEBHOOK_SECRET` and is
  idempotent per `X-Razorpay-Event-Id`.
- Emits `PaymentInitiated`, `PaymentSuccessful`, `PaymentFailed` through the outbox.
- Payment status is visible only to its owner. Returns `503` when credentials are missing —
  the UI never fakes success.

### Notification Service
- Consumes `OrderConfirmed`, `PaymentSuccessful`, `PaymentFailed`, `OrderCancelled`.
- Sends SMTP e-mail (MailHog locally); logs `SENT` / `SKIPPED` / `FAILED` per event id.
- Duplicate deliveries are skipped; errors are truncated to the column length.

### API Gateway
- Routes: `/api/users/**`, `/api/products/**`, `/api/orders/**`, `/api/payments/**`.
- Public: `POST /api/users/register|login`, `GET /api/products/**`,
  `POST /api/payments/webhook`, `/actuator/health|info`. Everything else needs a bearer token.
- Adds `X-Correlation-Id`; strips client-supplied identity headers.
- Unified `application/problem+json` errors for `401` and unavailable-downstream cases.

### Frontend (NovaCart)
- Product grid with search, category filter, pagination, sorting.
- Product detail, quantity-aware cart persisted in `localStorage`.
- Register / login, protected routes, profile + password change.
- Checkout creates the order, opens Razorpay Checkout, verifies the signature server-side,
  then shows the order.
- Order history, order detail, cancel, payment status page.
- Admin product management (`/admin/products`) gated by the `ADMIN` role.
- Loading, empty, and error states. All API traffic goes to `/api` (gateway).

---

## 6. REST API Reference

Base URL through the gateway: `http://localhost:8081` (frontend dev/prod proxy uses `/api`).

### Auth / User (`/api/users`)

| Method | Path | Auth | Body / notes |
|---|---|---|---|
| POST | `/api/users/register` | public | `{ "name", "email", "password" }` → `201` `{ id, name, email, role }` |
| POST | `/api/users/login` | public | `{ "email", "password" }` → `200` `{ token, userId, name, email, role }` |
| GET | `/api/users/me` | bearer | Current profile |
| PUT | `/api/users/me` | bearer | `{ "name", "email" }` |
| PUT | `/api/users/me/password` | bearer | `{ "currentPassword", "newPassword" }` → `204` |

### Products (`/api/products`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/products?page=0&size=12&sort=createdAt,desc` | public | Paged `{ content, page, size, totalElements, totalPages, first, last }` |
| GET | `/api/products/{id}` | public | `200` or `404` |
| GET | `/api/products/category/{category}?page=&size=&sort=` | public | Case-insensitive category |
| GET | `/api/products/search?name=key&page=&size=&sort=` | public | Partial name search |
| POST | `/api/products` | ADMIN | `{ "name", "description", "price", "quantity", "category", "imageUrl" }` → `201` |
| PUT | `/api/products/{id}` | ADMIN | Same body |
| DELETE | `/api/products/{id}` | ADMIN | `204` |

### Orders (`/api/orders`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/orders` | bearer | `{ "items": [{ "productId", "quantity" }] }` → `201` `PENDING_PAYMENT` |
| GET | `/api/orders?page=0&size=10&sort=createdAt,desc` | bearer | Current user's orders only |
| GET | `/api/orders/{orderId}` | bearer | Owner or ADMIN |
| PUT | `/api/orders/{orderId}/cancel` | bearer | Owner; `PENDING_PAYMENT`/`PAYMENT_FAILED` only |

Status values: `PENDING_PAYMENT`, `CONFIRMED`, `PAYMENT_FAILED`, `CANCELLED`.

### Payments (`/api/payments`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/payments/orders` | bearer | `{ "orderId" }` → `201` `{ paymentId, orderId, razorpayOrderId, razorpayKeyId, amount, currency }` |
| POST | `/api/payments/verify` | bearer | `{ razorpayOrderId, razorpayPaymentId, razorpaySignature }` → verified status |
| GET | `/api/payments/order/{orderId}` | bearer | Owner's payment status |
| POST | `/api/payments/webhook` | Razorpay signature | Raw body; headers `X-Razorpay-Signature`, `X-Razorpay-Event-Id` |

### Internal (not routed by the gateway)

| Method | Path | Guard | Notes |
|---|---|---|---|
| POST | `/internal/products/{id}/reserve` | `X-Internal-Api-Key` | `{ "quantity": n }`; `409` when short |
| POST | `/internal/products/{id}/release` | `X-Internal-Api-Key` | `{ "quantity": n }` |
| GET | `/internal/orders/{orderId}` | `X-User-Id` header | Order summary for Payment Service |

### Health

Every service exposes `GET /actuator/health` and `GET /actuator/info`.

---

## 7. Kafka Topics and Events

| Topic | Producer | Consumer | Payload (record) |
|---|---|---|---|
| `order.created` | Order | — | `OrderCreatedEvent` (items snapshot) |
| `payment.initiated` | Payment | — | `PaymentInitiatedEvent` |
| `payment.successful` | Payment | Order, Notification | `PaymentSuccessfulEvent` |
| `payment.failed` | Payment | Order, Notification | `PaymentFailedEvent` |
| `order.confirmed` | Order | Notification | `OrderConfirmedEvent` |
| `order.cancelled` | Order | Notification | `OrderCancelledEvent` |

- JSON payloads with Spring Kafka type headers (`com.ecommerce.common.events`).
- `occurredAt` is an ISO-8601 `String` so payloads stay serialization-stable.
- Consumers retry with a `DefaultErrorHandler` (`FixedBackOff`, 2 s × 3) and route exhausted
  records to `<topic>.DLT`.
- Deduplication: `processed_event` / `processed_webhook` / `notification_log` tables.

---

## 8. Configuration

Copy the template and fill it in:

```powershell
Copy-Item .env.example .env
```

```bash
cp .env.example .env
```

| Variable | Required | Purpose |
|---|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | yes | MySQL application user |
| `DB_ROOT_PASSWORD` | Docker only | MySQL root password for the container |
| `DB_URL` | no | Fallback DB URL for **all** services; leave empty locally so each uses its own schema |
| `AUTH_DB_URL` / `PRODUCT_DB_URL` / `ORDER_DB_URL` / `PAYMENT_DB_URL` / `NOTIFICATION_DB_URL` | Docker only | Per-service DB URL overrides; take precedence over `DB_URL` |
| `JWT_SECRET` | yes | HS256 key, ≥ 32 bytes |
| `JWT_EXPIRATION_MS` | no | Token lifetime (default 1 h) |
| `JWT_ISSUER` / `JWT_AUDIENCE` | no | Defaults `ecommerce-auth` / `ecommerce-api` |
| `INTERNAL_API_KEY` | yes | Shared key for `/internal/**` |
| `CORS_ALLOWED_ORIGINS` | no | Comma-separated origins |
| `AUTH_SERVICE_URL` / `PRODUCT_SERVICE_URL` / `ORDER_SERVICE_URL` / `PAYMENT_SERVICE_URL` | no | Gateway upstreams; service-to-service URLs |
| `KAFKA_BOOTSTRAP_SERVERS` | no | Defaults to `localhost:9092` |
| `KAFKA_LISTENERS_ENABLED` | no | `false` to start without consumers |
| `OUTBOX_ENABLED` | no | `false` to disable scheduled publishers |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | no | Product cache |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | for payments | **Test-mode** keys |
| `RAZORPAY_WEBHOOK_SECRET` | for webhooks | Webhook signing secret |
| `RAZORPAY_CURRENCY` | no | Default `INR` |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | no | SMTP (MailHog defaults) |
| `MAIL_SMTP_AUTH` / `MAIL_STARTTLS` / `MAIL_ENABLED` / `MAIL_FROM` | no | Mail behaviour |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | no | Bootstrap admin |
| `JPA_SHOW_SQL` | no | Debug SQL only |
| `VITE_API_BASE_URL` | no | Empty = same-origin `/api` |
| `VITE_GATEWAY_URL` | dev only | Vite proxy target (loaded with `loadEnv`) |

Docker Compose reads this file twice: every key is passed into the backend containers via
`env_file`, and explicit `environment:` entries use the same values through Compose
interpolation. Local Maven runs load the file through Spring's `config.import`.

> **Security:** rotate `JWT_SECRET` and any previously committed credentials before
> deployment. Old values remain in Git history.

### Databases and migrations

Flyway creates the schema (`ddl-auto=validate` in all services):

```text
ecommerce-platform-server/src/main/resources/db/migration/V1__create_users.sql
ecommerce-product-service/src/main/resources/db/migration/V1__create_products.sql
ecommerce-order-service/src/main/resources/db/migration/V1__create_order_schema.sql
ecommerce-payment-service/src/main/resources/db/migration/V1__create_payment_schema.sql
ecommerce-notification-service/src/main/resources/db/migration/V1__create_notification_log.sql
```

Create the schemas manually for non-Docker MySQL:

```sql
CREATE DATABASE ecommerce_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE product_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE order_db      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE payment_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 9. Build and Test

From the repository root:

```bash
mvn clean verify
```

**Last verified result (this workspace): `BUILD SUCCESS`, 162 tests, 0 failures, 0 errors.**

| Module | Tests | Notes |
|---|---|---|
| `ecommerce-common-security` | 5 | JWT issue/parse, claims, tamper/expiry rejection |
| `ecommerce-platform-server` | 23 | Register/login/duplicate/validation, profile, RBAC, API integration |
| `ecommerce-product-service` | 26 | CRUD, search/category/paging, stock reserve/release, Redis cache, security integration |
| `ecommerce-order-service` | 31 | Creation/totals/stock, ownership, cancel, payment handlers, outbox, event payloads |
| `ecommerce-payment-service` | 51 | Razorpay HMAC vectors, webhook parse/verify, settlement, duplicates, outbox |
| `ecommerce-notification-service` | 11 | Idempotency, skip/fail/sent logging, e-mail content per event |
| `ecommerce-api-gateway` | 15 | Public-route matcher, JWT filter headers/401s, routing + downstream errors |

Tests use the `test` profile (in-memory H2, Flyway off, Kafka listeners off, simple cache).
Run a single module:

```bash
mvn -pl ecommerce-product-service -am test
```

Build without tests:

```bash
mvn -DskipTests package
```

---

## 10. Running Locally

### Option A — Docker Compose (recommended)

Requires Docker Desktop with the daemon running.

```bash
docker compose up --build
```

| URL | What |
|---|---|
| http://localhost:3000 | NovaCart frontend |
| http://localhost:8081 | API Gateway |
| http://localhost:8025 | MailHog inbox |
| http://localhost:3307 | MySQL (host port; root / `DB_ROOT_PASSWORD`) |
| localhost:29092 | Kafka external listener |
| localhost:6379 | Redis |

All containers have health checks and start in dependency order. Data lives in the
`mysql-data`, `redis-data`, `kafka-data` volumes.

Stop: `docker compose down` — wipe data: `docker compose down -v`.

### Option B — Run services from the IDE / Maven

Start MySQL, Redis, Kafka and MailHog (or use Compose for just the infrastructure):

```bash
docker compose up mysql redis kafka mailhog
```

Then in separate terminals:

```bash
mvn -pl ecommerce-platform-server      spring-boot:run
mvn -pl ecommerce-product-service      spring-boot:run
mvn -pl ecommerce-order-service        spring-boot:run
mvn -pl ecommerce-payment-service      spring-boot:run
mvn -pl ecommerce-notification-service spring-boot:run
mvn -pl ecommerce-api-gateway          spring-boot:run
```

Frontend:

```bash
cd frontend
cmd /c npm install      # Windows: npm.ps1 may be blocked
cmd /c npm run dev      # http://localhost:5173, proxies /api -> gateway
```

Production build check:

```bash
cmd /c npm run build
```

### Bootstrap an ADMIN user

Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` in `.env` and restart the auth service; the account
is created or promoted on startup.

---

## 11. Razorpay Test Mode — Setup and End-to-End Procedure

1. Create a Razorpay account and switch the dashboard to **Test Mode**.
2. **Account & Settings → API Keys → Generate Test Key.** Put the values in `.env`:
   `RAZORPAY_KEY_ID=rzp_test_...`, `RAZORPAY_KEY_SECRET=...`.
3. For webhooks: **Account & Settings → Webhooks → Add**:
   - URL: `https://<public-tunnel>/api/payments/webhook` (e.g. `ngrok http 8081`)
   - Secret: the value you put in `RAZORPAY_WEBHOOK_SECRET`
   - Events: `payment.captured`, `payment.failed`, `order.paid`
4. Restart the payment service so it picks up the variables.

**Browser procedure**

1. Register and log in.
2. Add products to the cart and check out.
3. `POST /api/payments/orders` creates the Razorpay order; Razorpay Checkout opens with the
   public key id only.
4. Pay with a Razorpay test card, e.g. card `4111 1111 1111 1111`, any future expiry, CVV
   `123`, OTP `1234` (test mode).
5. The frontend posts the checkout response to `/api/payments/verify`. The backend recomputes
   `HMAC_SHA256(order_id|payment_id, key_secret)`; only a match marks the payment
   `SUCCESS` and emits `PaymentSuccessful`, which confirms the order and e-mails the buyer.
6. A failed/cancelled payment leaves a retryable order; `payment.failed` / dismissal marks it
   `PAYMENT_FAILED` and releases stock.

**Webhook smoke test** (after the service is configured):

```bash
curl -X POST http://localhost:8081/api/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Razorpay-Signature: <computed-hmac>" \
  -H "X-Razorpay-Event-Id: evt_demo_1" \
  --data '{"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_demo","order_id":"order_demo"}}}}'
```

A wrong or missing signature is rejected; a repeated event id is acknowledged without
re-processing.

> No real money is charged. Only test keys are supported; there is no fake "success" button
> and the frontend never receives the key secret or webhook secret.

---

## 12. Implementation Classification

### IMPLEMENTED AND TESTED
- JWT issue/verify (claims, expiry, tampering), BCrypt hashing, duplicate-email handling,
  `401` vs `403`.
- User registration/login/profile/password change via API integration tests.
- Product CRUD, category/search/pagination, ADMIN authorization, validation errors.
- Redis cache population and eviction on create/update/delete/reserve/release.
- Order creation totals, stock validation/reservation/release, ownership, cancel rules,
  status transitions, outbox records, event payload shape.
- Payment HMAC verification (known vectors), webhook parsing/verification, duplicate webhook
  idempotency, settlement transitions, outbox records.
- Notification idempotency and e-mail content for all four events.
- Gateway public-route rules, JWT filter header injection, routing, downstream `503`.
- Full reactor build and all 162 tests.
- Frontend production build.
- Docker Compose full-stack startup: all 11 containers healthy; gateway and frontend return HTTP 200.
- Live MySQL Flyway migrations in `ecommerce_db`, `product_db`, `order_db`, `payment_db`,
  and `notification_db` with `ddl-auto=validate`.
- Live gateway smoke: register/login → ADMIN product create → order create; stock reserved
  10→8 and the `OrderCreated` outbox event published to `order.created`.

### IMPLEMENTED BUT NOT FULLY TESTED
- Razorpay test-mode checkout and webhook E2E (needs sandbox credentials).
- Browser-level E2E of the full purchase flow.
- SMTP delivery to a real mailbox (MailHog is running but no message was asserted live).
- Redis cache hit/evict measured against the live server (container is healthy and wired;
  cache semantics are covered by integration tests with the test provider).

### PENDING / OUT OF SCOPE
- Razorpay **live** mode (deliberately unsupported; test mode only).
- Real refunds after a paid order is cancelled (`refund` status is not implemented; the
  cancellation e-mail says the payment "will be reviewed for refund").
- Shipping address, tax, inventory reservation expiry, and scheduled reconciliation.
- Distributed tracing (OpenTelemetry), rate limiting, and production observability.
- CI pipeline and load/performance testing.
- Automated `ddl-auto=update` removal for pre-existing dev databases (clean dev DB or
  Flyway baseline is required when adopting this on an old schema).

### Known limitations
- The customer can pay, then cancel only unpaid orders; refunds are manual.
- Stock reservations have no timeout: an abandoned unpaid order keeps stock reserved until
  the payment fails or the order is cancelled. A sweeper job is future work.
- Kafka topics are auto-created; production should pre-create them with explicit partitions
  and replication.
- The gateway validates JWTs and forwards identity; it does not call the auth service for
  revocation checks (stateless tokens).
- Concurrent payment creation is serialized within one Payment Service instance, so a
  double-click reuses one Razorpay order. Multiple replicas would need a database-level
  single-active-payment constraint.

---

## 13. Postman Collection

`docs/NovaCart.postman_collection.json` contains the happy path plus negative cases:
register, login, profile, product reads/writes, order create/list/detail/cancel, payment
create/verify/status/webhook. Set `baseUrl` to `http://localhost:8081` and run **Login**
first — it stores the bearer token in the collection variable `token`.

---

## 14. License

No license has been added yet.
