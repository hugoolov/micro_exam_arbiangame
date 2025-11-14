# Arabian Card Game - Microservices Architecture

## Project Overview

This project implements an Arabian card game (similar to Golf card game) using a microservices architecture. The game involves players trying to achieve the lowest score by strategically swapping cards from their hand. The system also includes weather information that encourages players to go outside when weather conditions are good.

### Key Features:
- **Card Game**: Strategic turn-based game against AI opponent
- **User Authentication**: Secure registration and login
- **Game Results Tracking**: Persistent storage of game outcomes
- **Weather Integration**: Real-time weather data with "go outside" recommendations
---

## System Architecture

### Architecture Diagram

```
┌──────────────┐
│   Frontend   │ (React - Port 5173)
│   (Vite)     │
└──────┬───────┘
       │
       │ HTTP/REST
       │
┌──────▼───────────────────────────────────────────┐
│            API Gateway (Port 8080)                │
│     Spring Cloud Gateway + Consul Discovery       │
└────┬─────┬──────┬──────────┬────────────────┬────┘
     │     │      │          │                │
     │     │      │          │                │
┌────▼──┐ ┌▼─────▼┐ ┌───────▼────┐  ┌───────▼─────┐
│ Auth  │ │ Game  │ │   Game     │  │   Weather   │
│Service│ │ Logic │ │  Results   │  │   Service   │
│ 8083  │ │ 8081  │ │    8082    │  │    8084     │
└───┬───┘ └───┬───┘ └─────▲──────┘  └─────────────┘
    │         │           │
    │         │      ┌────┴─────┐
    │         │      │ RabbitMQ │
    │         └──────► (5672)   │
    │                │  Queue   │
    │                └──────────┘
┌───▼───────────────────────────┐
│       Service Discovery       │
│      Consul (Port 8500)       │
└───────────────────────────────┘

┌───────────────┬────────────────┬──────────────┐
│ PostgreSQL    │  PostgreSQL    │ PostgreSQL   │
│ auth-db       │  game-logic-db │ results-db   │
│ (5435)        │  (5433)        │ (5434)       │
└───────────────┴────────────────┴──────────────┘
```

### Services

| Service | Port | Purpose | Database | External APIs |
|---------|------|---------|----------|---------------|
| **API Gateway** | 8080 | Routes requests, load balancing, CORS | None | - |
| **Auth Service** | 8083 | User registration/login, password hashing | PostgreSQL (5435) | - |
| **Game Logic** | 8081 | Game state management, AI opponent, card logic | PostgreSQL (5433) | - |
| **Game Results** | 8082 | Persists game outcomes, leaderboard | PostgreSQL (5434) | - |
| **Weather Service** | 8084 | Fetches weather, "go outside" logic | None | MET Norway API |

### Infrastructure Components

| Component | Port | Purpose |
|-----------|------|---------|
| **Consul** | 8500 (UI), 8600 (DNS) | Service discovery, health monitoring, configuration |
| **RabbitMQ** | 5672 (AMQP), 15672 (UI) | Asynchronous messaging between services |
| **PostgreSQL** | 5433, 5434, 5435 | Database per service pattern |

### Communication Patterns

#### Synchronous (REST/HTTP):
- Frontend → API Gateway → All Services
- Services registered with Consul for discovery
- Load-balanced via `lb://service-name` URIs

#### Asynchronous (Message Queue):
- Game Logic → RabbitMQ → Game Results
- Event-driven: When game ends, result published to queue
- Decouples game logic from result persistence

---

## Prerequisites

### Required Software:
- **Java 21** (JDK)
- **Maven 3.8+**
- **Docker** 20.10+
- **Docker Compose** 2.0+
- **Node.js 18+** (for frontend)
- **npm** or **yarn**

### Verify Installation:
```bash
java -version   # Should show Java 21
mvn -version    # Should show Maven 3.8+
docker --version
docker-compose --version
node --version
npm --version
```

---

## Build and Run Instructions

### Option 1: Using Docker Compose (Recommended for Examiners)

This is the **recommended method** as it requires minimal setup and matches production deployment.

#### Step 1: Build all services
```bash
# Navigate to project root
cd /path/to/project

# Build all Maven projects
mvn clean package -f auth/pom.xml
mvn clean package -f game_logic/pom.xml
mvn clean package -f gameresult/pom.xml
mvn clean package -f api-gateway/pom.xml
mvn clean package -f weather/pom.xml
```

#### Step 2: Start all services with Docker Compose
```bash
# Start all services (builds Docker images if needed)
docker-compose up --build

# Or run in detached mode (background):
docker-compose up --build -d

# Run with multiple game-logic instances
docker compose up -d --scale game-logic=3 #game-logic="number of instances"
```

#### Step 3: Verify services are running
```bash
# Check running containers
docker-compose ps

# You should see all services as "Up"
```

#### Step 4: Access the application
- **Frontend**: http://localhost:5173 (after starting frontend separately - see below)
- **API Gateway**: http://localhost:8080
- **Consul UI**: http://localhost:8500
- **RabbitMQ Management**: http://localhost:15672 (user: guest, pass: guest)

