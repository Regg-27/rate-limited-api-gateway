# Learning Log — Rate-Limited API Gateway

## Progress
Day 1: Spring Boot scaffold, health endpoint
Day 2: VSE integration, POST /search and POST /ingest endpoints working


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

## Day 
### What I built


### What confused me


### How I resolved it


### Performance notes


---