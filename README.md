# E-Commerce Microservices

E-Commerce Microservices is a full-stack e-commerce application built using **Spring Boot, React, and a microservices architecture**.

The platform is designed to demonstrate how independent services communicate and work together to handle products, users, orders, payments, and notifications.

The project uses **MySQL for persistent storage, Kafka for event-driven communication, Redis for caching, JWT for authentication, and Docker for containerized deployment**.

## Features

* User registration and authentication
* JWT-based authentication and authorization
* Role-based access control
* Product management
* Product search and browsing
* Shopping cart
* Order management
* Payment processing
* Email/notification service
* Kafka-based event communication
* Redis caching
* MySQL database
* REST APIs
* Dockerized microservices
* React-based frontend

---

# Architecture

The application follows a microservices architecture where each service is responsible for a specific business capability.

```text
                         ┌─────────────────┐
                         │   React UI      │
                         │   Vite          │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   API Gateway   │
                         └────────┬────────┘
                                  │
             ┌────────────────────┼────────────────────┐
             │                    │                    │
             ▼                    ▼                    ▼
      ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
      │ User Service │     │Product       │     │ Order Service│
      │              │     │Service       │     │              │
      └──────────────┘     └──────────────┘     └──────┬───────┘
                                                        │
                                                        ▼
                                               ┌────────────────┐
                                               │ Payment Service │
                                               └───────┬────────┘
                                                       │
                                                       ▼
                                                ┌────────────┐
                                                │   Kafka    │
                                                └─────┬──────┘
                                                      │
                                      ┌───────────────┴───────────────┐
                                      ▼                               ▼
                              ┌───────────────┐               ┌───────────────┐
                              │ Notification  │               │ Other Services│
                              │ Service       │               │               │
                              └───────────────┘               └───────────────┘

                   ┌─────────────┐              ┌─────────────┐
                   │    MySQL    │              │    Redis    │
                   │  Database   │              │    Cache    │
                   └─────────────┘              └─────────────┘
```

---

# Microservices

## User Service

Responsible for user management and authentication.

Responsibilities:

* User registration
* Login
* JWT generation and validation
* Role-based authorization
* User profile management

---

## Product Service

Responsible for product catalog management.

Responsibilities:

* Create products
* Update products
* Delete products
* Get product details
* Product search
* Product categories
* Product inventory

---

## Order Service

Handles the complete order lifecycle.

Responsibilities:

* Create orders
* Manage order items
* Calculate order totals
* Track order status
* Maintain order history
* Communicate with Payment Service

Example flow:

```text
User
 ↓
Create Order
 ↓
Order Service
 ↓
Payment Service
 ↓
Payment Successful
 ↓
Order Confirmed
```

---

## Payment Service

Responsible for payment processing.

Responsibilities:

* Create payment
* Validate payment
* Track payment status
* Handle successful payments
* Handle failed payments
* Publish payment events

Payment events are published through Kafka so other services can react asynchronously.

---

## Notification Service

Responsible for sending notifications to users.

Responsibilities:

* Order confirmation emails
* Payment notifications
* Order status notifications
* Account-related notifications

Example:

```text
Order Service
      │
      ▼
    Kafka
      │
      ▼
Notification Service
      │
      ▼
   Email / Message
```

---

# Infrastructure

## API Gateway

The API Gateway provides a single entry point for clients.

Responsibilities:

* Request routing
* Authentication
* Authorization
* Rate limiting
* Centralized API entry point

```text
React
  │
  ▼
API Gateway
  │
  ├── User Service
  ├── Product Service
  ├── Order Service
  └── Payment Service
```

## Apache Kafka

Kafka is used for asynchronous communication between services.

Example events:

```text
ORDER_CREATED
PAYMENT_SUCCESS
PAYMENT_FAILED
ORDER_CONFIRMED
ORDER_CANCELLED
```

Example:

```text
Order Service
     │
     ▼
 Kafka Topic
     │
     ├──────────► Payment Service
     │
     └──────────► Notification Service
```

This allows services to communicate without being tightly coupled.

---

## Redis

Redis is used as a caching layer to reduce database load and improve response time.