#### Step 5: Start the frontend (separate terminal)
```bash
cd frontend
npm install
npm run dev
```

#### Stop all services
```bash
# Stop and remove containers
docker-compose down

# Stop, remove containers, and volumes (clean slate)
docker-compose down -v
```

---

### Option 2: Running Services Locally (Development)

This method is useful for development but requires more setup.

#### Step 1: Start infrastructure services
```bash
docker-compose up consul rabbitmq postgres-auth postgres-game-logic postgres-game-results
```

#### Step 2: Build all services
```bash
mvn clean package -f auth/pom.xml
mvn clean package -f game_logic/pom.xml
mvn clean package -f gameresult/pom.xml
mvn clean package -f api-gateway/pom.xml
mvn clean package -f weather/pom.xml
```

#### Step 3: Start each service (separate terminals)
```bash
# Terminal 1: Auth Service
cd auth
mvn spring-boot:run

# Terminal 2: Game Logic
cd game_logic
mvn spring-boot:run

# Terminal 3: Game Results
cd gameresult
mvn spring-boot:run

# Terminal 4: Weather Service
cd weather
mvn spring-boot:run

# Terminal 5: API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 6: Frontend
cd frontend
npm run dev
```

---

## Testing the Application

### Test Credentials

For testing authentication:
- **Username**: `testuser`
- **Password**: `testpass123`

(Or register a new user through the UI)

### User Stories / Test Scenarios

#### 1. User Registration and Login
**Objective**: Test authentication service

**Steps**:
1. Open http://localhost:5173
2. Click "Registrer deg" (Register)
3. Enter a unique username (at least 3 characters)
4. Enter a password (at least 6 characters)
5. Click "Registrer"
6. You should see a success message
7. Enter the same credentials in the login form
8. Click "Login"
9. You should be logged in

**Expected Result**: User is registered in PostgreSQL (auth-db) and can login

---

#### 2. Play a Complete Game
**Objective**: Test game logic service and AI opponent

**Steps**:
1. After logging in, click "Start Game"
2. You will see:
   - Your 4 cards (face up)
   - Computer's 4 cards (face down)
   - Main deck size
   - Open table (discard pile)
3. Click "Draw from Main Deck"
4. View the card you drew
5. Either:
   - **Option A**: Click a card in your hand to select it, then click "Swap with Selected Card"
   - **Option B**: Click "Discard" to discard the drawn card
6. The computer will automatically take its turn
7. Repeat until the deck is empty
8. Game ends automatically when no cards left to draw
9. View final scores (lower score wins)

**Expected Result**:
- Game state persists between turns
- Computer makes strategic decisions
- Final scores calculated correctly
- Winner determined (player/computer/tie)

---

#### 3. Save Game Results
**Objective**: Test asynchronous messaging (RabbitMQ) and game results service

**Steps**:
1. After a game ends, click "Save Result"
2. Enter your name in the prompt
3. Click OK
4. View RabbitMQ Management UI: http://localhost:15672
   - Login: guest / guest
   - Go to "Queues" tab
   - You should see `game-result-queue` with messages processed
5. Verify result was saved by checking:
   ```bash
   curl http://localhost:8080/api/results
   ```

**Expected Result**:
- Message sent to RabbitMQ queue
- Game Results service consumes message
- Result saved to PostgreSQL (results-db)
- Can retrieve all results via API

---

#### 4. Weather Integration
**Objective**: Test weather service and external API integration

**Steps**:
1. On the main page, observe the weather widget
2. It will show:
   - Current temperature
   - Wind speed
   - Weather condition
   - A message (either "Go Outside!" or "Enjoy gaming")
3. When weather is sunny and nice (15-25°C, low wind):
   - Widget should pulse with pink/red gradient
   - Message: "Perfect weather! Stop playing and go outside! ☀️"
4. When weather is bad:
   - Calm purple gradient
   - Message: "Perfect weather for gaming! 🌧️🎮"

---

#### 5. Service Discovery and Health Monitoring
**Objective**: Verify Consul integration

**Steps**:
1. Open Consul UI: http://localhost:8500
2. Click "Services" in sidebar
3. Verify all services are listed:
   - api-gateway
   - auth-service
   - game-logic
   - game-results
   - weather
4. Each service should show:
   - ✓ Green checkmark (healthy)
   - Number of instances
   - Health check status
5. Click on any service to see details:
   - Service address
   - Health check endpoint (/actuator/health)
   - Metadata

