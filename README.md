# Creator CRM

A full-stack SaaS platform for content creators to manage brand deals, deliverables, invoices, and campaign deadlines.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind CSS |
| API Gateway | Spring Cloud Gateway, JWT auth, Redis rate limiter |
| Microservices | Spring Boot 3 / Java 21 (virtual threads) |
| Messaging | Apache Kafka |
| Database | Supabase (Postgres) + Row-Level Security |
| Cache | Redis |
| Payments | Stripe Billing + Stripe Connect |
| Containers | Docker, Kubernetes (Helm charts) |
| Observability | OpenTelemetry → Jaeger + Prometheus + Grafana + Loki |
| CI/CD | GitHub Actions → GHCR → Helm deploy |

## Project structure

```
creator-crm/
├── .github/workflows/     # CI + Release workflows
├── services/
│   ├── api-gateway/       # Spring Cloud Gateway (port 8080)
│   ├── deal-service/      # Brand deal lifecycle  (port 8081)
│   ├── deliverable-service/                       (port 8082)
│   ├── campaign-service/                          (port 8083)
│   ├── invoice-service/   # Stripe billing        (port 8084)
│   ├── notification-service/ # Email + in-app     (port 8085)
│   └── analytics-service/                         (port 8086)
├── frontend/              # Next.js app
├── infra/
│   ├── docker/            # OTel, Prometheus, Grafana configs
│   └── k8s/helm/          # Helm charts per service
├── docker-compose.yml     # Full local dev stack
├── build.gradle.kts       # Root Gradle build
└── settings.gradle.kts    # Subproject declarations
```

## Local development

### Prerequisites
- Java 21+, Docker Desktop, Node.js 20+

### Start everything

```bash
# 1. Clone and copy env
git clone https://github.com/your-org/creator-crm
cd creator-crm
cp .env.example .env          # fill in Supabase + Stripe keys

# 2. Start infrastructure (Postgres, Redis, Kafka, OTel, Grafana)
docker compose up -d postgres redis kafka zookeeper otel-collector jaeger prometheus grafana

# 3. Run backend services
./gradlew :services:api-gateway:bootRun &
./gradlew :services:deal-service:bootRun &

# 4. Run frontend
cd frontend && npm install && npm run dev
```

### Run all tests

```bash
./gradlew test          # all Spring Boot tests (Testcontainers auto-spin)
cd frontend && npm test
```

## Observability UIs (local)

| Tool | URL |
|---|---|
| Grafana | http://localhost:3001 |
| Jaeger | http://localhost:16686 |
| Prometheus | http://localhost:9090 |
| Kafka UI | http://localhost:9021 |
| API docs | http://localhost:8080/swagger-ui.html |

## CI/CD

Every pull request runs: `lint → test → security scan`.
Merges to `main` additionally: `docker build → push to GHCR → deploy to staging`.
Version tags (`v*.*.*`) trigger the production release workflow.

## Project phases

| Phase | Scope | Weeks |
|---|---|---|
| 1 | Foundation, CI, auth, DB schema | 1–3 |
| 2 | Core microservices, Kafka, dashboard UI | 4–9 |
| 3 | Payments, invoicing, notifications | 10–13 |
| 4 | Kubernetes, OTel observability, security | 14–17 |
| 5 | Analytics, load testing, production launch | 18–20 |