Potential use cases:

* Product caching
* Frequently accessed data
* Session-related data
* Temporary data
* Rate limiting

```text
Client
  │
  ▼
Product Service
  │
  ├──► Redis ──► Data Found
  │
  └──► MySQL ──► Data Not Cached
```

---

## MySQL

MySQL is used for persistent data storage.

Each business service can maintain ownership of its own data, following the **database-per-service** approach where appropriate.

---

# Communication

The application uses both synchronous and asynchronous communication.

### REST Communication

Used when an immediate response is required.

```text
Frontend
   │
   ▼
API Gateway
   │
   ▼
Microservice
   │
   ▼
Response
```

### Event-Driven Communication

Kafka is used for asynchronous communication.

```text
Service A
   │
   ▼
 Kafka
   │
   ├──► Service B
   │
   └──► Service C
```

---

# Authentication & Security

The application uses **Spring Security and JWT** for securing APIs.

Authentication flow:

```text
User
 ↓
Login
 ↓
User Service
 ↓
JWT Token
 ↓
React
 ↓
Authenticated Request
 ↓
API Gateway
 ↓
Microservice
```

Security features:

* JWT authentication
* Role-based authorization
* Password hashing
* Protected REST APIs
* Token validation

---

# Technology Stack

| Category         | Technology                 |
| ---------------- | -------------------------- |
| Backend          | Java 21, Spring Boot       |
| Microservices    | Spring Boot                |
| Security         | Spring Security, JWT       |
| Frontend         | React, TypeScript, Vite    |
| Database         | MySQL                      |
| ORM              | Spring Data JPA, Hibernate |
| Messaging        | Apache Kafka               |
| Caching          | Redis                      |
| Gateway          | Spring Cloud Gateway       |
| Containerization | Docker, Docker Compose     |
| Build Tool       | Maven                      |
| Testing          | JUnit, Mockito             |
| API              | REST                       |

---

# Project Structure

```text
ecommerce-microservices/
│
├── ecommerce-platform-server/
│
├── product-service/
│
├── order-service/
│
├── payment-service/
│
├── notification-service/
│
├── user-service/
│
├── api-gateway/
│
├── frontend/
│
├── docker-compose.yml
│
└── pom.xml
```

---

# Prerequisites

Make sure the following are installed:

* Java 21
* Maven 3.9+
* Node.js
* Docker
* Docker Compose
* Git

---

# Quick Start

Clone the repository:

```bash
git clone https://github.com/Chetanc99/ecommerce-microservices.git
```

Navigate to the project:

```bash
cd ecommerce-microservices
```

Build the project:

```bash
mvn clean install
```

Start the application:

```bash
docker compose up --build
```

---

# Development

### Backend

Spring Boot services can be started independently during development.

```bash
mvn spring-boot:run
```

### Frontend

Install dependencies:

```bash
npm install
```

Start the React development server:

```bash
npm run dev
```

Vite provides Hot Module Replacement (HMR) for faster frontend development.

---

# Docker

Build and start all services:

```bash
docker compose up --build
```

Stop the services:

```bash
docker compose down
```

Stop services and remove volumes:

```bash
docker compose down -v
```

> **Warning:** Removing volumes can delete persistent database data.

---

# Order Processing Flow

A typical order flow is:

```text
Customer
   │
   ▼
React Frontend
   │
   ▼
API Gateway
   │
   ▼
Order Service
   │
   ▼
Payment Service
   │
   ▼
Kafka
   │
   ├──────────────► Notification Service
   │
   └──────────────► Order Service
                         │
                         ▼
                   Order Confirmed
```

This architecture demonstrates:

* Service isolation
* REST-based communication
* Event-driven architecture
* Asynchronous processing
* Distributed data management
* Caching
* Authentication and authorization

---

# Future Improvements

* Kubernetes deployment
* Service discovery
* Spring Cloud Config
* Resilience4j circuit breakers
* Prometheus monitoring
* Grafana dashboards
* Distributed tracing
* ELK centralized logging
* CI/CD pipeline
* AWS cloud deployment
* Elasticsearch-based product search

---
