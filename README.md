# Spring Boot Microservices Platform

![Java 17](https://img.shields.io/badge/Java-17-orange) ![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen) ![MongoDB](https://img.shields.io/badge/MongoDB-7-green) ![Kubernetes](https://img.shields.io/badge/Kubernetes-ArgoCD-blue)

A production-shaped reference: **Spring Cloud Gateway** in front of a **Spring Boot + MongoDB** service with
**JWT authentication and role-based access control**, plus Docker, Kubernetes manifests, **ArgoCD GitOps** and a
CI pipeline with **Trivy** and **Gitleaks** scanning.

Design doc (planned with Claude, implemented by hand): [docs/design-doc.md](docs/design-doc.md)

## Architecture
```
Client -> api-gateway :8080 --/task-service/**--> task-service :8081 -> MongoDB
```

| Module | What it does |
|--------|--------------|
| `api-gateway` | Spring Cloud Gateway route with prefix stripping, health probes, Prometheus metrics |
| `task-service` | Task CRUD with pagination and search, JWT + RBAC (USER / ADMIN), OpenAPI docs, Mongo indexes |

## Run locally
Prerequisites: Java 17, Maven 3.9+, and MongoDB on `localhost:27017` (or Docker).

```bash
mvn verify                                   # build + tests
java -jar task-service/target/task-service-*.jar
java -jar api-gateway/target/api-gateway-*.jar
```
Or with Docker: `docker compose up --build`.

### Try it
```bash
# demo users: admin / admin-demo-pass (ADMIN, USER) and user / user-demo-pass (USER)
TOKEN=$(curl -s localhost:8080/task-service/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user-demo-pass"}' | jq -r .token)

curl -s localhost:8080/task-service/api/tasks -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Write design doc","tags":["docs"]}'

curl -s "localhost:8080/task-service/api/tasks?q=design" -H "Authorization: Bearer $TOKEN"
```
Swagger UI: http://localhost:8081/swagger-ui.html. Deleting a task as `user` returns 403; as `admin` it returns 204.
The demo credentials are for local use only. Set `JWT_SECRET`, `DEMO_ADMIN_PASSWORD` and `DEMO_USER_PASSWORD`
in any shared environment.

## Deploy
- `k8s/` contains Deployments, Services, probes, resource limits and Prometheus scrape annotations.
- `argocd/application.yaml` points ArgoCD at `k8s/` with automated sync, prune and self-heal.
- Create the `task-service-secrets` secret (`mongodb-uri`, `jwt-secret`) in the target namespace first.

## Quality gates (`.github/workflows/ci.yml`)
Build and test on Java 17, Gitleaks secret scan, and a Trivy image scan that fails on fixable HIGH/CRITICAL issues.

## Tests
`mvn test` runs JUnit 5 and Mockito unit tests for JWT issuing and validation (valid, tampered and expired tokens)
and for the task service logic (defaults, not found, update, search precedence).

## Status
Unit tests and the Maven build are verified. The Docker, Kubernetes and ArgoCD files are provided as a deployment
template and have not been run against a live cluster here.
