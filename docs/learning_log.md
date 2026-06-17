# Learning Log — Rate-Limited API Gateway

## Progress
Day 1: Spring Boot scaffold, health endpoint
Day 2: VSE integration, POST /search and POST /ingest endpoints working
Day 3: Postgres integration, vectors persist across restarts
Day 4: JWT authentication — login endpoint, token filter, protected endpoints
Day 5: Redis caching — cache-aside pattern, query results cached by hashed key
Day 6: Rate limiting (Redis counter, 100/min, 429) + global error handling (409 on duplicate)

---

## Day 1
### What I built
Spring Boot project scaffolded via start.spring.io with Maven, Java 21 config,
and Spring Web dependency. Embedded Tomcat server starts on port 8080. First
working endpoint — GET /health returns "OK". Verified live in browser.

### What confused me
The annotations @RestController and @GetMapping were completely new — there was no prior project to connect them to.

### How I resolved it


### Performance notes
Server startup time: 0.676 seconds. No performance work yet — setup day only.

---

## Day 2
### What I built
Copied VSE core files (VectorIndex, BruteForceIndex, VectorMath) into the
api-gateway project with updated package declarations. Created package structure:
controller, service, index, math, dto. Built SearchService with search() and
add() methods backed by BruteForceIndex. Built SearchController with POST /search
and POST /ingest endpoints. Created SearchRequest and IngestRequest DTOs with
no-args constructors and getters for Jackson deserialization. Verified both
endpoints live via curl — ingest returned "Vector Added", search returned
[{"id":1,"score":1.0}].

### What confused me
- Why index.add() didn't work through the VectorIndex reference when index.search() did. 
- The mechanical nature of Spring Boot setup made it feel like copying patterns without understanding them. 
- Whether casting to BruteForceIndex or updating the interface was the right fix. 
- Initially thought System.out.println would send a response back to the client.

### How I resolved it
- The interface issue clicked after reasoning through what the compiler sees at compile time vs what the JVM resolves at runtime — the compiler only allows calls declared on the interface. 
- Changed position on casting vs interface after understanding casting breaks the abstraction. 
- The "not learning" feeling resolved after connecting the day's concepts — dynamic dispatch, dependency injection, separation of concerns — to things that matter in real systems. 
- System.out.println correction was immediate once pointed out.

### Performance notes
Server startup: 0.593 seconds. No performance benchmarking yet — integration
day only. First end-to-end HTTP request through Spring Boot to VSE and back
confirmed working.

---

## Day 3
### What I built
Added Postgres integration via Docker container (postgres:15) running on port 5432. 
Added spring-boot-starter-jdbc and postgresql dependencies. Configured
database connection in application.properties. Created schema.sql with IF NOT
EXISTS guard that auto-runs on startup. Built VectorRepository with save() and
findAll() methods using JdbcTemplate. Vectors serialized to comma-separated
strings for storage, deserialized back to float[] on load. Added @PostConstruct
loadFromDatabase() to SearchService to reload all vectors into BruteForceIndex
on startup. Updated IngestRequest to include label field. 
Verified full persistence cycle — ingested vector survived server restart and was searchable without re-ingesting.

### What confused me
- The RowMapper lambda syntax confused me. It's not something I use often, but it was opportune to do so, but I couldn't connect exactly what the lambda was supposed to look like and how to complete the following code using it.
- Updating IngestRequest to include label caused a cascade of compiler errors through SearchService and SearchController that needed to be traced and fixed.

### How I resolved it
- The lambda clicked after seeing the full class version side by side with the lambda version, jogging my memory on why it is more efficient and better practice all-around
- Followed the compiler errors in order — SearchService's add() needed a label
  parameter, which then surfaced the missing getLabel() call in SearchController.
  Fixed each one by tracing back to the source change.

### Performance notes
Server startup: stable. Postgres round-trip not yet benchmarked — persistence
day only. Full ingest → restart → search cycle confirmed working end to end.

---

## Day 4
### What I built
Added JWT authentication. Pulled in the JJWT library (jjwt-api, jjwt-impl,
jjwt-jackson) via pom.xml. Built JwtService with generateToken() — builds a
signed JWT with subject, issued-at, and one-hour expiration — and validateToken()
which verifies the signature and extracts the username, throwing if the token is
invalid, expired, or tampered with. Created LoginRequest DTO and AuthController
with POST /login that issues a token. Built JwtFilter extending
OncePerRequestFilter — intercepts every request, exempts /login, pulls the token
from the Authorization header, strips the "Bearer " prefix, validates it, and
either passes the request down the filter chain or returns 401. Verified the full
flow: no token → 401, login → token issued, valid token → 200 with results.

### What confused me
Nothing major caused real confusion this session. The day was mostly absorbing
new JWT concepts — token structure, signatures, the filter chain — rather than
struggling with them. The one genuinely new mechanism was the filter chain and
how filterChain.doFilter() controls whether a request proceeds.

### How I resolved it
Worked through the JWT model conceptually before writing code, which made the
implementation straightforward. Deduced the /login filter-exemption problem before
it became a bug rather than hitting it and debugging afterward.