**Test health endpoints directly**:
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8084/actuator/health
```

**Expected Result**: All services register with Consul and report healthy status

---

#### 6. Load Balancing (For A-Grade)
**Objective**: Test multiple instances and load balancing

**Steps**:
1. Scale game-logic service to 2 instances:
   ```bash
   docker-compose up --scale game-logic=2 -d
   ```
2. Check Consul UI - should show 2 instances of game-logic
3. Play multiple games and observe logs:
   ```bash
   docker-compose logs -f game-logic
   ```
4. Requests should be distributed across both instances

**Expected Result**: Load balancer distributes requests across multiple instances

---

## Architecture Decisions

### 1. Database Per Service Pattern
**Decision**: Each microservice has its own PostgreSQL database

**Reasoning**:
- Service independence: Each service can evolve its schema independently
- Fault isolation: Database issues in one service don't affect others
- Scalability: Databases can be scaled independently based on load

**Tradeoff**:
- More complex data management
- No foreign key constraints across services
- Requires distributed transactions for cross-service operations (not needed in our case)

### 2. API Gateway Pattern
**Decision**: Use Spring Cloud Gateway as single entry point

**Reasoning**:
- Single point of entry for clients
- Centralized CORS management
- Service discovery integration
- Load balancing capabilities
- Future: Can add authentication, rate limiting, etc.

**Tradeoff**:
- Single point of failure (mitigated with health monitoring)
- Additional network hop (minimal latency impact)

### 3. Asynchronous Communication for Game Results
**Decision**: Use RabbitMQ message queue for game result persistence

**Reasoning**:
- Decouples game logic from result storage
- Game can end immediately without waiting for persistence
- Better fault tolerance: If results service is down, messages queue up
- Event-driven architecture: Multiple services could subscribe to game events

**Tradeoff**:
- Eventually consistent (results not immediately available)
- More complex infrastructure
- Requires message broker monitoring

### 4. Consul for Service Discovery
**Decision**: Use HashiCorp Consul instead of Eureka

**Reasoning**:
- Built-in health checking
- Configuration management capabilities
- Active development and cloud-native focus
- Better production readiness

**Tradeoff**:
- Additional infrastructure component
- Learning curve for operations team

### 5. Weather as Separate Service
**Decision**: Weather functionality in dedicated microservice

**Reasoning**:
- Single Responsibility Principle
- Can be reused by multiple applications
- Isolates external API dependency
- Independent scaling based on weather check frequency

**Tradeoff**:
- Overhead for simple functionality
- Additional deployment complexity

---

## Assumptions and Simplifications

### 1. Authentication
**Assumption**: Simple username/password authentication is sufficient

**Simplification**:
- No JWT tokens (services trust the authenticated user ID)
- No session management
- In production: Would use OAuth2/JWT with Spring Security

### 2. Game State Persistence
**Assumption**: Single-player games only (no multiplayer)

**Simplification**:
- Game state stored in memory during play
- Only persisted when game ends
- No real-time multiplayer synchronization needed

### 3. Weather Service
**Assumption**: MET Norway API always available

**Simplification**:
- No caching of weather data
- No fallback if API is down
- In production: Would cache results for 5-10 minutes

### 4. Security
**Assumption**: Running in trusted network environment

**Simplification**:
- No service-to-service authentication
- No API key management for external APIs
- No HTTPS/TLS
- In production: Would use mutual TLS between services

### 5. Monitoring
**Assumption**: Consul health checks are sufficient

**Simplification**:
- No distributed tracing (e.g., Zipkin)
- No centralized logging (e.g., ELK stack)
- No metrics aggregation (e.g., Prometheus)
- In production: Would add full observability stack

### 6. Data Management
**Assumption**: Low data volume

**Simplification**:
- No database migrations (using Hibernate DDL auto)
- No database backups
- No data retention policies
- In production: Would use Flyway/Liquibase for migrations

---

## Troubleshooting

### Services not starting
```bash
# Check logs
docker-compose logs -f [service-name]

# Restart specific service
docker-compose restart [service-name]

# Clean restart
docker-compose down -v
docker-compose up --build
```

### Service not registering with Consul
1. Check Consul is running: http://localhost:8500
2. Verify service has `@EnableDiscoveryClient` annotation
3. Check application.properties has Consul configuration
4. Check logs for connection errors

### Database connection errors
```bash
# Check database is running
docker-compose ps postgres-auth postgres-game-logic postgres-game-results

# Check database logs
docker-compose logs postgres-auth
```

### RabbitMQ messages not being consumed
1. Check RabbitMQ UI: http://localhost:15672
2. Verify queue `game-result-queue` exists
3. Check both game-logic and game-results are running
4. Check consumer logs for errors

---

## Project Structure

```
├── api-gateway/          # API Gateway service
├── auth/                 # Authentication service
├── game_logic/           # Game logic service
├── gameresult/           # Game results service
├── weather/              # Weather service
├── frontend/             # React frontend
├── docker-compose.yml    # Docker Compose configuration
└── README.md            # This file
```

---

## Technologies Used

### Backend
- **Spring Boot 3.5.5**: Microservices framework
- **Spring Cloud Gateway**: API Gateway
- **Spring Cloud Consul**: Service discovery
- **Spring Data JPA**: Database access
- **RabbitMQ**: Message broker
- **PostgreSQL 16**: Database
- **Lombok**: Boilerplate reduction

### Frontend
- **React 18**: UI framework
- **Vite**: Build tool
- **TypeScript**: Type-safe JavaScript

### Infrastructure
- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration
- **Consul**: Service discovery and health monitoring
- **RabbitMQ**: Message queue

---


## Acknowledgments

- MET Norway for weather API
- Course instructors for guidance and feedback
