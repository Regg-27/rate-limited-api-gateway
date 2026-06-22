# Rate-Limited API Gateway
A production-grade REST API gateway built in Java/Spring Boot that serves a
custom vector search engine with JWT authentication, Redis caching, rate
limiting, and Docker deployment.

---

## Overview
A production-style REST API that wraps the Vector Search Engine project and serves it
as a deployed service. A client authenticates with JWT, sends a query vector, and
receives the top-k most similar vectors by cosine similarity — with results cached in
Redis, search metadata persisted in PostgreSQL, requests rate-limited per user, and the
entire stack containerized with Docker. This project takes the algorithmic work from
the Vector Search Engine and answers the next question: how do you actually ship it?

---

## Motivation
Most of what I'd seen of Spring Boot and REST APIs before this project was a brief
demo in my OOP course — enough to know they existed, not enough to understand them.
I wanted to actually build with the stack rather than just recognize it, and to
understand what sits between "the algorithm works" and "the service is running."

The decision to wrap the Vector Search Engine specifically wasn't accidental. That
project was something I'd built months earlier from scratch — the math, the indexing,
the benchmarking — and repurposing it here felt like closing a loop: the engine I
understood deeply, now deployed as something real. It's the same search logic, but
now it has authentication, caching, persistence, rate limiting, and can be reached
by anyone with a network connection and a token.

The broader goal was to prove something to myself: that I can learn and build at the
same time, not sequentially. This project covers more ground than anything I'd built
before — more infrastructure, more tools, more moving parts — and it's the closest
thing in my portfolio to what I might actually be asked to build and ship on a real
team. That's why it exists.

---

## Architecture & Design

### Architectural Style
A layered REST API following a controller → service → repository/index structure,
with cross-cutting concerns (authentication, rate limiting, caching) implemented as
a servlet filter and a Redis-backed component sitting in front of the core search
logic. Vectors are held in memory for fast similarity search; metadata is persisted
to PostgreSQL for durability across restarts. The whole system runs as three
containers — application, database, and cache — orchestrated with Docker Compose.

### Package Structure
```
api-gateway/
├── docs/
│   ├── README.md
│   └── learning_log.md
├── src/main/java/com/reggie/api_gateway/
│   ├── ApiGatewayApplication.java   # Spring Boot entry point, starts embedded Tomcat
│   ├── controller/
│   │   ├── HealthController.java    # GET /health — liveness check, exempt from auth
│   │   ├── AuthController.java      # POST /login — issues JWTs
│   │   └── SearchController.java    # POST /search, POST /ingest
│   ├── service/
│   │   └── SearchService.java       # orchestrates index, Postgres, and Redis cache
│   ├── index/
│   │   ├── VectorIndex.java         # interface shared with the VSE project
│   │   └── BruteForceIndex.java     # in-memory cosine similarity search
│   ├── math/
│   │   └── VectorMath.java          # vector math, copied from the VSE project
│   ├── dto/
│   │   ├── SearchRequest.java
│   │   ├── IngestRequest.java
│   │   ├── LoginRequest.java
│   │   └── VectorRecord.java        # maps Postgres rows back to in-memory vectors
│   ├── repository/
│   │   └── VectorRepository.java    # JDBC persistence for vector metadata
│   ├── security/
│   │   ├── JwtService.java          # issues and validates JWTs
│   │   ├── JwtFilter.java           # intercepts requests, enforces auth + rate limiting
│   │   └── RateLimiter.java         # Redis-backed per-user request counter
│   ├── exception/
│   │   └── GlobalExceptionHandler.java  # centralized error responses
│   └── config/
│       └── RedisConfig.java         # RedisTemplate bean with JSON serialization
├── src/main/resources/
│   ├── application.properties
│   └── schema.sql                   # creates the vectors table on startup
├── locustfile.py                    # Locust load test definitions
├── Dockerfile                       # multi-stage build: Maven build → JRE runtime
├── docker-compose.yml               # wires app, Postgres, and Redis together
└── pom.xml
```

### Key Design Decisions
**Layered architecture (controller → service → repository/index).** Each layer has
one responsibility — controllers handle HTTP only, the service holds business logic,
and the repository/index layer handles persistence and search. This mirrors the
separation used in the Vector Search Engine and SQL Engine projects.