### Performance notes
No performance benchmarking — security day. Auth adds one signature verification
per protected request, which is negligible (HMAC-SHA256 is fast). Token expiration
set to one hour. Known limitations for README: secret key regenerates on restart
(invalidating existing tokens) and /login accepts any credentials without
verifying against stored hashed passwords.

---

## Day 5
### What I built
Added Redis caching via Docker container (redis:7) on port 6379. Added
spring-boot-starter-data-redis dependency and Redis connection config in
application.properties. Built RedisConfig with a RedisTemplate bean using
StringRedisSerializer for keys and GenericJacksonJsonRedisSerializer for values.
Implemented cache-aside pattern in SearchService.search(): hash the query vector
with Arrays.hashCode() to build a key ("search:" + hash), check Redis first, return
on hit, otherwise run the search, store the result, and return it. Added a no-args
constructor to SearchResult for Jackson deserialization. Verified the cache works —
confirmed the hashed key landed in Redis via redis-cli KEYS.

### What confused me
Not much conceptual confusion — the cache-aside pattern mapped directly onto the
cache simulator work. The real time sink was a misleading 401 on /ingest that
looked like an auth failure but was actually a DuplicateKeyException from trying to
re-insert id 1, which already existed in Postgres from Day 3 testing. A separate
deprecation issue with the Redis serializer (Spring Data Redis 4.0 deprecated
GenericJackson2JsonRedisSerializer in favor of GenericJacksonJsonRedisSerializer,
which uses a builder instead of a constructor) also took a few tries to resolve.

### How I resolved it
Added e.printStackTrace() to the filter's catch block to surface the real
exception, which revealed the duplicate-key error rather than an auth problem —
reading the stack trace was the key. Resolved the duplicate by testing with a fresh
id. Fixed the serializer by switching to the non-deprecated builder API.

### Performance notes
Cache hit returns stored results without re-running cosine similarity across the
index — the expensive operation is skipped entirely on repeat queries. Not yet
benchmarked with timing; that comes with the Locust load testing on Day 8. Known
limitations for README: Arrays.hashCode is a 32-bit hash with theoretical collision
risk (production would use SHA-256); no TTL on cache entries yet so the cache grows
unbounded; duplicate-id ingest throws instead of returning a clean error.


---

## Day 6
### What I built
Added rate limiting and global error handling. Built RateLimiter in the security
package — uses Redis to track per-user request counts with isAllowed(username):
increments a "rate:" + username counter, sets a 60-second TTL on the first request
of a window, and returns whether the count is within the limit of 100. Wired it into
JwtFilter after token validation — over-limit requests get a 429 and stop before
reaching the search. Built GlobalExceptionHandler with @RestControllerAdvice to
catch exceptions app-wide and return proper status codes with messages — duplicate
ingest now returns a clean 409 Conflict instead of a raw stack trace. Verified rate
limiting with a 105-request loop (100×200, 5×429) and the 409 on duplicate id.

### What confused me
No real conceptual confusion this session — rate limiting reused the Redis patterns
from Day 5 and the cache-aside thinking, so it was mostly applying existing knowledge.
The only friction was shell-level: a multi-line curl loop broke in zsh because the
line continuations weren't handled, and a variable assignment failed because I put
spaces around the equals sign (TOKEN = "..." instead of TOKEN="...").

### How I resolved it
Collapsed the curl loop onto a single line so zsh ran it as one command, and removed
the spaces around the assignment. Both were syntax issues rather than logic problems —
the rate-limiting and exception-handling code worked as written once the test commands
were formatted correctly.

### Performance notes
Rate limiting adds one Redis increment per request — negligible overhead, far cheaper
than the search it protects. The 60-second TTL auto-resets the window with no manual
cleanup. Known limitations for README: fixed-window rate limiting can allow bursts at
window boundaries (a sliding window would be smoother); limit is hardcoded at 100
rather than configurable.

---

## Day
### What I built


### What confused me


### How I resolved it


### Performance notes


---

## Notes
Vectors stored as comma-separated TEXT in Postgres rather than a native array type — simplest JDBC mapping, at the cost of not being able to query individual vector values in SQL. Acceptable tradeoff since vector contents are never queried at the database level; all similarity computation happens in-memory.
JWT secret key regenerates on each restart, invalidating all existing tokens. Production would load a stable secret from config/env.
/login accepts any credentials without verifying against stored hashed passwords.
Arrays.hashCode is a 32-bit hash with theoretical collision risk for cache keys; production would use SHA-256.
No TTL on search cache entries — the cache grows unbounded. (Note: the rate-limiter counter does have a TTL; this is specifically about the search result cache.)
Duplicate-id ingest now returns a clean 409, but other malformed input (e.g. wrong vector dimensions, bad JSON) isn't yet validated with specific handlers.
Fixed-window rate limiting can allow bursts at window boundaries — up to 2x the limit across a window edge. A sliding window would smooth this out.
Rate limit is hardcoded at 100/minute rather than configurable via properties.



