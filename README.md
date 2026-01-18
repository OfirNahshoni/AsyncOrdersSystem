# AsyncOrdersSystem

A microservices-based order management system built with Kotlin, Spring Boot 4, and PostgreSQL. This project demonstrates modern backend architecture patterns including event-driven communication with Kafka.

> ⚠️ **Work in Progress** - This project is actively being developed as part of a backend engineering portfolio.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        AsyncOrdersSystem                         │
├─────────────────┬─────────────────┬─────────────────────────────┤
│  orders-service │ products-service│   notifications-service     │
│     :8081       │      :8082      │          :8083              │
├─────────────────┼─────────────────┼─────────────────────────────┤
│   orders_db     │   products_db   │      notifications_db       │
│  (PostgreSQL)   │   (PostgreSQL)  │       (PostgreSQL)          │
└────────┬────────┴────────┬────────┴──────────────┬──────────────┘
         │                 │                       │
         └─────────────────┴───────────────────────┘
                           │
                     Apache Kafka
                  (Event Messaging)
```

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.2 |
| Framework | Spring Boot 4.0 |
| Build Tool | Gradle (Kotlin DSL) |
| Database | PostgreSQL 16 |
| ORM | Hibernate / Spring Data JPA |
| Messaging | Apache Kafka |
| Containerization | Docker & Docker Compose |
| Orchestration | Kubernetes (planned) |

## 📁 Project Structure

```
AsyncOrdersSystem/
├── orders-service/           # Handles order creation and management
│   ├── src/main/kotlin/
│   │   └── com/ofir/orders_service/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── entity/
│   │       └── dto/
│   └── src/main/resources/
│       └── application.yml
├── products-service/         # Product catalog management (CRUD)
│   └── ...
├── notifications-service/    # Kafka consumer for notifications (planned)
│   └── ...
└── docker-compose.yml        # Local development environment
```

## 🚀 Services

### Orders Service (Port 8081)
Manages customer orders with the following entities:
- **Order** - Main order entity with status tracking
- **OrderItem** - Line items linking orders to products
- **Product** - Snapshot of product data at order time

### Products Service (Port 8082)
RESTful API for product management:
- Full CRUD operations
- Product catalog management

### Notifications Service (Port 8083) - *Planned*
Event-driven notification handler:
- Kafka consumer for order events
- Email/SMS notification simulation

## 📊 Database Schema

### orders_db
```
Orders              OrderItems          Products
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ id           │    │ id           │    │ id           │
│ status       │    │ order_id     │───▶│ name         │
│ totalPrice   │◀───│ product_id   │    │ price        │
│ createdAt    │    │ quantity     │    └──────────────┘
│ items[]      │    └──────────────┘
└──────────────┘
```

## 🏃 Getting Started

### Prerequisites
- JDK 21
- PostgreSQL 16
- Docker & Docker Compose (optional)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/OfirNahshoni/AsyncOrdersSystem.git
   cd AsyncOrdersSystem
   ```

2. **Setup databases**
   ```bash
   sudo -u postgres psql
   ```
   ```sql
   CREATE USER orders_user WITH PASSWORD 'your_password';
   CREATE DATABASE orders_db OWNER orders_user;
   
   CREATE USER products_user WITH PASSWORD 'your_password';
   CREATE DATABASE products_db OWNER products_user;
   ```

3. **Run services**
   ```bash
   cd orders-service
   ./gradlew bootRun
   ```

### Using Docker Compose (Coming Soon)
```bash
docker-compose up -d
```

## 📋 Project Progress

| Phase | Task | Status |
|-------|------|--------|
| Setup | GitHub repo creation | ✅ Done |
| Setup | Init Gradle projects (MonoRepo) | ✅ Done |
| Design | Microservices architecture | ✅ Done |
| Implement | Orders JPA entities & DTOs | ✅ Done |
| Implement | Orders Controller & Service | 🔄 In Progress |
| Implement | Products Service (CRUD) | ⏳ Planned |
| Implement | Kafka + Notifications | ⏳ Planned |
| Testing | Unit & Integration tests | ⏳ Planned |
| DevOps | Docker Compose setup | ⏳ Planned |
| DevOps | Kubernetes deployment | ⏳ Planned |

## 🔗 API Endpoints (Planned)

### Orders Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create new order |
| GET | `/api/orders/{id}` | Get order by ID |
| GET | `/api/orders` | List all orders |
| PATCH | `/api/orders/{id}/status` | Update order status |

### Products Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/products` | Create product |
| GET | `/api/products/{id}` | Get product by ID |
| GET | `/api/products` | List all products |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

## 👤 Author

**Ofir Nahshoni**

## 📝 License

This project is for educational purposes.

---

*Last updated: 18-January 2026*