**VectorIndex interface, not a concrete class.** SearchService depends on the
VectorIndex interface rather than BruteForceIndex directly, so the underlying search
strategy (brute-force today, IVF in the future) can be swapped without touching the
service or controller layers.

**Cache-aside pattern for Redis.** On search, the gateway checks Redis first using a
hash of the query vector as the key. On a miss, it runs the search, stores the result,
and returns it. This mirrors the eviction/cache-hit concepts from the Cache Simulator
project, applied to a real production cache.

**JWT for stateless authentication.** Chosen over session-based auth because it
requires no server-side session storage — any instance of the service can verify a
token using the shared secret, which matters for horizontal scaling even though this
project runs a single instance.

**Fixed-window rate limiting via Redis.** Each user gets a request counter keyed by
username with a 60-second TTL, capped at 100 requests/minute. Chosen for simplicity
over a sliding window — the tradeoff (burst allowance at window edges) is documented
under Known Limitations.

**Global exception handling over per-endpoint try-catch.** A single
@RestControllerAdvice class converts exceptions into proper HTTP status codes and
messages app-wide, keeping endpoint methods focused on the happy path.

**Multi-stage Docker build.** The Dockerfile builds the JAR in a Maven+JDK image,
then copies only the finished JAR into a lightweight JRE image. This keeps the final
runtime image small and leaves build tooling out of what actually ships.

**Service-name networking in Docker Compose.** Inside the compose network, the app
reaches Postgres and Redis by service name rather than localhost, since each container
has its own network namespace. Environment variables override application.properties
specifically inside Docker, while localhost still works for running from IntelliJ.

### Known Limitations
- Vectors are stored as comma-separated TEXT in Postgres rather than a native array type — the simplest JDBC mapping, at the cost of not being able to query individual vector values in SQL. Acceptable since all similarity computation happens in-memory; vector contents are never queried at the database level.
- The JWT secret key regenerates on each application restart, invalidating all previously issued tokens. A production system would load a stable secret from configuration or an environment variable.
- `/login` accepts any username/password combination without verifying credentials against stored, hashed passwords.
- Cache keys use `Arrays.hashCode()`, a 32-bit hash with a theoretical collision risk. Production would use a stronger hash like SHA-256.
- Search cache entries have no TTL and grow unbounded. (The rate-limiter counter does have a TTL — this limitation is specific to the search result cache.)
- Duplicate-id ingest returns a clean 409 Conflict, but other malformed input (invalid vector dimensions, malformed JSON) isn't yet validated with specific handlers.
- Rate limiting uses a fixed window, which can allow bursts of up to 2x the limit across a window boundary. A sliding window would smooth this out.
- The rate limit (100 requests/minute) is hardcoded rather than configurable via application properties.
- Postgres data lives inside the container rather than a named Docker volume — removing the container deletes the data. A named volume would decouple data lifetime from container lifetime.

---

