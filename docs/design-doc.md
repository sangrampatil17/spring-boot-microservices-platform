# Design Doc: Task Platform (gateway + task-service)

Planned and reviewed with Claude as a design partner, then implemented and tested by hand. Format follows the
"what / why / who / how" structure used for team design reviews.

## 1. What
A small microservices platform: an **API gateway** in front of a **task-service** that stores tasks in MongoDB,
secured with JWT and role-based access control.

## 2. Why
A compact, production-shaped reference for the things a backend service needs beyond CRUD: stateless auth,
RBAC, pagination and search, health probes, metrics, containers, GitOps deployment and CI security scanning.

## 3. Who uses it
| Role | Can do |
|------|--------|
| USER | Log in, create, read, update and search tasks |
| ADMIN | Everything a USER can, plus delete tasks |

## 4. How

### 4.1 Architecture
```
Client -> api-gateway :8080 --/task-service/** (StripPrefix=1)--> task-service :8081 -> MongoDB
                |                                                   |
         /actuator/prometheus                               /actuator/prometheus
```

### 4.2 Endpoints (task-service)
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/api/auth/login` | public | returns `{ "token": "<jwt>" }` |
| GET | `/api/tasks?q=&status=&page=&size=` | any user | newest first, size capped at 100 |
| GET | `/api/tasks/{id}` | any user | 404 if missing |
| POST | `/api/tasks` | any user | owner taken from the token |
| PUT | `/api/tasks/{id}` | any user | owner is preserved |
| DELETE | `/api/tasks/{id}` | ADMIN | 403 for USER |

Payload: `{ "title": "string (required, max 120)", "description": "string (max 2000)", "status": "TODO|IN_PROGRESS|DONE", "tags": ["string"] }`

### 4.3 Data (MongoDB, collection `tasks`)
`_id`, `title` (indexed), `description`, `status` (indexed), `owner`, `tags[]`, `createdAt`, `updatedAt`.
Indexes on `title` and `status` back the search and filter queries.

### 4.4 Security decisions
- Stateless HS256 JWT with `sub` and `roles` claims, 60-minute TTL, secret supplied via `JWT_SECRET`.
- A filter populates the Spring Security context; `@PreAuthorize` enforces roles at the method level.
- Invalid or expired tokens leave the request unauthenticated, which returns 401.
- The demo login uses two in-memory users. Production would delegate to an identity provider.

### 4.5 Operations
- Liveness and readiness probes at `/actuator/health/{liveness,readiness}`.
- Prometheus metrics at `/actuator/prometheus` (scrape annotations in `k8s/`), ready for Grafana dashboards.
- ArgoCD syncs `k8s/` with automated prune and self-heal.
- CI: build and test, Gitleaks secret scan, Trivy image scan failing on fixable HIGH/CRITICAL findings.

### 4.6 Operational notes (documentation only)
- Secrets: `JWT_SECRET` and `MONGODB_URI` must come from a Kubernetes Secret (`task-service-secrets`), never from the
  image. The defaults in `application.yml` exist for local demos only.
- Probes and metrics: readiness and liveness use the Actuator health groups; Prometheus scrapes
  `/actuator/prometheus`, so a Grafana dashboard can chart JVM, HTTP and request-latency metrics.
- Deployment files (Dockerfiles, `docker-compose.yml`, `k8s/`, `argocd/`, `.github/workflows/ci.yml`) are a template.
  They were not exercised against a live cluster or Docker daemon while writing this repo.
- Lesson learned: the servlet container forwards failures to `/error`, and the JWT filter does not run on that
  internal dispatch. `/error` is therefore permitted explicitly so a 403 is not reported as a 401.

## 5. Trade-offs and what is next
- Token validation happens in the service. Moving it to the gateway would centralise auth.
- Search uses a case-insensitive title match. A MongoDB text index or Atlas Search would scale better.
- Next: refresh tokens, rate limiting at the gateway, and integration tests with Testcontainers.