### Benchmark Results
Load tested with [Locust](https://locust.io/), 50 concurrent simulated users, 1-second
wait time between requests, against the Dockerized stack.

**Authenticated search, repeated query vector (mostly cache hits):**

| Metric | Value |
|--------|-------|
| Requests | 3,702 |
| Failures | 0 |
| Median latency | 18 ms |
| 95th percentile | 33 ms |
| 99th percentile | 39 ms |
| Throughput | 45.2 req/s |

**Authenticated search, randomized query vectors (mostly cache misses):**

| Metric | Value |
|--------|-------|
| Requests | 3,050 |
| Failures | 0 |
| Median latency | 19 ms |
| 95th percentile | 30 ms |
| 99th percentile | 39 ms |

**Login (JWT issuance):**

| Metric | Value |
|--------|-------|
| Requests | 50 |
| Failures | 0 |
| Median latency | 27 ms |
| 95th percentile | 95 ms |

Cached and uncached search latency were nearly identical at this index size
(~3 vectors) — brute-force cosine similarity over a handful of vectors is already
faster than the overhead the cache would save. The cache's value is proportional to
dataset size and would show a measurable gap with a larger index; at this scale, the
bottleneck is JWT validation, JSON parsing, and network overhead rather than search
computation itself.

Two real issues surfaced during load testing and were fixed before these results were
captured: the `/health` endpoint was unintentionally protected by the JWT filter, and
the rate limiter was initially keyed by username — since all simulated users shared one
hardcoded login, they shared a single rate-limit bucket. Fixed by issuing each test user
a unique username via UUID.

---

## How to Run
### Prerequisites
- Docker Desktop
- Java 21 (only needed if running outside Docker)
- Python 3 + pip (only needed for load testing)

### Run the full stack
```bash
git clone https://github.com/Regg-27/rate-limited-api-gateway.git
cd rate-limited-api-gateway
docker compose up --build
```

This builds the application image and starts three containers — the API gateway,
PostgreSQL, and Redis — wired together on a shared network. The API is available at
`http://localhost:8080`.

### Try it
```bash
# Log in to get a JWT
TOKEN=$(curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"anything"}')

# Ingest a vector
curl -X POST http://localhost:8080/ingest \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"id":1,"label":"example","vector":[1.0,0.0,0.0]}'

# Search
curl -X POST http://localhost:8080/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":[1.0,0.0,0.0],"k":1}'
```

### Stop the stack
```bash
docker compose down
```

### Run load tests (optional)
```bash
pip install locust
python3 -m locust -f locustfile.py --host http://localhost:8080
```
Then open `http://localhost:8089` and configure a test run.

---

## What I Learned
This was the most backend-heavy project I've built, and it exposed me to an enormous
amount of infrastructure I'd never touched before — Spring Boot, JWT, Redis, Postgres,
Docker, and load testing, all in eight days. The biggest shift from my earlier projects
was realizing how much invisible machinery sits between "the code works" and "the
service is actually running and reachable" — authentication, caching, persistence,
rate limiting, and containerization all have to work together before a single request
succeeds, and debugging across that many layers was genuinely harder than anything in
the first three projects.

A few technical things stuck with me specifically. Interfaces aren't just abstraction
for its own sake — when I tried to call add() through a VectorIndex reference and the
compiler rejected it, I understood for the first time that the compiler only trusts
what's declared on the interface, while the JVM resolves the actual implementation at
runtime. JWTs made sense once I decoded a real token's payload and saw it was just
readable text — the security comes entirely from the signature being unforgeable
without the secret key, not from hiding anything. The Redis caching work connected
directly back to my Cache Simulator project; the cache-aside pattern is the same idea
of trading memory for speed that I built from scratch in C++, just applied at the
scale of a real system instead of a simulation. And Docker's container networking
model — where localhost means the container itself, not the host machine — was the
single concept that caused the most actual debugging time, including a connection-
refused error that turned out to just be a stopped container, not a networking bug
at all.

I also want to be honest about how this project differed from my earlier ones. The
first three projects were close to fully self-derived through Socratic guidance.
This one leaned more on direct explanation, especially on Postgres, JWT, and Docker —
territory I had zero prior exposure to — and on one day specifically I asked to shift
to a faster, more guided pace because of a tight deadline. That means my depth on this
project right now is uneven: I have far more surface exposure to a much larger stack
than I've worked with before, but not yet the kind of from-memory fluency I have with
the Vector Search Engine. I'm treating that gap as unfinished work, not a finished
state — this project is getting a full independent review pass during interview prep,
where the goal is to be able to explain and rebuild every piece of it without notes.

What this project proved to me, more than any single technical lesson, is that
shipping software is a different skill than building an algorithm. The Vector Search
Engine taught me how to think about search. This taught me everything that has to
happen around that thinking before anyone else can actually use it.

---

## Future Features
- **Swap to IVF indexing.** SearchService depends on the VectorIndex interface
  specifically so the underlying index can change without touching the service or
  controller layers — swapping in the Vector Search Engine's IVFIndex would let this
  gateway trade a small amount of recall for faster search at scale.
- **Named Docker volume for Postgres**, decoupling data lifetime from container
  lifetime so `docker compose down` doesn't risk losing persisted vectors.
- **Real authentication** — verify credentials against stored, hashed passwords
  instead of accepting any username/password pair.
- **Stable JWT secret** loaded from configuration or environment variable, so tokens
  survive application restarts.
- **Configurable, sliding-window rate limiting**, replacing the hardcoded 100/minute
  fixed window with a tunable, burst-resistant implementation.
- **SHA-256 cache keys**, replacing Arrays.hashCode() to eliminate theoretical
  32-bit collision risk.
- **TTL on search cache entries**, so the Redis cache doesn't grow unbounded over time.
- **Broader input validation** — specific handlers for malformed vectors, bad JSON,
  and other invalid input beyond the current duplicate-id case.